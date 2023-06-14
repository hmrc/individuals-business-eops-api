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

package v2.controllers.validators

import api.controllers.validators.Validator
import api.controllers.validators.Validator._
import api.controllers.validators.validations._
import api.models.domain.Nino
import api.models.errors.MtdError
import api.models.request.NinoAndJsonBodyRawData
import v2.models.request.{ SubmitEndOfPeriod, SubmitEndOfPeriodStatementRequest }

import javax.inject.Singleton

@Singleton
class SubmitEndOfPeriodStatementValidator extends Validator[NinoAndJsonBodyRawData, SubmitEndOfPeriodStatementRequest] {

  protected val preParserValidations: PreParseValidationCallers[NinoAndJsonBodyRawData] =
    List(
      data => NinoValidation(data.nino),
      enumValidator
    )

  protected val parserValidation: ParserValidationCaller[NinoAndJsonBodyRawData, SubmitEndOfPeriodStatementRequest] =
    bodyFormatValidator

  protected val postParserValidations =
    List(
      bodyFieldFormatValidation
    )

  private def enumValidator: PreParseValidationCaller[NinoAndJsonBodyRawData] = { data =>
    JsonFormatValidation.validate[String](data.body.json \ "typeOfBusiness")(TypeOfBusinessValidation(_))
  }

  private def bodyFormatValidator: NinoAndJsonBodyRawData => Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] = { data =>
    JsonFormatValidation.validate[SubmitEndOfPeriod](data.body.json).map { parsed =>
      SubmitEndOfPeriodStatementRequest(Nino(data.nino), parsed)
    }
  }

  private def bodyFieldFormatValidation: PostParseValidationCaller[SubmitEndOfPeriodStatementRequest] = { parsed =>
    import parsed.submitEndOfPeriod._

    BusinessIdValidation(businessId) ++
      DateValidation(accountingPeriod.startDate, accountingPeriod.endDate) ++
      FinalisedValidation(finalised)
  }
}
