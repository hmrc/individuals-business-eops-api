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

import data.SubmitEndOfPeriodStatementData.validRequest
import uk.gov.hmrc.domain.Nino
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData.SubmitEndOfPeriodStatementRequest
import v1.services.{ServiceSpec, SubmitEndOfPeriodStatementService}

import scala.concurrent.Future

class SubmitEndOfPeriodStatementServiceSpec extends ServiceSpec {

  val nino: Nino = Nino("AA123456A")

  trait Test extends MockSubmitEndOfPeriodStatementConnector {
    lazy val service = new SubmitEndOfPeriodStatementService(connector)
  }

  lazy val request: SubmitEndOfPeriodStatementRequest = SubmitEndOfPeriodStatementRequest(nino,validRequest)

  "Submit End Of Period Statement" should {
    "return a Right" when {
      "the connector call is successful" in new Test {
        val desResponse: DesResponse[Unit] = DesResponse(correlationId, ())
        val expected: DesResponse[Unit] = DesResponse(correlationId, ())
        MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(Right(desResponse)))

        await(service.submitEndOfPeriodStatementService(request)) shouldBe Right(expected)
      }
    }
    "return that wrapped error as-is" when {
      "the connector returns an outbound error" in new Test {
        val someError:MtdError = DownstreamError
        val desResponse: DesResponse[OutboundError] = DesResponse(correlationId, OutboundError(someError))
        MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(Left(desResponse)))

        await(service.submitEndOfPeriodStatementService(request)) shouldBe Left(ErrorWrapper(correlationId, Seq(someError)))
      }
    }
    "return a downstream error" when {
      "the connector call returns a single unknown error default to downstream error" in new Test {
        val desResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(MtdError("Error","error")))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, Seq(DownstreamError))

        MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(Left(desResponse)))
        await(service.submitEndOfPeriodStatementService(request)) shouldBe Left(expected)
      }
      "the connector call returns a single downstream error" in new Test {
        val desResponse: DesResponse[SingleError] = DesResponse(correlationId, SingleError(DownstreamError))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, Seq(DownstreamError))
        MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(Left(desResponse)))

        await(service.submitEndOfPeriodStatementService(request)) shouldBe Left(expected)
      }
      "the connector call returns multiple errors including a downstream error" in new Test {
        val desResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, DownstreamError)))
        val expected: ErrorWrapper = ErrorWrapper(correlationId, Seq(DownstreamError))
        MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(Left(desResponse)))

        await(service.submitEndOfPeriodStatementService(request)) shouldBe Left(expected)
      }
    }
    Map(
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
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(
              Left(DesResponse(correlationId, SingleError(MtdError(k, "doesn't matter"))))))

            await(service.submitEndOfPeriodStatementService(request)) shouldBe Left(ErrorWrapper(correlationId, Seq(v)))
          }
        }
    }
    Map(
      "C55503" -> RuleConsolidatedExpensesError,
      "C55316" -> RuleConsolidatedExpensesError,
      "C55008" -> RuleMismatchedStartDateError,
      "C55013" -> RuleMismatchedEndDateError,
      "C55014" -> RuleMismatchedEndDateError,
      "C55317" -> RuleClass4Over16Error,
      "C55318" -> RuleClass4PensionAge,
      "C55501" -> RuleFHLPrivateUseAdjustment,
      "C55502" -> RuleNonFHLPrivateUseAdjustment
    ).foreach {
      case (k, v) =>
        s"return a ${v.code} error" when {
          s"the connector call returns $k" in new Test {
            MockSubmitEndOfPeriodStatementConnector.submitEndOfPeriodStatement(request).returns(Future.successful(
              Left(DesResponse(correlationId, BVRErrors(Seq(MtdError(k, "doesn't matter")))))))

            await(service.submitEndOfPeriodStatementService(request)) shouldBe Left(ErrorWrapper(correlationId, Seq(v)))
          }
        }
    }
  }
}
