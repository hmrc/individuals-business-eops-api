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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.models.domain.Nino
import api.models.errors.{ErrorWrapper, NinoFormatError, RuleTaxYearNotSupportedError}
import api.models.outcomes.ResponseWrapper
import api.models.request.NinoAndJsonBodyRawData
import play.api.mvc.{AnyContentAsJson, Result}
import v2.controllers.validators.MockSubmitEndOfPeriodStatementValidatorFactory
import v2.data.SubmitEndOfPeriodStatementData._
import v2.mocks.services._
import v2.models.request.SubmitEndOfPeriodStatementRequestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitEndOfPeriodStatementControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSubmitEndOfPeriodStatementValidatorFactory
    with MockNrsProxyService
    with MockSubmitEndOfPeriodStatementService {

  private val rawData     = NinoAndJsonBodyRawData(nino, AnyContentAsJson(jsonRequestBody()))
  private val requestData = SubmitEndOfPeriodStatementRequestData(Nino(nino), validRequest)

  trait Test extends ControllerTest {

    val controller = new SubmitEndOfPeriodStatementController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validator = mockSubmitEndOfPeriodStatementValidator,
      nrsProxyService = mockNrsProxyService,
      service = mockSubmitEndOfPeriodStatementService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(nino)(fakeRequest.withBody(jsonRequestBody()))

  }

  "handleRequest" should {
    "return NO_CONTENT" when {
      "happy path" in new Test {
        MockedSubmitEndOfPeriodStatementValidator
          .parseAndValidateRequest(rawData)
          .returns(Right(requestData))

        MockNrsProxyService
          .submit(nino, validRequest)
          .returns(Future.successful((): Unit))

        MockSubmitEndOfPeriodStatementService
          .submitEndOfPeriodStatementService(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTest(NO_CONTENT)
      }
    }

    "return the error as per the spec" when {
      "the parser validation fails" in new Test {
        MockedSubmitEndOfPeriodStatementValidator
          .parseAndValidateRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError, None)))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        MockedSubmitEndOfPeriodStatementValidator
          .parseAndValidateRequest(rawData)
          .returns(Right(requestData))

        MockNrsProxyService
          .submit(nino, validRequest)
          .returns(Future.successful((): Unit))

        MockSubmitEndOfPeriodStatementService
          .submitEndOfPeriodStatementService(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

}
