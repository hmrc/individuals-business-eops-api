/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import javax.inject._
import play.api.http.MimeTypes
import play.api.libs.json.{JsDefined, JsObject, JsUndefined, JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.controllers.requestParsers.SubmitEndOfPeriodStatementParser
import v1.hateoas.AmendHateoasBody
import v1.models.audit._
import v1.models.errors._
import v1.models.request.SubmitEndOfPeriodStatementRawData
import v1.services._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitEndOfPeriodStatementController @Inject()(val authService: EnrolmentsAuthService,
                                                     val lookupService: MtdIdLookupService,
                                                     val idGenerator: IdGenerator,
                                                     nrsProxyService: NrsProxyService,
                                                     service: SubmitEndOfPeriodStatementService,
                                                     requestParser: SubmitEndOfPeriodStatementParser,
                                                     auditService: AuditService,
                                                     cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) with AmendHateoasBody with BaseController with Logging {

  implicit val endpointLogContext: EndpointLogContext = EndpointLogContext(controllerName = "SubmitEndOfPeriodStatementController",
    endpointName = "Submit end of period statement")

  def handleRequest(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val correlationId: String = idGenerator.getCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
          s"with CorrelationId: $correlationId")

      val rawData = SubmitEndOfPeriodStatementRawData(nino, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](requestParser.parseRequest(rawData))
          serviceResponse <- {
            nrsProxyService.submit(nino, parsedRequest.submitEndOfPeriod)
            EitherT(service.submit(parsedRequest))
          }
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          auditSubmission(GenericAuditDetail(request.userDetails, nino, request.body,
            correlationId, AuditResponse(NO_CONTENT, Right(None))))

          NoContent.withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val correlationId = errorWrapper.correlationId
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)

        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $correlationId")

        val auditRequestJson = request.body \ "finalised" match {
          case JsDefined(finalised) =>
            request.body.as[JsObject] - "finalised" ++ Json.obj("endOfPeriodStatementFinalised" -> finalised)
          case _: JsUndefined       => request.body
        }

        auditSubmission(GenericAuditDetail(request.userDetails, nino, auditRequestJson,
          correlationId, AuditResponse(result.header.status, Left(errorWrapper.auditErrors))))

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper): Result = {
    (errorWrapper.error: @unchecked) match {
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

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("SubmitEndOfPeriodStatementAuditType", "submit-end-of-period-statement-transaction-type", details)
    auditService.auditEvent(event)
  }
}
