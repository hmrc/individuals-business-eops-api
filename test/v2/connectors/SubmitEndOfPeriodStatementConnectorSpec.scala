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

package v2.connectors

import mocks.MockAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import v2.data.SubmitEndOfPeriodStatementData._
import v2.models.domain.Nino
import v2.mocks._
import v2.models.downstream.EmptyJsonBody
import v2.models.errors._
import v2.models.outcomes.ResponseWrapper
import v2.models.request.SubmitEndOfPeriodStatementRequest

import scala.concurrent.Future

class SubmitEndOfPeriodStatementConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: SubmitEndOfPeriodStatementConnector = new SubmitEndOfPeriodStatementConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifsBaseUrl returns baseUrl
    MockAppConfig.ifsToken returns "ifs-token"
    MockAppConfig.ifsEnvironment returns "ifs-environment"
    MockAppConfig.ifsEnvironmentHeaders returns Some(allowedIfsHeaders)
  }

  "Submit end of period statement" when {

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
    val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient
          .post(
            url = s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
            s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId",
            config = dummyHeaderCarrierConfig,
            body = EmptyJsonBody,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

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
          .post(
            url = s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
            s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId",
            config = dummyHeaderCarrierConfig,
            body = EmptyJsonBody,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

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
          .post(
            url = s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
            s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId",
            config = dummyHeaderCarrierConfig,
            body = EmptyJsonBody,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        await(connector.submitPeriodStatement(
          SubmitEndOfPeriodStatementRequest(
            nino = Nino(nino),
            validRequest)
        )) shouldBe expected
      }
    }
  }
}