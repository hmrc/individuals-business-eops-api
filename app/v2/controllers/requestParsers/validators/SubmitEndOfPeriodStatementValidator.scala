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

package v2.controllers.requestParsers.validators

import api.controllers.requestParsers.validators.Validator
import v2.controllers.requestParsers.validators.validations._
import v2.models.errors._
import v2.models.request.{ SubmitEndOfPeriod, SubmitEndOfPeriodStatementRawData }

import javax.inject.Singleton

@Singleton
class SubmitEndOfPeriodStatementValidator extends Validator[SubmitEndOfPeriodStatementRawData] {

  private val validationSet = List(parameterFormatValidation, enumValidator, bodyFormatValidator, bodyFieldFormatValidation)

  private def parameterFormatValidation: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    List(NinoValidation.validate(data.nino))
  }

  private def enumValidator: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[String](data.body.json \ "typeOfBusiness")(TypeOfBusinessValidation.validate)
    )
  }

  private def bodyFieldFormatValidation: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    val body = data.body.json.as[SubmitEndOfPeriod]

    List(
      BusinessIdValidation.validateBusinessId(body.businessId),
      DateValidation.validateDates(body.accountingPeriod.startDate, body.accountingPeriod.endDate),
      FinalisedValidation.validateFinalised(body.finalised)
    )
  }

  private def bodyFormatValidator: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    JsonFormatValidation.validate[SubmitEndOfPeriod](data.body.json) match {
      case Nil          => NoValidationErrors
      case schemaErrors => List(schemaErrors)
    }
  }

  override def validate(data: SubmitEndOfPeriodStatementRawData): List[MtdError] = run(validationSet, data)
}
