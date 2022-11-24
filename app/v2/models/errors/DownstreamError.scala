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

package v2.models.errors

import play.api.libs.json.{ JsPath, Json, Reads }

sealed trait DownstreamError

object DownstreamError {
  // Note that we only deserialize for standard and BVR error formats; OutboundError is created programmatically as required
  implicit def reads: Reads[DownstreamError] =
    implicitly[Reads[DownstreamStandardError]].map[DownstreamError](identity) orElse
      implicitly[Reads[DownstreamBvrError]].map[DownstreamError](identity)
}

case class DownstreamStandardError(failures: List[DownstreamErrorCode]) extends DownstreamError

object DownstreamStandardError {
  implicit val reads: Reads[DownstreamStandardError] = Json.reads

  def apply(failures: DownstreamErrorCode*): DownstreamStandardError = DownstreamStandardError(failures.toList)
}

case class DownstreamErrorCode(code: String)

object DownstreamErrorCode {
  implicit val reads: Reads[DownstreamErrorCode] = Json.reads[DownstreamErrorCode]
}

case class DownstreamBvrError(
    code: String,
    validationRuleFailures: List[DownstreamValidationRuleFailure]
) extends DownstreamError

object DownstreamBvrError {
  implicit val reads: Reads[DownstreamBvrError] =
    (JsPath \ "bvrfailureResponseElement").read(Json.reads[DownstreamBvrError])
}

case class DownstreamValidationRuleFailure(id: String, text: String, `type`: String = "ERR")

object DownstreamValidationRuleFailure {
  implicit val reads: Reads[DownstreamValidationRuleFailure] = Json.reads[DownstreamValidationRuleFailure]
}

case class OutboundError(error: MtdError, errors: Option[Seq[MtdError]] = None) extends DownstreamError
