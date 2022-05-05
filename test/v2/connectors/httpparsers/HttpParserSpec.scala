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

package v2.connectors.httpparsers

import play.api.http.Status.{ BAD_REQUEST, CONFLICT, FORBIDDEN, NOT_FOUND, UNPROCESSABLE_ENTITY }
import play.api.libs.json.{ JsValue, Json }
import support.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import v2.models.errors.{ IfsErrorCode, IfsErrors }

class HttpParserSpec extends UnitSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  "The generic HTTP parser for empty response" when {
    val httpParser: HttpParser = new HttpParser {}

    Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, CONFLICT, UNPROCESSABLE_ENTITY).foreach(
      responseCode =>
        s"receiving a $responseCode response" should {
          "be able to parse a single error" in {
            val singleErrorJson: JsValue = Json.parse(
              """
                |{
                |   "code": "CODE",
                |   "reason": "MESSAGE"
                |}""".stripMargin
            )

            val httpResponse = HttpResponse(responseCode, singleErrorJson, Map("CorrelationId" -> Seq(correlationId)))

            httpParser.parseErrors(httpResponse) shouldBe IfsErrors(List(IfsErrorCode("CODE")))
          }

          "be able to parse multiple errors" in {
            val multipleErrorsJson: JsValue = Json.parse(
              """
                |{
                |   "failures": [
                |       {
                |           "code": "CODE 1",
                |           "reason": "MESSAGE 1"
                |       },
                |       {
                |           "code": "CODE 2",
                |           "reason": "MESSAGE 2"
                |       }
                |   ]
                |}""".stripMargin
            )

            val httpResponse = HttpResponse(responseCode, multipleErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

            httpParser.parseErrors(httpResponse) shouldBe IfsErrors(List(IfsErrorCode("CODE 1"), IfsErrorCode("CODE 2")))
          }

          "be able to parse a single bvr error" in {
            val id      = "C55013"
            val message = "Custom message"
            val expectedBvrErrorsJson: JsValue = Json.parse(
              s"""
                |{
                |   "bvrfailureResponseElement":{
                |      "code":"BVR_FAILURE_EXISTS",
                |      "reason":"Ignored top-level reason",
                |      "validationRuleFailures":[
                |         {
                |            "id": "$id",
                |            "type":"ERR",
                |            "text":"$message"
                |         }
                |      ]
                |   }
                |}""".stripMargin
            )

            val httpResponse = HttpResponse(responseCode, expectedBvrErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

            httpParser.parseErrors(httpResponse) shouldBe IfsErrors(List(IfsErrorCode("BVR_FAILURE_EXISTS", Some(id), Some(message))))
          }

          "be able to parse multiple bvr errors" in {
            val id1      = "C55001"
            val message1 = "Custom message one"
            val id2      = "C55002"
            val message2 = "Custom message two"
            val multipleBvrErrorsJson: JsValue = Json.parse(
              s"""
                |{
                |   "bvrfailureResponseElement":{
                |      "code":"BVR_FAILURE_EXISTS",
                |      "reason":"Ignored top-level reason",
                |      "validationRuleFailures":[
                |         {
                |            "id": "$id1",
                |            "type":"ERR",
                |            "text":"$message1"
                |         },
                |         {
                |            "id": "$id2",
                |            "type":"ERR",
                |            "text":"$message2"
                |         }
                |      ]
                |   }
                |}""".stripMargin
            )

            val httpResponse = HttpResponse(responseCode, multipleBvrErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

            httpParser.parseErrors(httpResponse) shouldBe IfsErrors(
              List(
                IfsErrorCode("BVR_FAILURE_EXISTS", Some(id1), Some(message1)),
                IfsErrorCode("BVR_FAILURE_EXISTS", Some(id2), Some(message2)),
              ))
          }

          "ignore non ERR  bvr errors" in {
            fail("should we ????")
          }

          "why are these tests response code specific?" in { fail() }

          "be able to parse bvr errors but not expected error code" in {
            val notExpectedBvrErrorsJson: JsValue = Json.parse(
              """
                |{
                |   "bvrfailureResponseElement":{
                |      "code":"BVR_FAILURE_EXISTS",
                |      "reason":"The period submission ...",
                |      "validationRuleFailures":[
                |         {
                |            "id":"C550136",
                |            "type":"ERR",
                |            "text":"Period submission ..."
                |         }
                |      ]
                |   }
                |}""".stripMargin
            )
            val httpResponse = HttpResponse(responseCode, notExpectedBvrErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

            httpParser.parseErrors(httpResponse) shouldBe IfsErrors(List(IfsErrorCode("BVR_UNKNOWN_ID")))
            fail("not required?")
          }

          "be able to parse multiple bvr errors but contains unexpected error code" in {
            val multipleBvrErrorsJson: JsValue = Json.parse(
              """
                |{
                |   "bvrfailureResponseElement":{
                |      "code":"BVR_FAILURE_EXISTS",
                |      "reason":"The period submission ...",
                |      "validationRuleFailures":[
                |         {
                |            "id":"C55013",
                |            "type":"ERR",
                |            "text":"Period submission ..."
                |         },
                |         {
                |            "id":"C550136",
                |            "type":"ERR",
                |            "text":"Period submission ..."
                |         }
                |      ]
                |   }
                |}""".stripMargin
            )

            val httpResponse = HttpResponse(responseCode, multipleBvrErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

            httpParser.parseErrors(httpResponse) shouldBe IfsErrors(List(IfsErrorCode("C55013"), IfsErrorCode("BVR_UNKNOWN_ID")))
            fail("not required?")
          }
      }
    )
  }
}
