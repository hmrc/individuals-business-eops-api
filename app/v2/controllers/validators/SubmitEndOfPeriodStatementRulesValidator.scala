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

import api.controllers.validators.RulesValidator
import api.controllers.validators.resolvers.{ResolveBusinessId, ResolveDateRange, ResolveTypeOfBusiness}
import api.models.errors.{FinalisedFormatError, MtdError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import v2.models.request.SubmitEndOfPeriodStatementRequestData

object SubmitEndOfPeriodStatementRulesValidator extends RulesValidator[SubmitEndOfPeriodStatementRequestData] {

  def validateBusinessRules(parsed: SubmitEndOfPeriodStatementRequestData): Validated[Seq[MtdError], SubmitEndOfPeriodStatementRequestData] = {
    import parsed.body._

    val validatedTypeOfBusiness = ResolveTypeOfBusiness(typeOfBusiness.toString)

    val validatedBusinessId = ResolveBusinessId(businessId)

    val validatedAccountingPeriod = ResolveDateRange(accountingPeriod.startDate -> accountingPeriod.endDate)

    val validatedFinalised = validateFinalised(finalised)

    combine(
      validatedTypeOfBusiness,
      validatedBusinessId,
      validatedAccountingPeriod,
      validatedFinalised
    ).onSuccess(parsed)
  }

  private def validateFinalised(finalised: Boolean): Validated[Seq[MtdError], Unit] = {
    if (finalised) {
      Valid(())
    } else {
      Invalid(List(FinalisedFormatError))
    }
  }

}
