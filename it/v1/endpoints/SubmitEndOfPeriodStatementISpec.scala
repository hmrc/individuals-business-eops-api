/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}
import itData.SubmitEndOfPeriodStatementData._

class SubmitEndOfPeriodStatementISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino = "AA123456A"

    val incomeSourceId: String = "XAIS12345678910"

    def uri: String = s"/$nino"

    def desUri(nino: String = nino,
               incomeSourceType:String = "self-employment",
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
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def errorBody(code: String, message: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "$message"
         |      }
    """.stripMargin
  }

  "Calling the retrieve endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.POST, desUri(), Map("incomeSourceId" -> incomeSourceId), Status.NO_CONTENT)
        }

        val response: WSResponse = await(request().post(fullValidJson()))
        response.status shouldBe Status.NO_CONTENT
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
          ("Badnino", fullValidJson(), Status.BAD_REQUEST, NinoFormatError),
          ("AA123456A", fullValidJson(typeOfBusiness = "error"), Status.BAD_REQUEST, TypeOfBusinessFormatError),
          ("AA123456A", fullValidJson(businessId = "error"), Status.BAD_REQUEST, BusinessIdFormatError),
          ("AA123456A", fullValidJson(startDate = "error"), Status.BAD_REQUEST, StartDateFormatError),
          ("AA123456A", fullValidJson(endDate = "error"), Status.BAD_REQUEST, EndDateFormatError),
          ("AA123456A", fullValidJson(finalised = "\"error\""), Status.BAD_REQUEST, FinalisedFormatError),
          ("AA123456A", Json.parse("{}"), Status.BAD_REQUEST, RuleIncorrectOrEmptyBodyError),
          ("AA123456A", fullValidJson(endDate ="2021-04-05"), Status.BAD_REQUEST, RangeEndDateBeforeStartDateError),
          ("AA123456A", fullValidJson(finalised = "false"), Status.BAD_REQUEST, RuleNotFinalisedError),
          ("AA123456A", fullValidJson("error","error","error","error","\"error\""), Status.BAD_REQUEST, BadRequestError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "des service error" when {
        def serviceErrorTest(desStatus: Int, desCode: String, desMessage: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns an $desCode error and status $desStatus with message $desMessage" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DesStub.onError(DesStub.POST, desUri(), Map("incomeSourceId" -> incomeSourceId), desStatus, errorBody(desCode,desMessage))
            }

            val response: WSResponse = await(request().post(fullValidJson()))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        //scalastyle:off
        val input : Seq[(Int, String, String, Int, MtdError)]= Seq(
          (Status.BAD_REQUEST, "INVALID_CORRELATIONID", "", Status.INTERNAL_SERVER_ERROR, DownstreamError),
          (Status.BAD_REQUEST, "INVALID_IDTYPE", "Submission has not passed validation. Invalid parameter idType.", Status.INTERNAL_SERVER_ERROR, DownstreamError),
          (Status.BAD_REQUEST, "INVALID_IDVALUE", "Submission has not passed validation. Invalid parameter idValue.", Status.BAD_REQUEST, NinoFormatError),
          (Status.BAD_REQUEST, "INVALID_ACCOUNTINGPERIODSTARTDATE", "Submission has not passed validation. Invalid parameter accountingPeriodStartDate.", Status.BAD_REQUEST, StartDateFormatError),
          (Status.BAD_REQUEST, "INVALID_ACCOUNTINGPERIODENDDATE", "Submission has not passed validation. Invalid parameter accountingPeriodEndDate.", Status.BAD_REQUEST, EndDateFormatError),
          (Status.BAD_REQUEST, "INVALID_INCOMESOURCEID", "Submission has not passed validation. Invalid parameter incomeSourceId.", Status.BAD_REQUEST, BusinessIdFormatError),
          (Status.BAD_REQUEST, "INVALID_INCOMESOURCETYPE", "Submission has not passed validation. Invalid parameter incomeSourceType.", Status.BAD_REQUEST, TypeOfBusinessFormatError),
          (Status.CONFLICT, "CONFLICT", "The remote endpoint has indicated that the taxation period has already been finalised", Status.FORBIDDEN, RuleAlreadySubmittedError),
          (Status.FORBIDDEN, "EARLY_SUBMISSION", "The remote endpoint has indicated that an early submission has been made before accounting period end date.", Status.FORBIDDEN, RuleEarlySubmissionError),
          (Status.FORBIDDEN, "LATE_SUBMISSION", "The remote endpoint has indicated that the period to finalise has passed", Status.FORBIDDEN, RuleLateSubmissionError),
          (Status.FORBIDDEN, "NON_MATCHING_PERIOD", "The remote endpoint has indicated that submission cannot be made with no matching accounting period.", Status.FORBIDDEN, RuleNonMatchingPeriodError),
          (Status.NOT_FOUND, "NOT_FOUND", "The remote endpoint has indicated that no income submissions exists", Status.NOT_FOUND, NotFoundError),
          (Status.NOT_FOUND, "NOT_FOUND", "The remote endpoint has indicated that no income source found", Status.NOT_FOUND, NotFoundError),
          (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "DES is currently experiencing problems that require live service intervention.", Status.INTERNAL_SERVER_ERROR, DownstreamError),
          (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Dependent systems are currently not responding.", Status.INTERNAL_SERVER_ERROR, DownstreamError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }

      "bvr service error" when {
        def serviceErrorTest(desStatus: Int, bvrError: JsValue, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns a bvr error and status $desStatus with message - ${bvrError.toString()}" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DesStub.onError(DesStub.POST, desUri(), Map("incomeSourceId" -> incomeSourceId), desStatus, bvrError.toString())
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
            |""".stripMargin
        )

        val input : Seq[(Int, JsValue, Int, MtdError)]= Seq(
          (Status.FORBIDDEN, bvr("C55503"), Status.FORBIDDEN, RuleConsolidatedExpensesError),
          (Status.FORBIDDEN, bvr("C55316"), Status.FORBIDDEN, RuleConsolidatedExpensesError),
          (Status.FORBIDDEN, bvr("C55008"), Status.FORBIDDEN, RuleMismatchedStartDateError),
          (Status.FORBIDDEN, bvr("C55013"), Status.FORBIDDEN, RuleMismatchedEndDateError),
          (Status.FORBIDDEN, bvr("C55014"), Status.FORBIDDEN, RuleMismatchedEndDateError),
          (Status.FORBIDDEN, bvr("C55317"), Status.FORBIDDEN, RuleClass4Over16Error),
          (Status.FORBIDDEN, bvr("C55318"), Status.FORBIDDEN, RuleClass4PensionAge),
          (Status.FORBIDDEN, bvr("C55501"), Status.FORBIDDEN, RuleFHLPrivateUseAdjustment),
          (Status.FORBIDDEN, bvr("C55502"), Status.FORBIDDEN, RuleNonFHLPrivateUseAdjustment),
          (Status.FORBIDDEN, bvrMultiple, Status.FORBIDDEN, BVRError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
