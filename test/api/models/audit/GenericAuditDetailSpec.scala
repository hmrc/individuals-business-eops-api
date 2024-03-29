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

package api.models.audit

import api.models.errors.TaxYearFormatError
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class GenericAuditDetailSpec extends UnitSpec {

  val nino: String                         = "XX751130C"
  val taxYear: String                      = "2020-21"
  val userType: String                     = "Agent"
  val agentReferenceNumber: Option[String] = Some("012345678")
  val correlationId: String                = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  val versionNumber: String                = "3.0"

  val auditDetailJsonSuccess: JsValue = Json.parse(
    s"""
       |{
       |   "versionNumber":"$versionNumber",
       |   "userType":"$userType",
       |   "agentReferenceNumber":"${agentReferenceNumber.get}",
       |   "taxYear":"$taxYear",
       |   "nino":"$nino",
       |   "request":{
       |      "taxAvoidance":[
       |         {
       |            "srn":"14213/1123",
       |            "taxYear":"2019-20"
       |         }
       |      ],
       |      "class2Nics":{
       |         "voluntaryContributions":true
       |      }
       |   },
       |   "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c253",
       |   "response":{
       |      "httpStatus":200,
       |      "body":{
       |         "links":[
       |            {
       |               "href":"/individuals/disclosures/$nino/$taxYear",
       |               "rel":"create-and-amend-disclosures",
       |               "method":"PUT"
       |            },
       |            {
       |               "href":"/individuals/disclosures/$nino/$taxYear",
       |               "rel":"self",
       |               "method":"GET"
       |            },
       |            {
       |               "href":"/individuals/disclosures/$nino/$taxYear",
       |               "rel":"delete-disclosures",
       |               "method":"DELETE"
       |            }
       |         ]
       |      }
       |   }
       |}
    """.stripMargin
  )

  val auditDetailModelSuccess: GenericAuditDetail = GenericAuditDetail(
    versionNumber = versionNumber,
    userType = userType,
    agentReferenceNumber = agentReferenceNumber,
    params = Map("nino" -> nino, "taxYear" -> taxYear),
    requestBody = Some(
      Json.parse(
        """
          |{
          |   "taxAvoidance":[
          |      {
          |         "srn":"14213/1123",
          |         "taxYear":"2019-20"
          |      }
          |   ],
          |   "class2Nics":{
          |      "voluntaryContributions":true
          |   }
          |}
        """.stripMargin
      )),
    `X-CorrelationId` = correlationId,
    auditResponse = AuditResponse(
      OK,
      Right(Some(Json.parse(s"""
                               |{
                               |   "links":[
                               |      {
                               |         "href":"/individuals/disclosures/$nino/$taxYear",
                               |         "rel":"create-and-amend-disclosures",
                               |         "method":"PUT"
                               |      },
                               |      {
                               |         "href":"/individuals/disclosures/$nino/$taxYear",
                               |         "rel":"self",
                               |         "method":"GET"
                               |      },
                               |      {
                               |         "href":"/individuals/disclosures/$nino/$taxYear",
                               |         "rel":"delete-disclosures",
                               |         "method":"DELETE"
                               |      }
                               |   ]
                               |}
        """.stripMargin)))
    )
  )

  val invalidTaxYearAuditDetailJson: JsValue = Json.parse(
    s"""
       |{
       |   "versionNumber":"$versionNumber",
       |   "userType":"$userType",
       |   "agentReferenceNumber":"${agentReferenceNumber.get}",
       |   "taxYear":"2020-2021",
       |   "nino":"$nino",
       |   "request":{
       |      "taxAvoidance":[
       |         {
       |            "srn":"14213/1123",
       |            "taxYear":"2019-20"
       |         }
       |      ],
       |      "class2Nics":{
       |         "voluntaryContributions":true
       |      }
       |   },
       |   "X-CorrelationId":"a1e8057e-fbbc-47a8-a8b4-78d9f015c253",
       |   "response":{
       |      "httpStatus":400,
       |      "errors":[
       |         {
       |            "errorCode":"FORMAT_TAX_YEAR"
       |         }
       |      ]
       |   }
       |}
    """.stripMargin
  )

  val invalidTaxYearAuditDetailModel: GenericAuditDetail = GenericAuditDetail(
    versionNumber = versionNumber,
    userType = userType,
    agentReferenceNumber = agentReferenceNumber,
    params = Map("nino" -> nino, "taxYear" -> "2020-2021"),
    requestBody = Some(
      Json.parse(
        """
          |{
          |   "taxAvoidance":[
          |      {
          |         "srn":"14213/1123",
          |         "taxYear":"2019-20"
          |      }
          |   ],
          |   "class2Nics":{
          |      "voluntaryContributions":true
          |   }
          |}
      """.stripMargin
      )),
    `X-CorrelationId` = correlationId,
    auditResponse = AuditResponse(BAD_REQUEST, Left(List(AuditError(TaxYearFormatError.code))))
  )

  "GenericAuditDetail" when {
    "written to JSON (success)" should {
      "produce the expected JsObject" in {
        Json.toJson(auditDetailModelSuccess) shouldBe auditDetailJsonSuccess
      }
    }

    "written to JSON (error)" should {
      "produce the expected JsObject" in {
        Json.toJson(invalidTaxYearAuditDetailModel) shouldBe invalidTaxYearAuditDetailJson
      }
    }
  }

}
