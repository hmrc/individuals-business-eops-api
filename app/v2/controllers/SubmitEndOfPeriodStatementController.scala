/*
 * Copyright 2022 HM Revenue & Customs
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

package v2.controllers

import cats.data.EitherT
import cats.implicits._
import play.api.http.MimeTypes
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents, Result }
import utils.{ IdGenerator, Logging }
import v2.controllers.requestParsers.SubmitEndOfPeriodStatementParser
import v2.models.errors.{ NotFoundError, _ }
import v2.models.request.SubmitEndOfPeriodStatementRawData
import v2.services._

import javax.inject._
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class SubmitEndOfPeriodStatementController @Inject()(val authService: EnrolmentsAuthService,
                                                     val lookupService: MtdIdLookupService,
                                                     val idGenerator: IdGenerator,
                                                     nrsProxyService: NrsProxyService,
                                                     service: SubmitEndOfPeriodStatementService,
                                                     requestParser: SubmitEndOfPeriodStatementParser,
                                                     cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController
    with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "SubmitEndOfPeriodStatementController", endpointName = "Submit end of period statement")

  def handleRequest(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val correlationId: String = idGenerator.generateCorrelationId
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

          NoContent
            .withApiHeaders(serviceResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")
        errorResult(errorWrapper).withApiHeaders(resCorrelationId)
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper): Result = {

    if (hasBvr(errorWrapper)) {
      Forbidden(Json.toJson(errorWrapper))
    } else {
      errorWrapper.error match {
        case _
            if errorWrapper.containsAnyOf(
              BadRequestError,
              NinoFormatError,
              TypeOfBusinessFormatError,
              BusinessIdFormatError,
              StartDateFormatError,
              EndDateFormatError,
              FinalisedFormatError,
              RuleIncorrectOrEmptyBodyError,
              RuleEndDateBeforeStartDateError,
              RuleTaxYearNotSupportedError
            ) =>
          BadRequest(Json.toJson(errorWrapper))

        case RuleAlreadySubmittedError | RuleEarlySubmissionError | RuleLateSubmissionError | RuleNonMatchingPeriodError |
            RuleBusinessValidationFailureTys =>
          Forbidden(Json.toJson(errorWrapper))

        case NotFoundError => NotFound(Json.toJson(errorWrapper))
        case InternalError => InternalServerError(Json.toJson(errorWrapper))
        case _             => unhandledError(errorWrapper)

      }
    }
  }

  private def hasBvr(errorWrapper: ErrorWrapper): Boolean = {
    errorWrapper.error match {
      case MtdErrorWithCode(RuleBusinessValidationFailure.code) => true
      case MtdErrorWithCode(BadRequestError.code) =>
        errorWrapper.errors.toSeq.flatten.exists {
          case MtdErrorWithCode(RuleBusinessValidationFailure.code) => true
          case _                                                    => false
        }

      case _ => false
    }
  }
}
