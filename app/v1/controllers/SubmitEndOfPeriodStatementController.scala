/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.controllers

import config.AppConfig
import javax.inject._
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v1.hateoas.AmendHateoasBody
import v1.models.audit._
import v1.models.auth.UserDetails
import v1.models.errors._
import v1.models.requestData.{SubmitEndOfPeriodStatementRawData, SubmitEndOfPeriodStatementRequest}
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

class SubmitEndOfPeriodStatementController @Inject()(val authService: EnrolmentsAuthService,
                                                     val lookupService: MtdIdLookupService,
                                                     service: SubmitEndOfPeriodStatementService,
                                                     requestParser: SubmitEndOfPeriodStatementParser,
                                                     auditService: AuditService,
                                                     appConfig: AppConfig,
                                                     cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) with AmendHateoasBody with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "SubmitEndOfPeriodStatementController",
      endpointName = "Submit end of period statement")

  def amend(nino: String, taxYear: String): Action[JsValue] = {
    authorisedAction(nino).async(parse.json) { implicit request =>

      val rawData = SubmitEndOfPeriodStatementRawData(nino, AnyContentAsJson(request.body))
      val parseRequest: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequest] = requestParser.parseRequest(rawData)

      val serviceResponse: Future[AmendPensionChargesOutcome] = parseRequest match {
        case Right(data) => service.submitEndOfPeriodStatement(data)
        case Left(errorWrapper) => Future.successful(Left(errorWrapper))
      }

      serviceResponse.map {
        case Right(responseWrapper) =>

          logger.info(s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Success response received with CorrelationId: ${responseWrapper.correlationId}")

          auditSubmission(
            createAuditDetails(rawData, NO_CONTENT, responseWrapper.correlationId, request.userDetails, None, Some(Json.toJson(responseWrapper.correlationId)))
          )

          NoContent.withApiHeaders(responseWrapper.correlationId)

        case Left(errorWrapper) =>
          val correlationId = getCorrelationId(errorWrapper)
          val result = errorResult(errorWrapper).withApiHeaders(correlationId)
          auditSubmission(createAuditDetails(rawData, result.header.status, correlationId, request.userDetails, Some(errorWrapper)))
          result
      }
    }
  }

  private def errorResult(errorWrapper: ErrorWrapper): Result = {

    (errorWrapper.errors.head.copy(paths = None): @unchecked) match {
      case BadRequestError |
           NinoFormatError |
           TaxYearFormatError |
           RuleTaxYearRangeInvalid |
           RuleTaxYearNotSupportedError |
           RuleIncorrectOrEmptyBodyError |
           ValueFormatError |
           RuleCountryCodeError |
           CountryCodeFormatError |
           QOPSRefFormatError |
           PensionSchemeTaxRefFormatError |
           ProviderNameFormatError |
           ProviderAddressFormatError |
           RuleIsAnnualAllowanceReducedError |
           RuleBenefitExcessesError |
           RulePensionReferenceError
                => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def createAuditDetails(rawData: AmendPensionChargesRawData,
                                 statusCode: Int,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None,
                                 responseBody: Option[JsValue] = None): GenericAuditDetail = {

    val response = errorWrapper.map( wrapper => AuditResponse(statusCode, Some(wrapper.auditErrors), None)).getOrElse(AuditResponse(statusCode, None, None))
    GenericAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, rawData.nino, correlationId, response)
  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("amendPensionChargesAuditType", "amend-pension-charges-transaction-type", details)
    auditService.auditEvent(event)
  }
}
