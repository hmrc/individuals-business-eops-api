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

package api.connectors.httpparsers

import api.models.errors.{DownstreamBvrError, DownstreamError, InternalError, OutboundError}
import play.api.libs.json._
import uk.gov.hmrc.http.HttpResponse
import utils.Logging

import scala.util.{Success, Try}

trait HttpParser extends Logging {

  implicit class KnownJsonResponse(response: HttpResponse) {

    def validateJson[T](implicit reads: Reads[T]): Option[T] = {
      Try(response.json) match {
        case Success(json: JsValue) => parseResult(json)
        case _ =>
          logger.warn("[KnownJsonResponse][validateJson] No JSON was returned")
          None
      }
    }

    private def parseResult[T](json: JsValue)(implicit reads: Reads[T]): Option[T] = json.validate[T] match {

      case JsSuccess(value, _) => Some(value)
      case JsError(error) =>
        logger.warn(s"[KnownJsonResponse][validateJson] Unable to parse JSON: $error")
        None
    }

  }

  def retrieveCorrelationId(response: HttpResponse): String = response.header("CorrelationId").getOrElse("")

  def parseErrors(response: HttpResponse): DownstreamError = {
    lazy val unableToParseJsonError: DownstreamError = {
      logger.warn(s"unable to parse errors from response: ${response.body}")
      OutboundError(InternalError)
    }

    response
      .validateJson[DownstreamError]
      .flatMap {
        case e: DownstreamBvrError =>
          val filtered = e.validationRuleFailures.filter(_.`type` == "ERR")
          if (filtered.isEmpty) {
            None
          } else {
            Some(e.copy(validationRuleFailures = filtered))
          }

        case e: DownstreamError => Some(e)
      }
      .getOrElse(unableToParseJsonError)
  }

}
