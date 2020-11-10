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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.connectors

import config.AppConfig
import play.api.Logger
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}

import scala.concurrent.{ExecutionContext, Future}

trait DesConnector {
  val http: HttpClient
  val appConfig: AppConfig

  val logger:Logger = Logger(this.getClass)

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier, correlationId: String): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
      .withExtraHeaders("Environment" -> appConfig.desEnv, "CorrelationId" -> correlationId)

  def postEmpty[Resp]( uri: DesUri[Resp])(implicit ec: ExecutionContext,
                                                              hc: HeaderCarrier,
                                                              correlationId: String,
                                                              httpReads: HttpReads[DesOutcome[Resp]]): Future[DesOutcome[Resp]] = {

    def doPostEmpty(implicit hc: HeaderCarrier): Future[DesOutcome[Resp]] = {
      http.POSTEmpty(s"${appConfig.desBaseUrl}/${uri.value}")
    }

    doPostEmpty(desHeaderCarrier(hc, correlationId))
  }

}
