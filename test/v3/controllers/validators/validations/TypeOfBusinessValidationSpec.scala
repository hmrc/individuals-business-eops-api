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

package v3.controllers.validators.validations

import api.controllers.validators.validations.TypeOfBusinessValidation
import api.models.errors.TypeOfBusinessFormatError
import support.UnitSpec

class TypeOfBusinessValidationSpec extends UnitSpec {

  "validate" must {
    "return no errors" when {
      def checkGood(typeOfBusiness: String): Unit =
        s"type of business is $typeOfBusiness" in {
          val result = TypeOfBusinessValidation(typeOfBusiness)
          result shouldBe empty
        }

      Seq("self-employment", "uk-property", "foreign-property").foreach(checkGood)
    }

    "return a validation error" when {
      "provided with an empty string" in {
        val result = TypeOfBusinessValidation("")
        result shouldBe List(TypeOfBusinessFormatError)
      }

      "provided with a non-matching string" in {
        val result = TypeOfBusinessValidation("self-employment-a")
        result shouldBe List(TypeOfBusinessFormatError)
      }
    }
  }

}
