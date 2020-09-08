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

package v1.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v1.connectors.SubmitEndOfPeriodStatementConnector
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.SubmitEndOfPeriodStatementRequest

import scala.concurrent.{ExecutionContext, Future}

class SubmitEndOfPeriodStatementService @Inject()(connector: SubmitEndOfPeriodStatementConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def submitEndOfPeriodStatementService(request: SubmitEndOfPeriodStatementRequest)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SumbmitEndOfPeriodStatmentOutcome] = {

    connector.submitPeriodStatement(request).map {

      mapToVendorDirect("submitEndOfPeriodStatement",desErrorToMtdError,desBvrErrorToMtdError)

    }
  }

    private def desErrorToMtdError: Map[String, MtdError] = Map(
      "INVALID_IDTYPE" -> DownstreamError,
      "INVALID_IDVALUE" -> NinoFormatError,
      "INVALID_ACCOUNTINGPERIODSTARTDATE" -> StartDateFormatError,
      "INVALID_ACCOUNTINGPERIODENDDATE" -> EndDateFormatError,
      "INVALID_INCOMESOURCEID" -> BusinessIdFormatError,
      "INVALID_INCOMESOURCETYPE" -> TypeOfBusinessFormatError,
      "CONFLICT" -> RuleAlreadySubmittedError,
      "EARLY_SUBMISSION" -> RuleEarlySubmissionError,
      "LATE_SUBMISSION" -> RuleLateSubmissionError,
      "NON_MATCHING_PERIOD" -> RuleNonMatchingPeriodError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )

    private def desBvrErrorToMtdError: Map[String, MtdError] = Map(
      "C55503" -> RuleConsolidatedExpensesError,
      "C55316" -> RuleConsolidatedExpensesError,
      "C55008" -> RuleMismatchedStartDateError,
      "C55013" -> RuleMismatchedEndDateError,
      "C55014" -> RuleMismatchedEndDateError,
      "C55317" -> RuleClass4Over16Error,
      "C55318" -> RuleClass4PensionAge,
      "C55501" -> RuleFHLPrivateUseAdjustment,
      "C55502" -> RuleNonFHLPrivateUseAdjustment
    )

  }
