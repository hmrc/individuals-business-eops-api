/*
 * Copyright 2021 HM Revenue & Customs
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

import support.UnitSpec
import v1.models.errors.TypeOfBusinessFormatError

class TypeOfBusinessValidationSpec extends UnitSpec {

  "validate" should {

    "return no errors" when {

      "provided with a string of 'self-employment'" in {
        TypeOfBusinessValidation.typeOfBusinessFormat("self-employment").isEmpty shouldBe true
      }

      "provided with a string of 'uk-property'" in {
        TypeOfBusinessValidation.typeOfBusinessFormat("uk-property").isEmpty shouldBe true
      }

      "provided with a string of 'foreign-property'" in {
        TypeOfBusinessValidation.typeOfBusinessFormat("foreign-property").isEmpty shouldBe true
      }
    }

    "return an error" when {

      "provided with an empty string" in {
        TypeOfBusinessValidation.typeOfBusinessFormat("") shouldBe List(TypeOfBusinessFormatError)
      }

      "provided with a non-matching string" in {
        TypeOfBusinessValidation.typeOfBusinessFormat("self-employment-a") shouldBe List(TypeOfBusinessFormatError)
      }
    }
  }

}
