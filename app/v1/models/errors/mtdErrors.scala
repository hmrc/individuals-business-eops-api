/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.models.errors

import play.api.libs.json.{ Json, OWrites }

case class MtdError(code: String, message: String, paths: Option[Seq[String]] = None)

object MtdError {
  implicit val writes: OWrites[MtdError] = Json.writes[MtdError]

  implicit def genericWrites[T <: MtdError]: OWrites[T] =
    writes.contramap[T](c => c: MtdError)
}

object RuleTaxYearRangeInvalid extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required")
object TypeOfBusinessFormatError extends MtdError("FORMAT_TYPE_OF_BUSINESS","The provided type of business is invalid")
object BusinessIdFormatError extends MtdError("FORMAT_BUSINESS_ID","The provided Business ID is invalid")
object StartDateFormatError extends MtdError("FORMAT_START_DATE","The provided Start date is invalid")
object EndDateFormatError extends MtdError("FORMAT_END_DATE","The provided End date is invalid")
object FinalisedFormatError extends MtdError("FORMAT_FINALISED","The provided Finalised value is invalid")
object RangeEndDateBeforeStartDateError extends MtdError("RANGE_END_DATE_BEFORE_START_DATE","The End date must be after the Start date")

object NinoFormatError extends MtdError(
  code = "FORMAT_NINO",
  message = "The provided NINO is invalid"
)

object TaxYearFormatError extends MtdError(
  code = "FORMAT_TAX_YEAR",
  message = "The provided tax year is invalid"
)

// Rule Errors
object RuleTaxYearNotSupportedError extends MtdError( "RULE_TAX_YEAR_NOT_SUPPORTED",
  "Tax year not supported, because it precedes the earliest allowable tax year")

object RuleIncorrectOrEmptyBodyError extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")
object RuleNotFinalisedError extends MtdError("RULE_NOT_FINALISED","Finalised must be set to true")
object RuleAlreadySubmittedError extends MtdError("RULE_ALREADY_SUBMITTED","An End of Period Statement already exists for this business' accounting period.")
object RuleEarlySubmissionError extends MtdError("RULE_EARLY_SUBMISSION",
  "An End Of Period Statement cannot be submitted before the end of the accounting period.")
object RuleLateSubmissionError extends MtdError("RULE_LATE_SUBMISSION", "The period to finalise has passed")
object RuleNonMatchingPeriodError extends MtdError("RULE_NON_MATCHING_PERIOD",
  "An End of Period Statement without a matching accounting period cannot be submitted")
object RuleConsolidatedExpensesError extends MtdError("RULE_CONSOLIDATED_EXPENSES","Consolidated expenses not allowed, threshold exceeded")
object RuleMismatchedStartDateError extends MtdError("RULE_MISMATCHED_START_DATE",
  "The period submission start date must match the accounting period start date")
object RuleMismatchedEndDateError extends MtdError("RULE_MISMATCHED_END_DATE","The period submission end date must match the accounting period end date")
object RuleClass4Over16Error extends MtdError("RULE_CLASS4_OVER_16",
  "Class 4 exemption is not allowed because the individual’s age is greater than or equal to 16 years old on the 6th April of the current tax year")
object RuleClass4PensionAge extends MtdError("RULE_CLASS4_PENSION_AGE",
  "Class 4 exemption is not allowed because the individual’s age is less than their State Pension age on the 6th April of the current tax year")
object RuleFHLPrivateUseAdjustment extends MtdError("RULE_FHL_PRIVATE_USE_ADJUSTMENT",
  "For UK Furnished Holiday Lettings, the private use adjustment must not exceed the total allowable expenses")
object RuleNonFHLPrivateUseAdjustment extends MtdError("RULE_NON_FHL_PRIVATE_USE_ADJUSTMENT",
  "For UK non-Furnished Holiday Lettings, the private use adjustment must not exceed the total allowable expenses")
//Standard Errors
object NotFoundError extends MtdError(
  code = "MATCHING_RESOURCE_NOT_FOUND",
  message = "Matching resource not found"
)

object DownstreamError extends MtdError(
  code = "INTERNAL_SERVER_ERROR",
  message = "An internal server error occurred"
)

object BadRequestError extends MtdError(
  code = "INVALID_REQUEST",
  message = "Invalid request"
)

object BVRError extends MtdError(
  code = "BUSINESS_ERROR",
  message = "Business validation error"
)

object ServiceUnavailableError extends MtdError(
  code = "SERVICE_UNAVAILABLE",
  message = "Internal server error"
)

//Authorisation Errors
object UnauthorisedError extends MtdError(
  code = "CLIENT_OR_AGENT_NOT_AUTHORISED",
  message = "The client and/or agent is not authorised"
)

object InvalidBearerTokenError extends MtdError(
  code = "UNAUTHORIZED",
  message = "Bearer token is missing or not authorized"
)

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError(
  code = "ACCEPT_HEADER_INVALID",
  message = "The accept header is missing or invalid"
)

object UnsupportedVersionError extends MtdError(
  code = "NOT_FOUND",
  message = "The requested resource could not be found"
)

object InvalidBodyTypeError extends MtdError(
  code = "INVALID_BODY_TYPE",
  message = "Expecting text/json or application/json body"
)