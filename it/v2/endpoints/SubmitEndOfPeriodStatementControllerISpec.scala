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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itData.SubmitEndOfPeriodStatementData._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.test.Helpers.AUTHORIZATION
import support.V2IntegrationBaseSpec
import v2.models.errors._
import v2.stubs._

class SubmitEndOfPeriodStatementISpec extends V2IntegrationBaseSpec {

  "Calling the submit eops endpoint" should {

    "return a 204 status code" when {

      "any valid request is made with a successful NRS call" in new Test {

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
          DownstreamStub.onSuccess(DownstreamStub.POST, ifsUri(), Map("incomeSourceId" -> incomeSourceId), NO_CONTENT)
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "any valid request is made with a failed NRS call" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          NrsStub.onError(NrsStub.POST, s"/mtd-api-nrs-proxy/$nino/itsa-eops", INTERNAL_SERVER_ERROR, InternalError.message)
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationError(requestNino: String, requestBody: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino

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
          ("AA123456A", fullValidJson(endDate = "2021-04-05"), BAD_REQUEST, RuleEndDateBeforeStartDateError),
        )

        input.foreach(args => (validationError _).tupled(args))
      }

      "ifs service error" when {
        def fullServiceErrorTest(ifsStatus: Int, downstreamResponse: JsValue, expectedStatus: Int, expectedBody: JsValue): Test = new Test {
          override def setupStubs(): StubMapping = {
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
            DownstreamStub.onError(DownstreamStub.POST, ifsUri(), Map("incomeSourceId" -> incomeSourceId), ifsStatus, downstreamResponse.toString())
          }

          val response: WSResponse = await(request().post(fullValidJson()))
          response.json shouldBe expectedBody
          response.status shouldBe expectedStatus
        }

        def serviceErrorTest(ifsStatus: Int, ifsCode: String, ifsMessage: String, expectedStatus: Int, expectedError: MtdError): Unit =
          s"ifs returns an $ifsCode error and status $ifsStatus with message $ifsMessage" in {
            val errorBody: JsValue = Json.parse(s"""
                                                   |{
                                                   |  "failures": [{
                                                   |  "code": "$ifsCode",
                                                   |  "reason": "$ifsMessage"
                                                   |  }]
                                                   |}""".stripMargin)
            fullServiceErrorTest(ifsStatus, errorBody, expectedStatus, Json.toJson(expectedError))
          }

        val input: Seq[(Int, String, String, Int, MtdError)] = Seq(
          (BAD_REQUEST, "INVALID_IDTYPE", "Submission has not passed validation. Invalid parameter idType.", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_IDVALUE", "Submission has not passed validation. Invalid parameter idValue.", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST,
           "INVALID_ACCOUNTINGPERIODSTARTDATE",
           "Submission has not passed validation. Invalid parameter accountingPeriodStartDate.",
           BAD_REQUEST,
           StartDateFormatError),
          (BAD_REQUEST,
           "INVALID_ACCOUNTINGPERIODENDDATE",
           "Submission has not passed validation. Invalid parameter accountingPeriodEndDate.",
           BAD_REQUEST,
           EndDateFormatError),
          (BAD_REQUEST,
           "INVALID_INCOMESOURCEID",
           "Submission has not passed validation. Invalid parameter incomeSourceId.",
           BAD_REQUEST,
           BusinessIdFormatError),
          (BAD_REQUEST,
           "INVALID_INCOMESOURCETYPE",
           "Submission has not passed validation. Invalid parameter incomeSourceType.",
           BAD_REQUEST,
           TypeOfBusinessFormatError),
          (BAD_REQUEST,
           "INVALID_CORRELATIONID",
           "Submission has not passed validation. Invalid header CorrelationId.",
           INTERNAL_SERVER_ERROR,
           InternalError),
          (BAD_REQUEST,
           "EARLY_SUBMISSION",
           "The remote endpoint has indicated that an early submission has been made before accounting period end date.",
           BAD_REQUEST,
           RuleEarlySubmissionError),
          (BAD_REQUEST,
           "LATE_SUBMISSION",
           "The remote endpoint has indicated that the period to finalise has passed.",
           BAD_REQUEST,
           RuleLateSubmissionError),
          (BAD_REQUEST,
           "NON_MATCHING_PERIOD",
           "The remote endpoint has indicated that submission cannot be made with no matching accounting period.",
           BAD_REQUEST,
           RuleNonMatchingPeriodError),
          (NOT_FOUND, "NOT_FOUND", "The remote endpoint has indicated that no income source found.", NOT_FOUND, NotFoundError),
          (NOT_FOUND, "NOT_FOUND", "The remote endpoint has indicated that no income submissions exists.", NOT_FOUND, NotFoundError),
          (CONFLICT,
           "CONFLICT",
           "The remote endpoint has indicated that the taxation period has already been finalised.",
           BAD_REQUEST,
           RuleAlreadySubmittedError),
          (INTERNAL_SERVER_ERROR,
           "SERVER_ERROR",
           "IF is currently experiencing problems that require live service intervention.",
           INTERNAL_SERVER_ERROR,
           InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.", INTERNAL_SERVER_ERROR, InternalError),
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))

        "ifs returns a single BVR error" in {
          val ifsJson = Json.parse("""
                                     |{
                                     |    "bvrfailureResponseElement": {
                                     |        "code": "BVR_FAILURE_EXISTS",
                                     |        "reason": "Ignored",
                                     |        "validationRuleFailures": [
                                     |            {
                                     |                "id": "ID",
                                     |                "type": "ERR",
                                     |                "text": "MESSAGE"
                                     |            }
                                     |        ]
                                     |    }
                                     |}""".stripMargin)

          val mtdErrorJson = Json.parse("""{
                                          |   "code":"RULE_BUSINESS_VALIDATION_FAILURE",
                                          |   "errorId":"ID",
                                          |   "message":"MESSAGE"
                                          |}""".stripMargin)

          fullServiceErrorTest(FORBIDDEN, ifsJson, BAD_REQUEST, mtdErrorJson)
        }

        "ifs returns multiple BVR errors" in {
          val ifsJson = Json.parse("""{
                                     |    "bvrfailureResponseElement": {
                                     |        "code": "BVR_FAILURE_EXISTS",
                                     |        "reason": "Ignored",
                                     |        "validationRuleFailures": [
                                     |            {
                                     |                "id": "ID_0",
                                     |                "type": "ERR",
                                     |                "text": "MESSAGE_0"
                                     |            },{
                                     |                "id": "ID_1",
                                     |                "type": "ERR",
                                     |                "text": "MESSAGE_1"
                                     |            }
                                     |        ]
                                     |    }
                                     |}""".stripMargin)

          val mtdErrorJson = Json.parse("""{
                                          |   "code":"INVALID_REQUEST",
                                          |   "message":"Invalid request",
                                          |   "errors":[
                                          |      {
                                          |         "code":"RULE_BUSINESS_VALIDATION_FAILURE",
                                          |         "errorId": "ID_0",
                                          |         "message":"MESSAGE_0"
                                          |      },
                                          |      {
                                          |         "code":"RULE_BUSINESS_VALIDATION_FAILURE",
                                          |         "errorId": "ID_1",
                                          |         "message":"MESSAGE_1"
                                          |      }
                                          |   ]
                                          |}""".stripMargin)

          fullServiceErrorTest(FORBIDDEN, ifsJson, BAD_REQUEST, mtdErrorJson)
        }
      }
    }
  }

  private trait Test {

    val nino: String = "AA123456A"

    val incomeSourceId: String = "XAIS12345678910"

    def uri: String = s"/$nino"

    def ifsUri(nino: String = nino,
               incomeSourceType: String = "self-employment",
               accountingPeriodStartDate: String = "2021-04-06",
               accountingPeriodEndDate: String = "2022-04-05"): String = {
      s"/income-tax/income-sources/nino/" +
        s"$nino/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration"
    }

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"), (AUTHORIZATION, "Bearer 123"))
    }
  }
}
