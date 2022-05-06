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

import javax.inject.{ Inject, Singleton }
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v2.connectors.SubmitEndOfPeriodStatementConnector
import v2.controllers.EndpointLogContext
import v2.models.errors._
import v2.models.request.SubmitEndOfPeriodStatementRequest
import v2.support.DownstreamResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitEndOfPeriodStatementService @Inject()(connector: SubmitEndOfPeriodStatementConnector)
  extends DownstreamResponseMappingSupport with Logging {

  def submit(request: SubmitEndOfPeriodStatementRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext,
    correlationId: String): Future[SubmitEndOfPeriodStatementOutcome] = {

    val result = for {
      downstreamResponseWrapper <- EitherT(connector.submitPeriodStatement(request)).leftMap(mapDownstreamErrors(downstreamErrorMap))
    } yield downstreamResponseWrapper

    result.value
  }

  private def downstreamErrorMap: Map[String, MtdError] = Map(
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "INVALID_ACCOUNTINGPERIODSTARTDATE" -> StartDateFormatError,
    "INVALID_ACCOUNTINGPERIODENDDATE" -> EndDateFormatError,
    "INVALID_INCOMESOURCEID" -> BusinessIdFormatError,
    "INVALID_INCOMESOURCETYPE" -> TypeOfBusinessFormatError,
    "INVALID_CORRELATIONID" -> DownstreamError,
    "CONFLICT" -> RuleAlreadySubmittedError,
    "EARLY_SUBMISSION" -> RuleEarlySubmissionError,
    "LATE_SUBMISSION" -> RuleLateSubmissionError,
    "NON_MATCHING_PERIOD" -> RuleNonMatchingPeriodError,
    "NOT_FOUND" -> NotFoundError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError,
    "C55503" -> RuleConsolidatedExpensesError,
    "C55316" -> RuleConsolidatedExpensesError,
    "C55525" -> RuleConsolidatedExpensesError,
    "C55008" -> RuleMismatchedStartDateError,
    "C55013" -> RuleMismatchedEndDateError,
    "C55014" -> RuleMismatchedEndDateError,
    "C55317" -> RuleClass4Over16Error,
    "C55318" -> RuleClass4PensionAge,
    "C55501" -> RuleFHLPrivateUseAdjustment,
    "C55502" -> RuleNonFHLPrivateUseAdjustment,
    "BVR_UNKNOWN_ID" -> RuleBusinessValidationFailure
  )
}
