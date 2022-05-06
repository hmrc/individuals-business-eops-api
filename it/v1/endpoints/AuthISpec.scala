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

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itData.SubmitEndOfPeriodStatementData.fullValidJson
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.V1IntegrationBaseSpec
import v1.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class AuthISpec extends V1IntegrationBaseSpec {

  private trait Test {

    val nino = "AA123456A"

    val incomeSourceId: String = "XAIS12345678910"

    def uri: String = s"/$nino"

    def ifsUri(nino: String = nino,
               incomeSourceType: String = "self-employment",
               accountingPeriodStartDate: String = "2021-04-06",
               accountingPeriodEndDate: String = "2022-04-05"
              ): String = {
      s"/income-tax/income-sources/nino/" +
        s"$nino/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration"
    }

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123")) // some bearer token))
    }

    def errorBody(code: String, message: String): String =
      s"""
         |{
         |  "code": "$code",
         |  "reason": "$message"
         |}
    """.stripMargin
  }

  "Calling the submit eops endpoint" when {

    "the NINO cannot be converted to a MTD ID" should {

      "return 500" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.internalServerError(nino)
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "an MTD ID is successfully retrieve from the NINO and the user is authorised" should {

      "return 204" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.POST, ifsUri(), Map("incomeSourceId" -> incomeSourceId), Status.NO_CONTENT)
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe Status.NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "an MTD ID is successfully retrieve from the NINO and the user is NOT logged in" should {

      "return 403" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.unauthorisedNotLoggedIn()
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe Status.FORBIDDEN
      }
    }

    "an MTD ID is successfully retrieve from the NINO and the user is NOT authorised" should {

      "return 403" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.unauthorisedOther()
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe Status.FORBIDDEN
      }
    }
  }
}