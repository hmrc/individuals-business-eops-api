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

package v1.controllers.requestParsers.validators

import api.controllers.requestParsers.validators.validations.NoValidationErrors

import javax.inject.Singleton
import play.api.libs.json.{ JsLookupResult, JsValue }
import v1.controllers.requestParsers.validators.validations.BusinessIdValidation._
import v1.controllers.requestParsers.validators.validations.FinalisedValidation._
import v1.controllers.requestParsers.validators.validations.TypeOfBusinessValidation._
import v1.controllers.requestParsers.validators.validations.DateValidation._
import v1.controllers.requestParsers.validators.validations._
import v1.models.errors._
import v1.models.request.{ AccountingPeriod, SubmitEndOfPeriod, SubmitEndOfPeriodStatementRawData }

@Singleton
class SubmitEndOfPeriodStatementValidator extends Validator[SubmitEndOfPeriodStatementRawData] {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator)

  private def parameterFormatValidation: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    (List(NinoValidation.validate(data.nino)))
  }

  def jsonValidation(json: JsValue): List[MtdError] = {

    val typeOfBusiness: Option[String] = (json \ "typeOfBusiness").asOpt[String]
    val businessId                     = (json \ "businessId").asOpt[String]
    val accountingPeriod               = (json \ "accountingPeriod").asOpt[AccountingPeriod]
    val finalised: JsLookupResult      = json \ "finalised"

    typeOfBusiness.map(typeOfBusinessFormat).getOrElse(NoValidationErrors) ++
      businessId.map(validateBusinessId).getOrElse(NoValidationErrors) ++
      accountingPeriod.map(period => validateDates(period.startDate, period.endDate)).getOrElse(NoValidationErrors) ++
      validateFinalised(finalised)
  }

  private def bodyFormatValidator: SubmitEndOfPeriodStatementRawData => List[List[MtdError]] = { data =>
    val jsonValidationErrors                     = jsonValidation(data.body.json)
    lazy val jsonModelValidation: List[MtdError] = JsonFormatValidation.validate[SubmitEndOfPeriod](data.body.json)

    val errors = List(
      if (jsonValidationErrors.nonEmpty) jsonValidationErrors else jsonModelValidation
    )

    List(Validator.flattenErrors(errors))
  }

  override def validate(data: SubmitEndOfPeriodStatementRawData): List[MtdError] = run(validationSet, data)
}
