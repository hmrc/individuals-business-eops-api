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

package auth

import api.models.domain.TaxYear
import api.services.DownstreamStub
import itData.SubmitEndOfPeriodStatementData.{incomeSourceId, incomeSourceType}
import play.api.http.Status.{ACCEPTED, NO_CONTENT}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}

class IndividualsBusinessEopsApiAuthSupportingAgentsAllowedISpec extends AuthSupportingAgentsAllowedISpec {

  val callingApiVersion = "3.0"

  val supportingAgentsAllowedEndpoint = "submit-end-of-period-statement"

  val accountingPeriodStartDate = "2023-04-06"
  val accountingPeriodEndDate   = "2024-04-05"
  val taxYear: TaxYear          = TaxYear.fromIso(accountingPeriodEndDate)

  val mtdUrl = s"/$nino"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.post(requestJson))

  val downstreamUri: String =
    s"/income-tax/income-sources/${taxYear.asTysDownstream}/" +
      s"$nino/$incomeSourceId/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration"


  val maybeDownstreamResponseJson: Option[JsValue] = None

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.POST

  override val expectedMtdSuccessStatus: Int = NO_CONTENT
  override val downstreamSuccessStatus: Int = ACCEPTED

  protected val requestJson: JsValue =
    fullValidJson(typeOfBusiness = "foreign-property", startDate = accountingPeriodStartDate, endDate = accountingPeriodEndDate)

  def fullValidJson(typeOfBusiness: String,
                    businessId: String = "XAIS12345678910",
                    startDate: String ,
                    endDate: String,
                    finalised: String = "true"): JsValue = Json.parse(
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
