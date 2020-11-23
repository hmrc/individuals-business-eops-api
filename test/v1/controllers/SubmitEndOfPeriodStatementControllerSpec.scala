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

package v1.controllers

import data.SubmitEndOfPeriodStatementData._
import mocks.MockAppConfig
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.MockIdGenerator
import v1.mocks.requestParsers.MockSubmitEndOfPeriodStatementParser
import v1.mocks.services._
import v1.models.errors.{DownstreamError, NotFoundError, _}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.{SubmitEndOfPeriodStatementRawData, SubmitEndOfPeriodStatementRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitEndOfPeriodStatementControllerSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockSubmitEndOfPeriodStatementParser
  with MockSubmitEndOfPeriodStatementService
  with MockAppConfig
  with MockAuditService
  with MockIdGenerator {

    private val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
    private val nino = "AA123456A"

    private val rawData = SubmitEndOfPeriodStatementRawData(nino, AnyContentAsJson(fullValidJson()))
    private val requestData = SubmitEndOfPeriodStatementRequest(Nino(nino), validRequest)

    trait Test {
      val hc: HeaderCarrier = HeaderCarrier()

      val controller = new SubmitEndOfPeriodStatementController(
        authService = mockEnrolmentsAuthService,
        lookupService = mockMtdIdLookupService,
        requestParser = mockSubmitEndOfPeriodStatementParser,
        service = mockSubmitEndOfPeriodStatementService,
        auditService = mockAuditService,
        appConfig = mockAppConfig,
        cc = cc,
        idGenerator = mockIdGenerator
      )

      MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
      MockedEnrolmentsAuthService.authoriseUser()
      MockedAppConfig.apiGatewayContext.returns("individuals/business/end-of-period-statement").anyNumberOfTimes()
      MockIdGenerator.getCorrelationId.returns(correlationId)
    }

    "submit" should {
      "return a successful response with header X-CorrelationId" when {
        "the request received is valid" in new Test {

          MockSubmitEndOfPeriodStatementParser
            .parseRequest(rawData)
            .returns(Right(requestData))

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
            (RangeEndDateBeforeStartDateError, BAD_REQUEST),
            (RuleNotFinalisedError, BAD_REQUEST)
          )
          input.foreach(args => (errorsFromParserTester _).tupled(args))
        }
        "service errors occur" should {
          def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
            s"a $mtdError error is returned from the service" in new Test {

              MockSubmitEndOfPeriodStatementParser
                .parseRequest(rawData)
                .returns(Right(requestData))

              MockSubmitEndOfPeriodStatementService
                .submitEndOfPeriodStatementService(requestData)
                .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

              val result: Future[Result] = controller.handleRequest(nino)(fakePutRequest(fullValidJson()))

              status(result) shouldBe expectedStatus
              contentAsJson(result) shouldBe Json.toJson(mtdError)
              header("X-CorrelationId", result) shouldBe Some(correlationId)
            }
          }

          val input = Seq(
            (RuleAlreadySubmittedError, FORBIDDEN),
            (RuleEarlySubmissionError, FORBIDDEN),
            (RuleLateSubmissionError, FORBIDDEN),
            (RuleNonMatchingPeriodError, FORBIDDEN),
            (RuleConsolidatedExpensesError, FORBIDDEN),
            (RuleMismatchedStartDateError, FORBIDDEN),
            (RuleMismatchedEndDateError, FORBIDDEN),
            (RuleClass4Over16Error, FORBIDDEN),
            (RuleClass4PensionAge, FORBIDDEN),
            (RuleFHLPrivateUseAdjustment, FORBIDDEN),
            (RuleNonFHLPrivateUseAdjustment, FORBIDDEN),
            (NotFoundError, NOT_FOUND),
            (DownstreamError, INTERNAL_SERVER_ERROR)
          )
          input.foreach(args => (serviceErrors _).tupled(args))
        }
      }
    }
}
