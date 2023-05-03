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

package v1.connectors

import api.connectors.DownstreamUri.IfsUri
import config.AppConfig
import mocks.MockAppConfig
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpReads }
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper

import scala.concurrent.Future

class BaseDownstreamConnectorSpec extends ConnectorSpec {

  // WLOG
  case class Result(value: Int)

  // WLOG
  val body    = "body"
  val outcome = Right(ResponseWrapper(correlationId, Result(2)))

  val url         = "some/url?param=value"
  val absoluteUrl = s"$baseUrl/$url"

  implicit val httpReads: HttpReads[DownstreamOutcome[Result]] = mock[HttpReads[DownstreamOutcome[Result]]]

  class Test(ifsEnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.ifsBaseUrl returns baseUrl
    MockAppConfig.ifsToken returns "ifs-token"
    MockAppConfig.ifsEnvironment returns "ifs-environment"
    MockAppConfig.ifsEnvironmentHeaders returns ifsEnvironmentHeaders
  }

  "BaseDownstreamConnector" when {
    val requiredHeaders: Seq[(String, String)] = Seq(
      "Environment"       -> "ifs-environment",
      "Authorization"     -> "Bearer ifs-token",
      "User-Agent"        -> "individuals-business-eops-api",
      "CorrelationId"     -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )

    val excludedHeaders: Seq[(String, String)] = Seq(
      "AnotherHeader" -> "HeaderValue"
    )

    "making a HTTP request to a downstream service (i.e IFS)" must {
      testHttpMethods(dummyHeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedIfsHeaders))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment"   -> "ifs-environment",
          "Authorization" -> "Bearer ifs-token",
          "User-Agent"    -> "individuals-business-eops-api",
          "CorrelationId" -> correlationId,
        )

        testHttpMethods(dummyHeaderCarrierConfig, requiredHeaders, otherHeaders, None)
      }
    }
  }

  def testHttpMethods(config: HeaderCarrier.Config,
                      requiredHeaders: Seq[(String, String)],
                      excludedHeaders: Seq[(String, String)],
                      ifsEnvironmentHeaders: Option[Seq[String]]): Unit = {

    "POST" in new Test(ifsEnvironmentHeaders) {
      implicit val hc: HeaderCarrier                 = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
      val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

      MockedHttpClient
        .post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
        .returns(Future.successful(outcome))

      await(connector.post(body, IfsUri[Result](url))) shouldBe outcome
    }
  }
}
