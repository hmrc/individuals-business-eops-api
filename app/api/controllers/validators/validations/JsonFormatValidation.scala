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

package api.controllers.validators.validations

import api.models.errors.{ MtdError, RuleIncorrectOrEmptyBodyError }
import play.api.Logger
import play.api.libs.json._

object JsonFormatValidation extends Validation {

  def validate[T: Reads](jsLookupResult: JsLookupResult)(validation: T => Seq[MtdError]): Seq[MtdError] =
    jsLookupResult.validate[T] match {
      case JsSuccess(value, _) => validation(value)
      case _: JsError          => NoValidationErrors
    }

  def validate[A: OFormat](data: JsValue): Either[Seq[MtdError], A] =
    if (data == JsObject.empty) {
      Left(List(RuleIncorrectOrEmptyBodyError))
    } else {
      data.validate[A] match {
        case JsSuccess(body, _) =>
          if (Json.toJson(body) == JsObject.empty) {
            Left(List(RuleIncorrectOrEmptyBodyError))
          } else {
            Right(body)
          }
        case JsError(errors) => {
          val immutableErrors = errors.map { case (path, errors) => (path, errors.toList) }.toList
          Left(handleErrors(immutableErrors))
        }
      }
    }

  private def handleErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]): Seq[MtdError] = {
    val failures = errors.map {
      case (path: JsPath, Seq(JsonValidationError(Seq("error.path.missing"))))                              => MissingMandatoryField(path)
      case (path: JsPath, Seq(JsonValidationError(Seq(error: String)))) if error.contains("error.expected") => WrongFieldType(path)
      case (path: JsPath, _)                                                                                => OtherFailure(path)
    }

    val logString = failures
      .groupBy(_.getClass)
      .values
      .map(failure => s"${failure.head.failureReason}: " + s"${failure.map(_.fromJsPath)}")
      .toString()
      .dropRight(1)
      .drop(5)

    val logger: Logger = Logger(this.getClass)
    logger.warn(s"[JsonFormatValidation][validate] - Request body failed validation with errors - $logString")

    List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(failures.map(_.fromJsPath))))
  }

  private class JsonFormatValidationFailure(path: JsPath, failure: String) {
    val failureReason: String = this.failure

    def fromJsPath: String = this.path.toString().replace("(", "/").replace(")", "")
  }

  private case class MissingMandatoryField(path: JsPath) extends JsonFormatValidationFailure(path, "Missing mandatory field")
  private case class WrongFieldType(path: JsPath)        extends JsonFormatValidationFailure(path, "Wrong field type")
  private case class OtherFailure(path: JsPath)          extends JsonFormatValidationFailure(path, "Other failure")
}