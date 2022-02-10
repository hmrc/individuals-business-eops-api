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

package v1r7.controllers.requestParsers.validators.validations

import play.api.Logger
import play.api.libs.json.Json
import v1r7.models.downstream.TypeOfBusiness
import v1r7.models.errors.{MtdError, TypeOfBusinessFormatError}

object TypeOfBusinessValidation {

  lazy val logger: Logger = Logger(this.getClass)
  lazy val log = s"[JsonFormatValidation][validate] - Request body failed validation with errors -"

  def typeOfBusinessFormat(typeOfBusiness: String): List[MtdError] = {
    val validTypeOfBusiness: Boolean = {
      Json.parse(s""""$typeOfBusiness"""").asOpt[TypeOfBusiness].isDefined
    }

    //400 FORMAT_TYPE_OF_BUSINESS The provided Type of business is invalid
    if(validTypeOfBusiness){ NoValidationErrors } else {
      logger.warn(s"$log typeOfBusiness is invalid. typeOfBusiness: $typeOfBusiness")
      List(TypeOfBusinessFormatError)
    }
  }
}