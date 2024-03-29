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

package v3.models.requestData

import support.UnitSpec
import v3.models.request.DownstreamTaxYear

import java.time.LocalDate

class DownstreamTaxYearSpec extends UnitSpec {

  val taxYear           = "2018-19"
  val downstreamTaxYear = "2019"

  "DownstreamTaxYear" should {
    "generate a downstream tax year" when {
      "given a year" in {
        val year = DownstreamTaxYear.toYearYYYY(taxYear)
        year.value shouldBe downstreamTaxYear
      }
    }
    "generate an mtd tax year" when {
      "given a year" in {
        val year = DownstreamTaxYear.toMTDYear(downstreamTaxYear)
        year.value shouldBe taxYear
      }
    }
    "generate a tax year as a string" when {
      "given a string" in {
        val year = DownstreamTaxYear("2018-19")
        year.toString shouldBe taxYear
      }
    }
    "generate the most recent tax year" when {
      "the date is before XXXX-04-05" in {
        DownstreamTaxYear.mostRecentTaxYear(LocalDate.parse("2020-04-01")) shouldBe DownstreamTaxYear("2019")
      }
      "the date is after XXXX-04-05" in {
        DownstreamTaxYear.mostRecentTaxYear(LocalDate.parse("2020-04-13")) shouldBe DownstreamTaxYear("2020")
      }
    }
  }

}
