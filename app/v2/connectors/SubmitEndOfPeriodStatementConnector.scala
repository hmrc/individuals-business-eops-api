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

package v2.connectors

import config.AppConfig
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v2.connectors.DownstreamUri.{IfsUri, TaxYearSpecificIfsUri}
import v2.connectors.httpparsers.StandardDownstreamHttpParser._
import v2.models.downstream.TypeOfBusiness
import v2.models.request.SubmitEndOfPeriodStatementRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitEndOfPeriodStatementConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def submitPeriodStatement(request: SubmitEndOfPeriodStatementRequest)(implicit hc: HeaderCarrier,
                                                                        ec: ExecutionContext,
                                                                        correlationId: String): Future[DownstreamOutcome[Unit]] = {

    import request._
    val nino                      = request.nino.nino
    val incomeSourceType          = submitEndOfPeriod.typeOfBusiness
    val accountingPeriodStartDate = submitEndOfPeriod.accountingPeriod.startDate
    val accountingPeriodEndDate   = submitEndOfPeriod.accountingPeriod.endDate
    val incomeSourceId            = submitEndOfPeriod.businessId

    if (taxYear.useTaxYearSpecificApi) {
      postEmpty(
        uri = TaxYearSpecificIfsUri[Unit](
          s"income-tax/income-sources/${taxYear.asTysDownstream}/" +
            s"$nino/$incomeSourceId/${TypeOfBusiness.toTys(incomeSourceType)}/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration")
      )
    } else {
      post(
        body = JsObject.empty,
        uri = IfsUri[Unit](
          s"income-tax/income-sources/nino/" +
            s"$nino/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId")
      )
    }


  }
}
