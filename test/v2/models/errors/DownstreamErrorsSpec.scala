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

package v2.models.errors

import play.api.libs.json.{ JsError, JsValue, Json }
import support.UnitSpec

class DownstreamErrorsSpec extends UnitSpec {

  "IfsErrors" should {
    "be able to read a JSON standard error format" in {
      val downstreamErrorsJson: JsValue = Json.parse(
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

      downstreamErrorsJson.as[DownstreamError] shouldBe DownstreamStandardError(List(DownstreamErrorCode("CODE 1"), DownstreamErrorCode("CODE 2")))
    }

    "be able to read a JSON bvr error format" in {
      val downstreamErrorsJson: JsValue = Json.parse(
        s"""
           |{
           |   "bvrfailureResponseElement":{
           |      "code":"CODE",
           |      "reason":"Ignored top-level reason",
           |      "validationRuleFailures":[
           |         {
           |            "id": "ID 0",
           |            "type":"ERR",
           |            "text":"MESSAGE 0"
           |         },
           |         {
           |            "id": "ID 1",
           |            "type":"INFO",
           |            "text":"MESSAGE 1"
           |         }
           |      ]
           |   }
           |}""".stripMargin
      )

      downstreamErrorsJson.as[DownstreamError] shouldBe DownstreamBvrError("CODE",
                                                                           List(
                                                                             DownstreamValidationRuleFailure("ID 0", "MESSAGE 0", "ERR"),
                                                                             DownstreamValidationRuleFailure("ID 1", "MESSAGE 1", "INFO"),
                                                                           ))
    }

    "fail if unrecognised error format is received" in {
      val downstreamErrorsJson: JsValue = Json.obj("something" -> "something")

      downstreamErrorsJson.validate[DownstreamError] shouldBe a[JsError]
    }
  }
}
