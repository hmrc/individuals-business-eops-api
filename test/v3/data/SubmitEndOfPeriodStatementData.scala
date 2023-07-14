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

package v3.data

import api.models.downstream.TypeOfBusiness
import api.models.downstream.TypeOfBusiness.`foreign-property`
import play.api.libs.json.{JsValue, Json}
import v3.models.request.{AccountingPeriod, SubmitEndOfPeriod}

object SubmitEndOfPeriodStatementData {

  val incomeSourceType: TypeOfBusiness  = `foreign-property`
  val incomeSourceId: String            = "XAIS12345678910"
  val accountingPeriodStartDate: String = "2021-04-06"
  val accountingPeriodEndDate: String   = "2022-04-05"

  val accountingPeriodStartDateTys: String = "2023-04-06"
  val accountingPeriodEndDateTys: String   = "2024-04-05"

  val validRequest: SubmitEndOfPeriod = SubmitEndOfPeriod(
    typeOfBusiness = `foreign-property`,
    businessId = "XAIS12345678910",
    accountingPeriod = AccountingPeriod(
      startDate = "2021-04-06",
      endDate = "2022-04-05"
    ),
    finalised = true
  )

  val validTysRequest: SubmitEndOfPeriod = SubmitEndOfPeriod(
    typeOfBusiness = `foreign-property`,
    businessId = "XAIS12345678910",
    accountingPeriod = AccountingPeriod(
      startDate = "2023-04-06",
      endDate = "2024-04-05"
    ),
    finalised = true
  )

  def jsonRequestBody(typeOfBusiness: String = "self-employment",
                      businessId: String = "XAIS12345678910",
                      startDate: String = "2021-04-06",
                      endDate: String = "2022-04-05",
                      finalised: String = "true"): JsValue = Json.parse(
    s"""
       |{
       |  "typeOfBusiness":"$typeOfBusiness",
       |  "businessId":"$businessId",
       |  "accountingPeriod": {
       |    "startDate": "$startDate",
       |    "endDate":"$endDate"
       |  },
       |  "finalised": $finalised
       |}
     """.stripMargin
  )

  val successJson: JsValue = Json.parse(
    """
      |{
      |   "typeOfBusiness": "foreign-property",
      |   "businessId": "XAIS12345678910",
      |   "accountingPeriod": {
      |      "startDate": "2021-04-06",
      |      "endDate": "2022-04-05"
      |   },
      |   "finalised": true
      |}
    """.stripMargin
  )

  def fullValidAuditJson(typeOfBusiness: String = "self-employment",
                         businessId: String = "XAIS12345678910",
                         startDate: String = "2021-04-06",
                         endDate: String = "2022-04-05",
                         finalised: String = "true"): JsValue = Json.parse(
    s"""
       |{
       |  "typeOfBusiness":"$typeOfBusiness",
       |  "businessId":"$businessId",
       |  "accountingPeriod": {
       |    "startDate": "$startDate",
       |    "endDate":"$endDate"
       |  },
       |  "endOfPeriodStatementFinalised": $finalised
       |}
     """.stripMargin
  )

}
