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

package v1.controllers.requestParsers.validators.validations

import play.api.Logger
import play.api.libs.json.JsLookupResult
import v1.models.errors.{FinalisedFormatError, MtdError, RuleNotFinalisedError}

object FinalisedValidation {

  lazy val log = s"[JsonFormatValidation][validate] - Request body failed validation with errors -"
  lazy val logger: Logger = Logger(this.getClass)

  def validateFinalised(json: JsLookupResult): List[MtdError] ={
    (json.asOpt[Boolean], json.asOpt[String]) match {
      case (Some(bool), _) => validateFinalised(bool)
      case (_, Some(string)) => finalisedIncorrectFormat(string)
      case _ => NoValidationErrors
    }
  }

  def validateFinalised(finalised: Boolean): List[MtdError] ={
    //400 RULE_NOT_FINALISED Finalised must be set to "true"
    if(finalised) NoValidationErrors else List(RuleNotFinalisedError)
  }

  def finalisedIncorrectFormat(finalised: String): List[MtdError] = {
    logger.warn(s"$log finalised was not of type boolean. finalised: $finalised")
    //400 FORMAT_FINALISED The provided Finalised value is invalid
    List(FinalisedFormatError)
  }
}
