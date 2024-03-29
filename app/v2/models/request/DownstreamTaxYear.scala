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

package v2.models.request

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Represents a tax year for Downstream
  *
  * @param value
  *   the tax year string (2017-18)
  */
case class DownstreamTaxYear(value: String) extends AnyVal {
  override def toString: String = value
}

object DownstreamTaxYear {

  val startOfYear      = 2
  val startYearAndDash = 5

  // TODO MOVE TO VALIDATION ONLY
  def toYearYYYY(taxYear: String): DownstreamTaxYear = DownstreamTaxYear(taxYear.take(startOfYear) + taxYear.drop(startYearAndDash))

  /** Converts YYYY year to MTD year YYYY-YY. E.g. 2018 -> 2017-18
    *
    * @param taxYear
    *   the tax year string (2018)
    */
  def toMTDYear(taxYear: String): DownstreamTaxYear =
    DownstreamTaxYear(s"${taxYear.toInt - 1}-${taxYear.drop(startOfYear)}")

  // TODO UPDATE IF NEEDED TO USE 2017-18 FORMAT
  def mostRecentTaxYear(date: LocalDate = LocalDate.now()): DownstreamTaxYear = {
    val limit = LocalDate.parse(s"${date.getYear}-04-05", DateTimeFormatter.ISO_DATE)
    if (date.isBefore(limit)) {
      DownstreamTaxYear(s"${date.getYear - 1}")
    } else {
      DownstreamTaxYear(s"${date.getYear}")
    }
  }

}
