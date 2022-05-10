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
import v2.models.errors.{ IfsValidationRuleFailure, _ }
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
        MockSubmitEndOfPeriodStatementConnector
          .submitEndOfPeriodStatement(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.submit(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    "unsuccessful" must {
      "map errors according to spec" when {

        def fullServiceErrorTest(ifsError: IfsError, expectedErrorWrapper: ErrorWrapper): Test = new Test {
          MockSubmitEndOfPeriodStatementConnector
            .submitEndOfPeriodStatement(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, ifsError))))

          await(service.submit(requestData)) shouldBe Left(expectedErrorWrapper)
        }

        def simpleServiceError(ifsErrorCode: String, singleMtdError: MtdError): Unit =
          s"a $ifsErrorCode error is returned from the service" in
            fullServiceErrorTest(IfsStandardError(List(IfsErrorCode(ifsErrorCode))), ErrorWrapper(correlationId, singleMtdError))

        val input = Seq(
          ("INVALID_IDTYPE", DownstreamError),
          ("INVALID_IDVALUE", NinoFormatError),
          ("INVALID_ACCOUNTINGPERIODSTARTDATE", StartDateFormatError),
          ("INVALID_ACCOUNTINGPERIODENDDATE", EndDateFormatError),
          ("INVALID_INCOMESOURCEID", BusinessIdFormatError),
          ("INVALID_INCOMESOURCETYPE", TypeOfBusinessFormatError),
          ("INVALID_CORRELATIONID", DownstreamError),
          ("EARLY_SUBMISSION", RuleEarlySubmissionError),
          ("LATE_SUBMISSION", RuleLateSubmissionError),
          ("NON_MATCHING_PERIOD", RuleNonMatchingPeriodError),
          ("NOT_FOUND", NotFoundError),
          ("CONFLICT", RuleAlreadySubmittedError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (simpleServiceError _).tupled(args))

        "a single BVR_FAILURE_EXISTS error occurs" in
          fullServiceErrorTest(
            IfsBvrError("BVR_FAILURE_EXISTS", List(IfsValidationRuleFailure("C55001", "Custom message"))),
            ErrorWrapper(correlationId, RuleBusinessValidationFailure(message = "Custom message", errorId = "C55001"))
          )

        "multiple BVR_FAILURE_EXISTS errors occur" in
          fullServiceErrorTest(
            IfsBvrError("BVR_FAILURE_EXISTS",
                        List(
                          IfsValidationRuleFailure("C55001", "Custom message1"),
                          IfsValidationRuleFailure("C55002", "Custom message2")
                        )),
            ErrorWrapper(
              correlationId,
              BadRequestError,
              Some(
                Seq(
                  RuleBusinessValidationFailure(message = "Custom message1", errorId = "C55001"),
                  RuleBusinessValidationFailure(message = "Custom message2", errorId = "C55002"),
                ))
            )
          )

        "a BVR failure with unexpected code occurs" in
          fullServiceErrorTest(
            IfsBvrError("OTHER", List(IfsValidationRuleFailure("C55001", "Custom message"))),
            ErrorWrapper(correlationId, DownstreamError)
          )
      }
    }
  }
}
