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

package v2.models.errors

import play.api.http.Status._

object FinalisedFormatError
    extends MtdError(
      code = "FORMAT_FINALISED",
      message = "Finalised must be set to `true`",
      httpStatus = BAD_REQUEST
    )

object RuleAlreadySubmittedError
    extends MtdError(
      code = "RULE_ALREADY_SUBMITTED",
      message = "An End of Period Statement already exists for this business' accounting period",
      httpStatus = BAD_REQUEST
    )

object RuleEarlySubmissionError
    extends MtdError(
      code = "RULE_EARLY_SUBMISSION",
      message = "An End Of Period Statement cannot be submitted before the end of the accounting period",
      httpStatus = BAD_REQUEST
    )

object RuleLateSubmissionError
    extends MtdError(
      code = "RULE_LATE_SUBMISSION",
      message = "The period to finalise has passed",
      httpStatus = BAD_REQUEST
    )

object RuleNonMatchingPeriodError
    extends MtdError(
      code = "RULE_NON_MATCHING_PERIOD",
      message = "An End of Period Statement without a matching accounting period cannot be submitted",
      httpStatus = BAD_REQUEST
    )

object RuleBusinessValidationFailure {
  val code = "RULE_BUSINESS_VALIDATION_FAILURE"

  def apply(message: String, errorId: String): MtdError =
    MtdError(code = code, message = message, errorId = Some(errorId), httpStatus = BAD_REQUEST)
}

object RuleBusinessValidationFailureTys
    extends MtdError(
      code = "RULE_BUSINESS_VALIDATION_FAILURE",
      message = "There are business validation rule failures.",
      httpStatus = BAD_REQUEST
    )
