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

package api.controllers.validators.resolvers

import api.models.errors.StartDateFormatError
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec

import java.time.LocalDate

class ResolveStartDateSpec extends UnitSpec {

  val minYear          = 1900
  val resolveStartDate = new ResolveStartDate(minYear)

  "ResolveStartDateRange" should {
    "return no errors" when {
      "passed a start date of 1900" in {
        val inputDate = "1900-01-21"
        val expected  = Valid(LocalDate.parse(inputDate))
        val result    = resolveStartDate(inputDate)
        result shouldBe expected
      }

      "passed a start date after 1900" in {
        val inputDate = "2021-01-21"
        val expected  = Valid(LocalDate.parse(inputDate))
        val result    = resolveStartDate(inputDate)
        result shouldBe expected
      }
    }

    "return errors" when {

      "passed a start date that is earlier than 1900" in {
        val inputDate = "1899-06-21"
        val result    = resolveStartDate(inputDate)
        result shouldBe Invalid(List(StartDateFormatError))
      }
    }
  }

}
