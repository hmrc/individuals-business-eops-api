/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.connectors

import data.SubmitEndOfPeriodStatementData._
import mocks.MockAppConfig
import v1.models.domain.Nino
import v1.mocks._
import v1.models.des.EmptyJsonBody
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.SubmitEndOfPeriodStatementRequest

import scala.concurrent.Future

class SubmitEndOfPeriodStatementConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: SubmitEndOfPeriodStatementConnector = new SubmitEndOfPeriodStatementConnector(http = mockHttpClient, appConfig = mockAppConfig)

    val desRequestHeaders: Seq[(String, String)] = Seq("Environment" -> "des-environment", "Authorization" -> s"Bearer des-token")
    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)
  }

  "Submit end of period statement" when {

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
            s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId", EmptyJsonBody)
          .returns(Future.successful(expected))

        await(connector.submitPeriodStatement(
          SubmitEndOfPeriodStatementRequest(
            nino = Nino(nino),
            validRequest)
        )) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, NinoFormatError))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
            s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId", EmptyJsonBody)
          .returns(Future.successful(expected))

        await(connector.submitPeriodStatement(
          SubmitEndOfPeriodStatementRequest(
            nino = Nino(nino),
            validRequest)
        )) shouldBe expected
      }
    }
    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, Seq(NinoFormatError, DownstreamError)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
            s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId", EmptyJsonBody)
          .returns(Future.successful(expected))

        await(connector.submitPeriodStatement(
          SubmitEndOfPeriodStatementRequest(
            nino = Nino(nino),
            validRequest)
        )) shouldBe expected
      }
    }
  }
}
