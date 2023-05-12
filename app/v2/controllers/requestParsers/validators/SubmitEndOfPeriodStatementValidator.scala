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
import api.controllers.requestParsers.validators.validations._
import api.models.errors.MtdError
import v2.models.request.{ SubmitEndOfPeriod, SubmitEndOfPeriodStatementRawData }

import javax.inject.Singleton

@Singleton
class SubmitEndOfPeriodStatementValidator extends Validator[SubmitEndOfPeriodStatementRawData] {

  override def validations: Seq[SubmitEndOfPeriodStatementRawData => Seq[MtdError]] =
    List(parameterFormatValidation, enumValidation, bodyFormatValidation, bodyFieldFormatValidation)

  private def parameterFormatValidation: SubmitEndOfPeriodStatementRawData => Seq[MtdError] = { data =>
    NinoValidation.validate(data.nino)
  }

  private def enumValidation: SubmitEndOfPeriodStatementRawData => Seq[MtdError] = { data =>
    JsonFormatValidation.validate[String](data.body.json \ "typeOfBusiness")(TypeOfBusinessValidation.validate)
  }

  private def bodyFieldFormatValidation: SubmitEndOfPeriodStatementRawData => Seq[MtdError] = { data =>
    val body = data.body.json.as[SubmitEndOfPeriod]

    BusinessIdValidation.validateBusinessId(body.businessId) ++
      DateValidation.validateDates(body.accountingPeriod.startDate, body.accountingPeriod.endDate) ++
      FinalisedValidation.validateFinalised(body.finalised)
  }

  private def bodyFormatValidation: SubmitEndOfPeriodStatementRawData => Seq[MtdError] = { data =>
    JsonFormatValidation.validate[SubmitEndOfPeriod](data.body.json) match {
      case Nil          => NoValidationErrors
      case schemaErrors => schemaErrors
    }
  }

}
