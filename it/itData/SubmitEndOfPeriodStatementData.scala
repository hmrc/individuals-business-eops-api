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

package itData

import play.api.libs.json.{JsValue, Json}
import v1.models.downstream.TypeOfBusiness
import v1.models.downstream.TypeOfBusiness.`foreign-property`
import v1.models.request.{AccountingPeriod, SubmitEndOfPeriod}

object SubmitEndOfPeriodStatementData {

  val incomeSourceType : TypeOfBusiness = `foreign-property`
  val accountingPeriodStartDate = "2021-04-06"
  val accountingPeriodEndDate = "2022-04-05"
  val incomeSourceId = "XAIS12345678910"

  val validRequest: SubmitEndOfPeriod = SubmitEndOfPeriod(`foreign-property`, "XAIS12345678910", accountingPeriod = AccountingPeriod(
    "2021-04-06","2022-04-05"
  ),finalised = true)

  def fullValidJson(typeOfBusiness: String = "self-employment",
                    businessId: String = "XAIS12345678910",
                    startDate: String = "2021-04-06",
                    endDate: String = "2022-04-05",
                    finalised: String = "true"

                   ): JsValue = Json.parse(
    s"""{
       | "typeOfBusiness":"$typeOfBusiness",
       | "businessId":"$businessId",
       | "accountingPeriod": {
       |   "startDate": "$startDate",
       |   "endDate":"$endDate"
       | },
       | "finalised": $finalised
       |}
       |""".stripMargin
  )
}
