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

package v2.endpoints

import api.models.domain.TaxYear
import api.models.errors._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itData.SubmitEndOfPeriodStatementData._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.V2IntegrationBaseSpec
import v2.stubs._

class SubmitEndOfPeriodStatementControllerTysISpec extends V2IntegrationBaseSpec {

  "Calling the submit eops endpoint" should {

    "return a 204 status code" when {

      "any valid request is made with a successful NRS call in a Tax Year Specific tax year" in new TysIfsTest {
        val nrsSuccess: JsValue = Json.parse(
          s"""
             |{
             |  "nrSubmissionId":"2dd537bc-4244-4ebf-bac9-96321be13cdc",
             |  "cadesTSignature":"30820b4f06092a864886f70111111111c0445c464",
             |  "timestamp":""
             |}
         """.stripMargin
        )

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          NrsStub.onSuccess(NrsStub.POST, s"/mtd-api-nrs-proxy/$nino/itsa-eops", ACCEPTED, nrsSuccess)
          DownstreamStub.onSuccess(DownstreamStub.POST, downstreamUri, ACCEPTED)
        }

        val response: WSResponse = await(request().post(validMtdRequestJson))
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "any valid request is made with a failed NRS call in a Tax Year Specific tax year" in new TysIfsTest {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          NrsStub.onError(NrsStub.POST, s"/mtd-api-nrs-proxy/$nino/itsa-eops", INTERNAL_SERVER_ERROR, InternalError.message)
        }

        val response: WSResponse = await(request().post(validMtdRequestJson))
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return a 400 status code" when {

      "any invalid request is made and downstream returns an error" in new TysIfsTest {
        val nrsSuccess: JsValue = Json.parse(
          s"""
             |{
             |  "nrSubmissionId":"2dd537bc-4244-4ebf-bac9-96321be13cdc",
             |  "cadesTSignature":"30820b4f06092a864886f70111111111c0445c464",
             |  "timestamp":""
             |}
         """.stripMargin
        )

        val downstreamResponse: String = s"""
          |{
          | "failures": [
          |    {
          |      "code": "INVALID_INCOME_SOURCE_TYPE",
          |      "reason": "Submission has not passed validation. Invalid parameter incomeSourceType."
          |    },
          |    {
          |      "code": "INVALID_PAYLOAD",
          |      "reason": "Submission has not passed validation. Invalid payload."
          |    }
          |  ]
          |}""".stripMargin

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          NrsStub.onSuccess(NrsStub.POST, s"/mtd-api-nrs-proxy/$nino/itsa-eops", ACCEPTED, nrsSuccess)
          DownstreamStub.onError(DownstreamStub.POST, downstreamUri, BAD_REQUEST, downstreamResponse)
        }

        val response: WSResponse = await(request().post(validMtdRequestJson))
        response.status shouldBe INTERNAL_SERVER_ERROR
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationError(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new TysIfsTest {

            override def nino: String = requestNino

            override def setupStubs(): StubMapping = {
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(requestNino)
            }

            val response: WSResponse = await(request().post(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("Badnino", fullValidJson(), BAD_REQUEST, NinoFormatError),
          ("AA123456A", fullValidJson(typeOfBusiness = "error"), BAD_REQUEST, TypeOfBusinessFormatError),
          ("AA123456A", fullValidJson(businessId = "error"), BAD_REQUEST, BusinessIdFormatError),
          ("AA123456A", fullValidJson(startDate = "error"), BAD_REQUEST, StartDateFormatError),
          ("AA123456A", fullValidJson(endDate = "error"), BAD_REQUEST, EndDateFormatError),
          ("AA123456A", fullValidJson(finalised = "false"), BAD_REQUEST, FinalisedFormatError),
          ("AA123456A", Json.parse("{}"), BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123456A", fullValidJson(endDate = "2021-04-05"), BAD_REQUEST, RuleEndBeforeStartDateError)
        )

        input.foreach(args => (validationError _).tupled(args))
      }

      "downstream service error" when {

        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedError: MtdError): Unit =
          s"downstream returns $downstreamCode with status $downstreamStatus" in new TysIfsTest {
            override def setupStubs(): StubMapping = {
              val downstreamErrorBody = s"""
                 | {
                 |   "failures": [
                 |     {
                 |      "code": "$downstreamCode",
                 |      "reason": "downstream message"
                 |     }
                 |   ]
                 | }
            """.stripMargin

              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)

              DownstreamStub.onError(DownstreamStub.POST, downstreamUri, downstreamStatus, downstreamErrorBody)
            }

            val response: WSResponse = await(request().post(validMtdRequestJson))
            response.json shouldBe Json.toJson(expectedError)
            response.status shouldBe expectedStatus
          }

        val errors: Seq[(Int, String, Int, MtdError)] = List(
          (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
          (CONFLICT, "CONFLICT", BAD_REQUEST, RuleAlreadySubmittedError),
          (UNPROCESSABLE_ENTITY, "EARLY_SUBMISSION", BAD_REQUEST, RuleEarlySubmissionError),
          (UNPROCESSABLE_ENTITY, "LATE_SUBMISSION", BAD_REQUEST, RuleLateSubmissionError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors: Seq[(Int, String, Int, MtdError)] = List(
          (BAD_REQUEST, "INVALID_TAX_YEAR", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_START_DATE", BAD_REQUEST, StartDateFormatError),
          (BAD_REQUEST, "INVALID_END_DATE", BAD_REQUEST, EndDateFormatError),
          (BAD_REQUEST, "INVALID_INCOME_SOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
          (BAD_REQUEST, "INVALID_INCOME_SOURCE_TYPE", BAD_REQUEST, TypeOfBusinessFormatError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_QUERY_PARAMETERS", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "PERIOD_MISMATCH", BAD_REQUEST, RuleNonMatchingPeriodError),
          (UNPROCESSABLE_ENTITY, "BVR_FAILURE", BAD_REQUEST, RuleBusinessValidationFailureTys),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {

    protected def nino: String = "AA123456A"

    private val mtdUri = s"/$nino"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(mtdUri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"), (AUTHORIZATION, "Bearer 123"))
    }

  }

  private trait TysIfsTest extends Test {

    protected val accountingPeriodStartDate = "2023-04-06"
    protected val accountingPeriodEndDate   = "2024-04-05"
    protected val taxYear: TaxYear          = TaxYear.fromIso(accountingPeriodEndDate)

    protected val validMtdRequestJson: JsValue =
      fullValidJson(typeOfBusiness = "foreign-property", startDate = accountingPeriodStartDate, endDate = accountingPeriodEndDate)

    def downstreamUri: String =
      s"/income-tax/income-sources/${taxYear.asTysDownstream}/" +
        s"$nino/$incomeSourceId/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration"

  }

}
