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

import api.models.errors.{ EndDateFormatError, MtdError, RuleEndDateBeforeStartDateError, StartDateFormatError }

import java.time.LocalDate

object DateValidation extends Validation {

  def apply(startDate: String, endDate: String): Seq[MtdError] = {

    val startLocalDate: Option[LocalDate] = try {
      Some(LocalDate.parse(startDate))
    } catch {
      case _: Exception => None
    }

    val endLocalDate: Option[LocalDate] = try {
      Some(LocalDate.parse(endDate))
    } catch {
      case _: Exception => None
    }

    (startLocalDate, endLocalDate) match {
      case (Some(startDate), Some(endDate)) =>
        if (endDate.isBefore(startDate)) {
          List(RuleEndDateBeforeStartDateError)
        } else {
          NoValidationErrors
        }

      case _ => validateStartDate(startLocalDate) ++ validateEndDate(endLocalDate)
    }
  }

  private def validateStartDate(startDate: Option[LocalDate]): Seq[MtdError] = {
    if (startDate.isDefined) NoValidationErrors else List(StartDateFormatError)
  }

  private def validateEndDate(endDate: Option[LocalDate]): Seq[MtdError] = {
    if (endDate.isDefined) NoValidationErrors else List(EndDateFormatError)
  }
}
