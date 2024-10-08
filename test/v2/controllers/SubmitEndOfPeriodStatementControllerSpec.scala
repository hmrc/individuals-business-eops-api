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
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.Nino
import api.models.errors.{ErrorWrapper, NinoFormatError, RuleTaxYearNotSupportedError}
import api.models.outcomes.ResponseWrapper
import api.services.MockAuditService
import config.MockAppConfig
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.Result
import routing.{Version, Version2}
import v2.controllers.validators.MockSubmitEndOfPeriodStatementValidatorFactory
import v2.data.SubmitEndOfPeriodStatementData.jsonRequestBody
import v2.mocks.services._
import v2.models.request.{SubmitEndOfPeriodRequestBody, SubmitEndOfPeriodStatementRequestData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitEndOfPeriodStatementControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockSubmitEndOfPeriodStatementValidatorFactory
    with MockSubmitEndOfPeriodStatementService
    with MockAppConfig
    with MockAuditService {

  private val requestBody: SubmitEndOfPeriodRequestBody = jsonRequestBody(typeOfBusiness = "foreign-property").as[SubmitEndOfPeriodRequestBody]

  private val requestData: SubmitEndOfPeriodStatementRequestData = SubmitEndOfPeriodStatementRequestData(Nino(nino), requestBody)

  "handleRequest" should {
    "return NO_CONTENT" when {
      "happy path" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockSubmitEndOfPeriodStatementService
          .submitEndOfPeriodStatementService(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(
          expectedStatus = NO_CONTENT,
          maybeAuditRequestBody = Some(jsonRequestBody()),
          maybeExpectedResponseBody = None,
          maybeAuditResponseBody = None
        )
      }
    }

    "return the error as per the spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)
      }

      "the service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockSubmitEndOfPeriodStatementService
          .submitEndOfPeriodStatementService(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail]{

    protected def apiVersion: Version = Version2

    val controller = new SubmitEndOfPeriodStatementController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockSubmitEndOfPeriodStatementValidatorFactory,
      service = mockSubmitEndOfPeriodStatementService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(nino)(fakeRequest.withBody(jsonRequestBody()))

    MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
      "supporting-agents-access-control.enabled" -> true
    )

    MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

    protected def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "SubmitEOPSStatement",
        transactionName = "submit-eops-statement",
        detail = GenericAuditDetail(
          versionNumber = "2.0",
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino),
          requestBody = requestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )
  }

}
