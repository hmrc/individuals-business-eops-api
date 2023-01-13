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

package v1.controllers.requestParsers.validators.validations

import java.time.LocalDate

import v1.models.errors.{EndDateFormatError, MtdError, RangeEndDateBeforeStartDateError, StartDateFormatError}

object DateValidation {

  def validateStartDate(startDate: Option[LocalDate]): List[MtdError] ={
    //400 FORMAT_START_DATE The provided Start date is invalid
    if(startDate.isDefined) NoValidationErrors else List(StartDateFormatError)
  }

  def validateEndDate(endDate: Option[LocalDate]): List[MtdError] ={
    //400 FORMAT_END_DATE The provided From date is invalid
    if(endDate.isDefined) NoValidationErrors else List(EndDateFormatError)
  }

  def validateDates(startDate: String, endDate: String): List[MtdError] ={

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

        if(endDate.isBefore(startDate)){
          //400 RANGE_END_DATE_BEFORE_START_DATE The End date must be after the Start date
          List(RangeEndDateBeforeStartDateError)
        } else {
          NoValidationErrors
        }

      case _ => validateStartDate(startLocalDate) ++ validateEndDate(endLocalDate)
    }
  }
}