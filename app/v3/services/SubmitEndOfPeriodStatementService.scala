/*
 * Copyright 2023 HM Revenue & Customs
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

package v3.services

import api.controllers.{EndpointLogContext, RequestContext}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import api.services.{BaseService, ServiceOutcome}
import cats.syntax.either._
import v3.connectors.SubmitEndOfPeriodStatementConnector
import v3.models.request.SubmitEndOfPeriodStatementRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitEndOfPeriodStatementService @Inject() (connector: SubmitEndOfPeriodStatementConnector) extends BaseService {

  def submit(request: SubmitEndOfPeriodStatementRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {
    connector
      .submitPeriodStatement(request)
      .map(_.leftMap(errorOrBvrMap))
  }

  def errorOrBvrMap(downstreamResponseWrapper: ResponseWrapper[DownstreamError])(implicit logContext: EndpointLogContext): ErrorWrapper = {
    downstreamResponseWrapper match {
      case ResponseWrapper(correlationId, DownstreamBvrError("BVR_FAILURE_EXISTS", items)) =>
        items match {
          case item :: Nil =>
            ErrorWrapper(correlationId, error = RuleBusinessValidationFailure(errorId = item.id, message = item.text), errors = None)
          case items =>
            ErrorWrapper(
              correlationId,
              error = BadRequestError,
              errors = Some(items.map(item => RuleBusinessValidationFailure(errorId = item.id, message = item.text))))
        }

      case wrapper => mapDownstreamErrors(downstreamErrorMap)(wrapper)
    }
  }

  private val downstreamErrorMap: Map[String, MtdError] = {
    val errors = Map(
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

    val extraTysErrors = Map(
      "INVALID_TAX_YEAR"                   -> InternalError,
      "INVALID_TAXABLE_ENTITY_ID"          -> NinoFormatError,
      "INVALID_START_DATE"                 -> StartDateFormatError,
      "INVALID_END_DATE"                   -> EndDateFormatError,
      "INVALID_INCOME_SOURCE_ID"           -> BusinessIdFormatError,
      "INVALID_INCOME_SOURCE_TYPE"         -> TypeOfBusinessFormatError,
      "INVALID_PAYLOAD"                    -> InternalError,
      "INVALID_QUERY_PARAMETERS"           -> InternalError,
      "PERIOD_MISMATCH"                    -> RuleNonMatchingPeriodError,
      "BVR_FAILURE"                        -> RuleBusinessValidationFailureTys,
      "TAX_YEAR_NOT_SUPPORTED"             -> RuleTaxYearNotSupportedError,
      "BUSINESS_INCOME_PERIOD_RESTRICTION" -> RuleBusinessIncomePeriodRestriction
    )

    errors ++ extraTysErrors
  }

}
