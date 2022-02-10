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

package v1r7.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.V1R7IntegrationBaseSpec
import v1r7.models.errors._
import v1r7.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub, NrsStub}
import itData.SubmitEndOfPeriodStatementData._

class SubmitEndOfPeriodStatementISpec extends V1R7IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"

    val incomeSourceId: String = "XAIS12345678910"

    def uri: String = s"/$nino"

    def ifsUri(nino: String = nino,
               incomeSourceType: String = "self-employment",
               accountingPeriodStartDate: String = "2021-04-06",
               accountingPeriodEndDate: String = "2022-04-05"
              ): String = {
      s"/income-sources/nino/" +
        s"$nino/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration"
    }

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def errorBody(code: String, message: String): String =
      s"""
         |{
         |  "code": "$code",
         |  "reason": "$message"
         |}
    """.stripMargin
  }

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
          AuditStub.audit()
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
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          NrsStub.onError(NrsStub.POST, s"/mtd-api-nrs-proxy/$nino/itsa-eops", INTERNAL_SERVER_ERROR, DownstreamError.message)
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe NO_CONTENT
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String, requestBody: JsValue,
                                expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error ${
            if(expectedBody.equals(TaxYearFormatError)) java.util.UUID.randomUUID else ""
          }" in new Test {

            override val nino: String = requestNino

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(requestNino)
            }

            val response: WSResponse = await(request().post(requestBody))
            response.status shouldBe expectedStatus

            if(expectedBody.equals(BadRequestError)){
              lazy val multipleErrors: Seq[MtdError] = Seq(
                FinalisedFormatError,
                StartDateFormatError,
                EndDateFormatError,
                TypeOfBusinessFormatError,
                BusinessIdFormatError,
              )
              lazy val multipleErrorsJson = Json.toJson(expectedBody).as[JsObject] + ("errors" -> Json.toJson(multipleErrors))
              response.json shouldBe multipleErrorsJson
            } else {
              response.json shouldBe Json.toJson(expectedBody)
            }
          }
        }

        val input = Seq(
          ("Badnino", fullValidJson(), BAD_REQUEST, NinoFormatError),
          ("AA123456A", fullValidJson(typeOfBusiness = "error"), BAD_REQUEST, TypeOfBusinessFormatError),
          ("AA123456A", fullValidJson(businessId = "error"), BAD_REQUEST, BusinessIdFormatError),
          ("AA123456A", fullValidJson(startDate = "error"), BAD_REQUEST, StartDateFormatError),
          ("AA123456A", fullValidJson(endDate = "error"), BAD_REQUEST, EndDateFormatError),
          ("AA123456A", fullValidJson(finalised = "\"error\""), BAD_REQUEST, FinalisedFormatError),
          ("AA123456A", Json.parse("{}"), BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123456A", fullValidJson(endDate ="2021-04-05"), BAD_REQUEST, RangeEndDateBeforeStartDateError),
          ("AA123456A", fullValidJson(finalised = "false"), BAD_REQUEST, RuleNotFinalisedError),
          ("AA123456A", fullValidJson("error","error","error","error","\"error\""), BAD_REQUEST, BadRequestError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "ifs service error" when {
        def serviceErrorTest(ifsStatus: Int, ifsCode: String, ifsMessage: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns an $ifsCode error and status $ifsStatus with message $ifsMessage" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.POST, ifsUri(), Map("incomeSourceId" -> incomeSourceId), ifsStatus, errorBody(ifsCode, ifsMessage))
            }

            val response: WSResponse = await(request().post(fullValidJson()))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        //scalastyle:off
        val input : Seq[(Int, String, String, Int, MtdError)]= Seq(
          (BAD_REQUEST, "INVALID_IDTYPE", "Submission has not passed validation. Invalid parameter idType.", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_IDVALUE", "Submission has not passed validation. Invalid parameter idValue.", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_ACCOUNTINGPERIODSTARTDATE", "Submission has not passed validation. Invalid parameter accountingPeriodStartDate.", BAD_REQUEST, StartDateFormatError),
          (BAD_REQUEST, "INVALID_ACCOUNTINGPERIODENDDATE", "Submission has not passed validation. Invalid parameter accountingPeriodEndDate.", BAD_REQUEST, EndDateFormatError),
          (BAD_REQUEST, "INVALID_INCOMESOURCEID", "Submission has not passed validation. Invalid parameter incomeSourceId.", BAD_REQUEST, BusinessIdFormatError),
          (BAD_REQUEST, "INVALID_INCOMESOURCETYPE", "Submission has not passed validation. Invalid parameter incomeSourceType.", BAD_REQUEST, TypeOfBusinessFormatError),
          (BAD_REQUEST, "MISSING_INCOMESOURCEID", "The remote endpoint has indicated that required query parameters are missing.", INTERNAL_SERVER_ERROR, DownstreamError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", "Submission has not passed validation. Invalid header CorrelationId.", INTERNAL_SERVER_ERROR, DownstreamError),
          (FORBIDDEN, "EARLY_SUBMISSION", "The remote endpoint has indicated that an early submission has been made before accounting period end date.", FORBIDDEN, RuleEarlySubmissionError),
          (FORBIDDEN, "LATE_SUBMISSION", "The remote endpoint has indicated that the period to finalise has passed.", FORBIDDEN, RuleLateSubmissionError),
          (FORBIDDEN, "NON_MATCHING_PERIOD", "The remote endpoint has indicated that submission cannot be made with no matching accounting period.", FORBIDDEN, RuleNonMatchingPeriodError),
          (NOT_FOUND, "NOT_FOUND", "The remote endpoint has indicated that no income source found.", NOT_FOUND, NotFoundError),
          (NOT_FOUND, "NOT_FOUND", "The remote endpoint has indicated that no income submissions exists.", NOT_FOUND, NotFoundError),
          (CONFLICT, "CONFLICT", "The remote endpoint has indicated that the taxation period has already been finalised.", FORBIDDEN, RuleAlreadySubmittedError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", "IF is currently experiencing problems that require live service intervention.", INTERNAL_SERVER_ERROR, DownstreamError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.", INTERNAL_SERVER_ERROR, DownstreamError),
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }

      "bvr service error" when {
        def serviceErrorTest(ifsStatus: Int, bvrError: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"ifs returns a bvr error and status $ifsStatus with message - ${bvrError.toString()}" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DownstreamStub.onError(DownstreamStub.POST, ifsUri(), Map("incomeSourceId" -> incomeSourceId), ifsStatus, bvrError.toString())
            }

            val response: WSResponse = await(request().post(fullValidJson()))
            response.status shouldBe expectedStatus

            if(expectedBody.equals(BVRError)){
              lazy val multipleErrors: Seq[MtdError] = Seq(
                RuleConsolidatedExpensesError,
                RuleMismatchedStartDateError,
                RuleMismatchedEndDateError,
                RuleClass4Over16Error,
                RuleClass4PensionAge,
                RuleFHLPrivateUseAdjustment,
                RuleNonFHLPrivateUseAdjustment
              )
              lazy val multipleErrorsJson = Json.toJson(expectedBody).as[JsObject] + ("errors" -> Json.toJson(multipleErrors))
              response.json shouldBe multipleErrorsJson
            } else {
              response.json shouldBe Json.toJson(expectedBody)
            }
          }
        }

        def bvr(code: String, text: String = "text") = Json.parse(
          s"""
            |{
            |    "bvrfailureResponseElement": {
            |        "code": "BVR_FAILURE_EXISTS",
            |        "reason": "The remote endpoint has indicated that there are bvr failures",
            |        "validationRuleFailures": [
            |            {
            |                "id": "$code",
            |                "type": "ERR",
            |                "text": "$text"
            |            }
            |        ]
            |    }
            |}
            |""".stripMargin)

        val bvrMultiple = Json.parse(
          """
            |{
            |    "bvrfailureResponseElement": {
            |        "code": "BVR_FAILURE_EXISTS",
            |        "reason": "The remote endpoint has indicated that there are bvr failures",
            |        "validationRuleFailures": [
            |            {
            |                "id": "C55503",
            |                "type": "ERR",
            |                "text": "C55503 text"
            |            },{
            |                "id": "C55316",
            |                "type": "ERR",
            |                "text": "C55316 text"
            |            },{
            |                "id": "C55008",
            |                "type": "ERR",
            |                "text": "C55008 text"
            |            },{
            |                "id": "C55013",
            |                "type": "ERR",
            |                "text": "C55013 text"
            |            },{
            |                "id": "C55014",
            |                "type": "ERR",
            |                "text": "C55014 text"
            |            },{
            |                "id": "C55317",
            |                "type": "ERR",
            |                "text": "C55317 text"
            |            },{
            |                "id": "C55318",
            |                "type": "ERR",
            |                "text": "C55318 text"
            |            },{
            |                "id": "C55501",
            |                "type": "ERR",
            |                "text": "C55501 text"
            |            },{
            |                "id": "C55502",
            |                "type": "ERR",
            |                "text": "C55502 text"
            |            }
            |        ]
            |    }
            |}
          """.stripMargin
        )

        val input : Seq[(Int, JsValue, Int, MtdError)]= Seq(
          (FORBIDDEN, bvr("C55503"), FORBIDDEN, RuleConsolidatedExpensesError),
          (FORBIDDEN, bvr("C55316"), FORBIDDEN, RuleConsolidatedExpensesError),
          (FORBIDDEN, bvr("C55008"), FORBIDDEN, RuleMismatchedStartDateError),
          (FORBIDDEN, bvr("C55013"), FORBIDDEN, RuleMismatchedEndDateError),
          (FORBIDDEN, bvr("C55014"), FORBIDDEN, RuleMismatchedEndDateError),
          (FORBIDDEN, bvr("C55317"), FORBIDDEN, RuleClass4Over16Error),
          (FORBIDDEN, bvr("C55318"), FORBIDDEN, RuleClass4PensionAge),
          (FORBIDDEN, bvr("C55501"), FORBIDDEN, RuleFHLPrivateUseAdjustment),
          (FORBIDDEN, bvr("C55502"), FORBIDDEN, RuleNonFHLPrivateUseAdjustment),
          (FORBIDDEN, bvrMultiple, FORBIDDEN, BVRError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}