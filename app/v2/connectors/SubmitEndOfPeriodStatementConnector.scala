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

package v2.connectors

import config.AppConfig

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v2.connectors.DownstreamUri.IfsUri
import v2.models.downstream.EmptyJsonBody
import v2.models.request.SubmitEndOfPeriodStatementRequest
import v2.connectors.httpparsers.StandardDownstreamHttpParser._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitEndOfPeriodStatementConnector @Inject()(val http: HttpClient,
                                                    val appConfig: AppConfig) extends BaseDownstreamConnector {

  def submitPeriodStatement(request: SubmitEndOfPeriodStatementRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationId: String): Future[DownstreamOutcome[Unit]] = {

    val nino                      = request.nino.nino
    val incomeSourceType          = request.submitEndOfPeriod.typeOfBusiness
    val accountingPeriodStartDate = request.submitEndOfPeriod.accountingPeriod.startDate
    val accountingPeriodEndDate   = request.submitEndOfPeriod.accountingPeriod.endDate
    val incomeSourceId            = request.submitEndOfPeriod.businessId

    val url = s"income-tax/income-sources/nino/" +
      s"$nino/$incomeSourceType/$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId"

    post(
      body = EmptyJsonBody,
      uri = IfsUri[Unit](url)
    )
  }
}