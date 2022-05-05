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

import v2.data.SubmitEndOfPeriodStatementData.validRequest
import v2.models.domain.Nino
import v2.mocks.connectors.MockSubmitEndOfPeriodStatementConnector
import v2.models.errors._
import v2.models.outcomes.ResponseWrapper
import v2.models.request.SubmitEndOfPeriodStatementRequest

import scala.concurrent.Future

class SubmitEndOfPeriodStatementServiceSpec extends ServiceSpec {

  val nino = "AA123456A"

  trait Test extends MockSubmitEndOfPeriodStatementConnector {
    val service = new SubmitEndOfPeriodStatementService(connector)
  }

  val requestData: SubmitEndOfPeriodStatementRequest = SubmitEndOfPeriodStatementRequest(Nino(nino), validRequest)

  "service" when {
    "service call successful" must {
      "return mapped result" in new Test {
        MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.submit(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    "unsuccessful" must {
      "map errors according to spec" when {

        def serviceError(ifsErrorCode: String, error: MtdError): Unit =
          s"a $ifsErrorCode error is returned from the service" in new Test {

            MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, IfsErrors.single(IfsErrorCode(ifsErrorCode))))))

            await(service.submit(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
          }

        val input = Seq(
          ("INVALID_IDTYPE", DownstreamError),
          ("INVALID_IDVALUE", NinoFormatError),
          ("INVALID_ACCOUNTINGPERIODSTARTDATE", StartDateFormatError),
          ("INVALID_ACCOUNTINGPERIODENDDATE", EndDateFormatError),
          ("INVALID_INCOMESOURCEID", BusinessIdFormatError),
          ("INVALID_INCOMESOURCETYPE", TypeOfBusinessFormatError),
          ("INVALID_CORRELATIONID", DownstreamError),
          ("CONFLICT", RuleAlreadySubmittedError),
          ("EARLY_SUBMISSION", RuleEarlySubmissionError),
          ("LATE_SUBMISSION", RuleLateSubmissionError),
          ("C55503", RuleConsolidatedExpensesError),
          ("C55316", RuleConsolidatedExpensesError),
          ("C55525", RuleConsolidatedExpensesError),
          ("C55008", RuleMismatchedStartDateError),
          ("C55013", RuleMismatchedEndDateError),
          ("C55014", RuleMismatchedEndDateError),
          ("C55317", RuleClass4Over16Error),
          ("C55318", RuleClass4PensionAge),
          ("C55501", RuleFHLPrivateUseAdjustment),
          ("C55502", RuleNonFHLPrivateUseAdjustment),
          ("BVR_UNKNOWN_ID", RuleBusinessValidationFailure),
          ("NON_MATCHING_PERIOD", RuleNonMatchingPeriodError),
          ("NOT_FOUND", NotFoundError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}
