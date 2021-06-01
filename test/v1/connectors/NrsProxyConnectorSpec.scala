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

import mocks.MockAppConfig
import v1.mocks.MockHttpClient
import v1.models.des.TypeOfBusiness.`foreign-property`
import v1.models.request.{AccountingPeriod, SubmitEndOfPeriod}

import scala.concurrent.Future

class NrsProxyConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  val submitEndOfPeriodRequestBody: SubmitEndOfPeriod = SubmitEndOfPeriod(
    typeOfBusiness = `foreign-property`,
    businessId = "XAIS12345678910",
    accountingPeriod = AccountingPeriod(startDate = "2021-04-06", endDate = "2022-04-05"),
    finalised = true
  )

  class Test extends MockHttpClient with MockAppConfig {

    val connector: NrsProxyConnector = new NrsProxyConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.mtdNrsProxyBaseUrl returns baseUrl
  }

  "NrsProxyConnector" when {
    "submit with valid data" should {
      "be successful" in new Test {

        MockedHttpClient
          .post(
            url = s"$baseUrl/mtd-api-nrs-proxy/$nino/itsa-eops",
            body = submitEndOfPeriodRequestBody
          ).returns(Future.successful((): Unit))

        await(connector.submit(nino, submitEndOfPeriodRequestBody)) shouldBe (():Unit)
      }
    }
  }
}