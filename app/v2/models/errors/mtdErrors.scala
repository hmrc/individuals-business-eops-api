/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{ Json, OWrites }

case class MtdError(code: String, message: String, paths: Option[Seq[String]] = None, errorId: Option[String] = None)

object MtdErrorWithCode {
  def unapply(arg: MtdError): Option[String] = Some(arg.code)
}

object MtdError {
  implicit val writes: OWrites[MtdError] = Json.writes[MtdError]

  implicit def genericWrites[T <: MtdError]: OWrites[T] =
    writes.contramap[T](c => c: MtdError)
}

object RuleTaxYearRangeInvalid
    extends MtdError(
      code = "RULE_TAX_YEAR_RANGE_INVALID",
      message = "Tax year range invalid. A tax year range of one year is required"
    )

object TypeOfBusinessFormatError
    extends MtdError(
      code = "FORMAT_TYPE_OF_BUSINESS",
      message = "The provided type of business is invalid"
    )

object BusinessIdFormatError
    extends MtdError(
      code = "FORMAT_BUSINESS_ID",
      message = "The provided Business ID is invalid"
    )

object StartDateFormatError
    extends MtdError(
      code = "FORMAT_START_DATE",
      message = "The provided Start date is invalid"
    )

object EndDateFormatError
    extends MtdError(
      code = "FORMAT_END_DATE",
      message = "The provided End date is invalid"
    )

object FinalisedFormatError
    extends MtdError(
      code = "FORMAT_FINALISED",
      message = "Finalised must be set to `true`"
    )

object RuleEndDateBeforeStartDateError
    extends MtdError(
      code = "RULE_END_DATE_BEFORE_START_DATE",
      message = "The End date must be after the Start date"
    )

object NinoFormatError
    extends MtdError(
      code = "FORMAT_NINO",
      message = "The provided NINO is invalid"
    )

object TaxYearFormatError
    extends MtdError(
      code = "FORMAT_TAX_YEAR",
      message = "The provided tax year is invalid"
    )

// Rule Errors
object RuleTaxYearNotSupportedError
    extends MtdError(
      code = "RULE_TAX_YEAR_NOT_SUPPORTED",
      message = "The tax year specified does not lie within the supported range"
    )

object RuleIncorrectOrEmptyBodyError
    extends MtdError(
      code = "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
      message = "An empty or non-matching body was submitted"
    )

object RuleAlreadySubmittedError
    extends MtdError(
      code = "RULE_ALREADY_SUBMITTED",
      message = "An End of Period Statement already exists for this business' accounting period"
    )

object RuleEarlySubmissionError
    extends MtdError(
      code = "RULE_EARLY_SUBMISSION",
      message = "An End Of Period Statement cannot be submitted before the end of the accounting period"
    )

object RuleLateSubmissionError
    extends MtdError(
      code = "RULE_LATE_SUBMISSION",
      message = "The period to finalise has passed"
    )

object RuleNonMatchingPeriodError
    extends MtdError(
      code = "RULE_NON_MATCHING_PERIOD",
      message = "An End of Period Statement without a matching accounting period cannot be submitted"
    )

object RuleBusinessValidationFailure {
  val code = "RULE_BUSINESS_VALIDATION_FAILURE"

  def apply(message: String, errorId: String): MtdError =
    MtdError(code = code, message = message, errorId = Some(errorId))
}

object RuleBusinessValidationFailureTys
    extends MtdError(
      code = "RULE_BUSINESS_VALIDATION_FAILURE",
      message = "There are business validation rule failures."
    )

//Standard Errors
object NotFoundError
    extends MtdError(
      code = "MATCHING_RESOURCE_NOT_FOUND",
      message = "Matching resource not found"
    )

object InternalError
    extends MtdError(
      code = "INTERNAL_SERVER_ERROR",
      message = "An internal server error occurred"
    )

object BadRequestError
    extends MtdError(
      code = "INVALID_REQUEST",
      message = "Invalid request"
    )

object ServiceUnavailableError
    extends MtdError(
      code = "SERVICE_UNAVAILABLE",
      message = "Internal server error"
    )

//Authorisation Errors
object UnauthorisedError
    extends MtdError(
      code = "CLIENT_OR_AGENT_NOT_AUTHORISED",
      message = "The client and/or agent is not authorised"
    )

object InvalidBearerTokenError
    extends MtdError(
      code = "UNAUTHORIZED",
      message = "Bearer token is missing or not authorized"
    )

// Accept header Errors
object InvalidAcceptHeaderError
    extends MtdError(
      code = "ACCEPT_HEADER_INVALID",
      message = "The accept header is missing or invalid"
    )

object UnsupportedVersionError
    extends MtdError(
      code = "NOT_FOUND",
      message = "The requested resource could not be found"
    )

object InvalidBodyTypeError
    extends MtdError(
      code = "INVALID_BODY_TYPE",
      message = "Expecting text/json or application/json body"
    )
