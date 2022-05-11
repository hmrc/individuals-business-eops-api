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

sealed trait IfsError

object IfsError {
  // Note that we only deserialize for standard and BVR error formats; OutboundError is created programmatically as required
  implicit def reads: Reads[IfsError] =
    implicitly[Reads[IfsStandardError]].map[IfsError](identity) orElse
      implicitly[Reads[IfsBvrError]].map[IfsError](identity)
}

case class IfsStandardError(
    failures: List[IfsErrorCode]
) extends IfsError

object IfsStandardError {
  implicit val reads: Reads[IfsStandardError] = Json.reads

  def apply(failures: IfsErrorCode*): IfsStandardError = IfsStandardError(failures.toList)
}

case class IfsErrorCode(code: String)

object IfsErrorCode {
  implicit val reads: Reads[IfsErrorCode] = Json.reads[IfsErrorCode]
}

case class IfsBvrError(
    code: String,
    validationRuleFailures: List[IfsValidationRuleFailure]
) extends IfsError

object IfsBvrError {
  implicit val reads: Reads[IfsBvrError] =
    (JsPath \ "bvrfailureResponseElement").read(Json.reads[IfsBvrError])
}

case class IfsValidationRuleFailure(id: String, text: String, `type`: String = "ERR")

object IfsValidationRuleFailure {
  implicit val reads: Reads[IfsValidationRuleFailure] = Json.reads[IfsValidationRuleFailure]
}

case class OutboundError(error: MtdError, errors: Option[Seq[MtdError]] = None) extends IfsError
