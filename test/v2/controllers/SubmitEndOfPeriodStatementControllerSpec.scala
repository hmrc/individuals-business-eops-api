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

package v2.controllers

import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, Result }
import uk.gov.hmrc.http.HeaderCarrier
import v2.data.SubmitEndOfPeriodStatementData._
import v2.mocks.MockIdGenerator
import v2.mocks.requestParsers.MockSubmitEndOfPeriodStatementParser
import v2.mocks.services._
import v2.models.domain.Nino
import v2.models.errors._
import v2.models.outcomes.ResponseWrapper
import v2.models.request.{ SubmitEndOfPeriodStatementRawData, SubmitEndOfPeriodStatementRequest }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitEndOfPeriodStatementControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockSubmitEndOfPeriodStatementParser
    with MockNrsProxyService
    with MockSubmitEndOfPeriodStatementService
    with MockIdGenerator {

  private val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  private val nino          = "AA123456A"

  private val rawData     = SubmitEndOfPeriodStatementRawData(nino, AnyContentAsJson(fullValidJson()))
  private val requestData = SubmitEndOfPeriodStatementRequest(Nino(nino), validRequest)

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller = new SubmitEndOfPeriodStatementController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      requestParser = mockSubmitEndOfPeriodStatementParser,
      nrsProxyService = mockNrsProxyService,
      service = mockSubmitEndOfPeriodStatementService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  "submit" should {
    "return a successful response with header X-CorrelationId" when {
      "the request received is valid" in new Test {

        MockSubmitEndOfPeriodStatementParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockNrsProxyService
          .submit(nino, validRequest)
          .returns(Future.successful((): Unit))

        MockSubmitEndOfPeriodStatementService
          .submitEndOfPeriodStatementService(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, Unit))))

        val result: Future[Result] = controller.handleRequest(nino)(fakePutRequest(fullValidJson()))
        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per the spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {
            MockSubmitEndOfPeriodStatementParser
              .parseRequest(rawData)
              .returns(Left(ErrorWrapper(correlationId, error)))

            val result: Future[Result] = controller.handleRequest(nino)(fakePutRequest(fullValidJson()))

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TypeOfBusinessFormatError, BAD_REQUEST),
          (BusinessIdFormatError, BAD_REQUEST),
          (StartDateFormatError, BAD_REQUEST),
          (EndDateFormatError, BAD_REQUEST),
          (FinalisedFormatError, BAD_REQUEST),
          (RuleIncorrectOrEmptyBodyError, BAD_REQUEST),
          (RuleEndDateBeforeStartDateError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" should {

        def fullServiceErrorTest(errorWrapper: ErrorWrapper, expectedStatus: Int): Test = new Test {
          MockSubmitEndOfPeriodStatementParser
            .parseRequest(rawData)
            .returns(Right(requestData))

          MockNrsProxyService
            .submit(nino, validRequest)
            .returns(Future.successful((): Unit))

          MockSubmitEndOfPeriodStatementService
            .submitEndOfPeriodStatementService(requestData)
            .returns(Future.successful(Left(errorWrapper)))

          val result: Future[Result] = controller.handleRequest(nino)(fakePutRequest(fullValidJson()))

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe Json.toJson(errorWrapper)
          header("X-CorrelationId", result) shouldBe Some(correlationId)
        }

        def simpleServiceError(mtdError: MtdError, expectedStatus: Int): Unit =
          s"a $mtdError error is returned from the service" in
            fullServiceErrorTest(ErrorWrapper(correlationId, mtdError), expectedStatus)

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (StartDateFormatError, BAD_REQUEST),
          (EndDateFormatError, BAD_REQUEST),
          (BusinessIdFormatError, BAD_REQUEST),
          (TypeOfBusinessFormatError, BAD_REQUEST),
          (RuleEarlySubmissionError, BAD_REQUEST),
          (RuleLateSubmissionError, BAD_REQUEST),
          (RuleBusinessValidationFailure("some message", "C54321"), BAD_REQUEST),
          (RuleNonMatchingPeriodError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (RuleAlreadySubmittedError, BAD_REQUEST),
          (InternalError, INTERNAL_SERVER_ERROR),
          (RuleBusinessValidationFailureTys, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST)
        )

        input.foreach(args => (simpleServiceError _).tupled(args))

        "multiple BVR errors occur" in
          fullServiceErrorTest(
            ErrorWrapper(correlationId,
                         BadRequestError,
                         Some(
                           Seq(
                             RuleBusinessValidationFailure("some message", "id1"),
                             RuleBusinessValidationFailure("some message", "id2")
                           ))),
            BAD_REQUEST
          )
      }
    }
  }
}
