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

package v2.services

import cats.data.EitherT
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v2.connectors.SubmitEndOfPeriodStatementConnector
import v2.controllers.EndpointLogContext
import v2.models.errors._
import v2.models.outcomes.ResponseWrapper
import v2.models.request.SubmitEndOfPeriodStatementRequest
import v2.support.DownstreamResponseMappingSupport

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class SubmitEndOfPeriodStatementService @Inject()(connector: SubmitEndOfPeriodStatementConnector)
    extends DownstreamResponseMappingSupport
    with Logging {

  def submit(request: SubmitEndOfPeriodStatementRequest)(implicit hc: HeaderCarrier,
                                                         ec: ExecutionContext,
                                                         logContext: EndpointLogContext,
                                                         correlationId: String): Future[ServiceOutcome[Unit]] = {

    EitherT(connector.submitPeriodStatement(request)).leftMap(errorOrBvrMap).value
  }

  def errorOrBvrMap(downstreamResponseWrapper: ResponseWrapper[DownstreamError])(implicit logContext: EndpointLogContext): ErrorWrapper = {
    downstreamResponseWrapper match {
      case ResponseWrapper(correlationId, DownstreamBvrError("BVR_FAILURE_EXISTS", items)) =>
        items match {
          case item :: Nil =>
            ErrorWrapper(correlationId, error = RuleBusinessValidationFailure(errorId = item.id, message = item.text), errors = None)
          case items =>
            ErrorWrapper(correlationId,
                         error = BadRequestError,
                         errors = Some(items.map(item => RuleBusinessValidationFailure(errorId = item.id, message = item.text))))
        }

      case wrapper => mapDownstreamErrors(downstreamErrorMap)(wrapper)
    }
  }

  private val downstreamErrorMap: Map[String, MtdError] = Map(
    "INVALID_IDTYPE"                    -> InternalError,
    "INVALID_IDVALUE"                   -> NinoFormatError,
    "INVALID_ACCOUNTINGPERIODSTARTDATE" -> StartDateFormatError,
    "INVALID_ACCOUNTINGPERIODENDDATE"   -> EndDateFormatError,
    "INVALID_INCOMESOURCEID"            -> BusinessIdFormatError,
    "INVALID_INCOMESOURCETYPE"          -> TypeOfBusinessFormatError,
    "INVALID_CORRELATIONID"             -> InternalError,
    "EARLY_SUBMISSION"                  -> RuleEarlySubmissionError,
    "LATE_SUBMISSION"                   -> RuleLateSubmissionError,
    "NON_MATCHING_PERIOD"               -> RuleNonMatchingPeriodError,
    "NOT_FOUND"                         -> NotFoundError,
    "CONFLICT"                          -> RuleAlreadySubmittedError,
    "SERVER_ERROR"                      -> InternalError,
    "SERVICE_UNAVAILABLE"               -> InternalError
  )
}
