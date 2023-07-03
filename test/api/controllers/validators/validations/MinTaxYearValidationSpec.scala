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

import api.controllers.validators.validations.MinTaxYearValidation
import api.models.errors.{ RuleTaxYearNotSupportedError, TaxYearFormatError }
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class MinTaxYearValidationSpec extends UnitSpec with JsonErrorValidators {

  // WLOG
  val minTaxYear = 2020

  "validate" should {
    "return no errors" when {
      "a tax year greater than minimum is supplied" in {
        val validationResult = MinTaxYearValidation("2020-21", minTaxYear)
        validationResult shouldBe empty
      }

      "a tax year equal to minimum is supplied" in {
        val validationResult = MinTaxYearValidation("2019-20", minTaxYear)
        validationResult shouldBe empty
      }
    }

    "return the given error" when {
      "a tax year is below the minimum is supplied" in {
        val validationResult = MinTaxYearValidation("2018-19", minTaxYear)
        validationResult shouldBe List(RuleTaxYearNotSupportedError)
      }
      "a tax year in the wrong format is supplied" in {
        val validationResult = MinTaxYearValidation("BEAN-OH", minTaxYear)
        validationResult shouldBe List(TaxYearFormatError)
      }
    }
  }
}