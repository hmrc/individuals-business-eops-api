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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.controllers.requestParsers.SubmitEndOfPeriodStatementParser
import v1.hateoas.AmendHateoasBody
import v1.models.audit._
import v1.models.auth.UserDetails
import v1.models.errors._
import v1.models.requestData.{SubmitEndOfPeriodStatementRawData, SubmitEndOfPeriodStatementRequest}
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

class SubmitEndOfPeriodStatementController @Inject()(val authService: EnrolmentsAuthService,
                                                     val lookupService: MtdIdLookupService,
                                                     val idGenerator: IdGenerator,
                                                     service: SubmitEndOfPeriodStatementService,
                                                     requestParser: SubmitEndOfPeriodStatementParser,
                                                     auditService: AuditService,
                                                     appConfig: AppConfig,
                                                     cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) with AmendHateoasBody with BaseController with Logging {

  implicit val endpointLogContext: EndpointLogContext = EndpointLogContext(controllerName = "SubmitEndOfPeriodStatementController",
    endpointName = "Submit end of period statement")

  def submitEndOfPeriodStatement(nino: String): Action[JsValue] = {
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val correlationId: String = idGenerator.getCorrelationId
      logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
        s"with correlationId : $correlationId")

      val rawData = SubmitEndOfPeriodStatementRawData(nino, AnyContentAsJson(request.body))
      val parseRequest: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequest] = requestParser.parseRequest(rawData)

      val serviceResponse: Future[SubmitEndOfPeriodStatmentOutcome] = parseRequest match {
        case Right(data) => service.submitEndOfPeriodStatementService(data)
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
          val resCorrelationId = errorWrapper.correlationId
          val result = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Error response received with CorrelationId: $resCorrelationId")

          auditSubmission(createAuditDetails(rawData, result.header.status, resCorrelationId, request.userDetails, Some(errorWrapper)))
          result
      }
    }
  }

  private def errorResult(errorWrapper: ErrorWrapper): Result = {
    (errorWrapper.errors.head.copy(paths = None): @unchecked) match {
      case BadRequestError |
           NinoFormatError |
           TypeOfBusinessFormatError |
           BusinessIdFormatError |
           StartDateFormatError |
           EndDateFormatError |
           FinalisedFormatError |
           RuleIncorrectOrEmptyBodyError |
           RangeEndDateBeforeStartDateError |
           RuleNotFinalisedError
                => BadRequest(Json.toJson(errorWrapper))

      case BVRError |
           RuleAlreadySubmittedError |
           RuleEarlySubmissionError |
           RuleLateSubmissionError |
           RuleNonMatchingPeriodError |
           RuleConsolidatedExpensesError |
           RuleMismatchedStartDateError |
           RuleMismatchedEndDateError |
           RuleClass4Over16Error |
           RuleClass4PensionAge |
           RuleFHLPrivateUseAdjustment |
           RuleNonFHLPrivateUseAdjustment
                => Forbidden(Json.toJson(errorWrapper))

      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def createAuditDetails(rawData: SubmitEndOfPeriodStatementRawData,
                                 statusCode: Int,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None,
                                 responseBody: Option[JsValue] = None): GenericAuditDetail = {

    val response = errorWrapper.map( wrapper => AuditResponse(statusCode, Some(wrapper.auditErrors), None)).getOrElse(AuditResponse(statusCode, None, None))
    GenericAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, rawData.nino, correlationId, response)
  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("submitEndOfPeriodStatementAuditType", "submit-end-of-period-statement-transaction-type", details)
    auditService.auditEvent(event)
  }
}
