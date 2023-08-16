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

package v3.connectors

import api.connectors.DownstreamUri.{IfsUri, TaxYearSpecificIfsUri}
import api.connectors.httpparsers.StandardDownstreamHttpParser
import api.connectors.httpparsers.StandardDownstreamHttpParser.SuccessCode
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import api.models.downstream.TypeOfBusiness
import config.AppConfig
import play.api.http.Status.{ACCEPTED, NO_CONTENT}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import v3.models.request.SubmitEndOfPeriodStatementRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitEndOfPeriodStatementConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def submitPeriodStatement(request: SubmitEndOfPeriodStatementRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[Unit]] = {
    import request._

    val incomeSourceType          = body.typeOfBusiness
    val accountingPeriodStartDate = body.accountingPeriod.startDate
    val accountingPeriodEndDate   = body.accountingPeriod.endDate
    val incomeSourceId            = body.businessId

    if (taxYear.useTaxYearSpecificApi) {
      implicit val httpReads: HttpReads[DownstreamOutcome[Unit]] =
        StandardDownstreamHttpParser.readsEmpty(successCode = SuccessCode(ACCEPTED))

      postEmpty(
        uri = TaxYearSpecificIfsUri[Unit](
          s"income-tax/income-sources/${taxYear.asTysDownstream}/" +
            s"$nino/$incomeSourceId/${TypeOfBusiness.toTys(incomeSourceType)}/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration")
      )
    } else {
      implicit val httpReads: HttpReads[DownstreamOutcome[Unit]] =
        StandardDownstreamHttpParser.readsEmpty(successCode = SuccessCode(NO_CONTENT))

      post(
        body = JsObject.empty,
        uri = IfsUri[Unit](
          s"income-tax/income-sources/nino/" +
            s"$nino/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId")
      )
    }

  }

}
