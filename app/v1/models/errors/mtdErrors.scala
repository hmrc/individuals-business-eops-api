/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Json, OWrites}

case class MtdError(code: String, message: String, paths: Option[Seq[String]] = None)

object MtdError {
  implicit val writes: OWrites[MtdError] = Json.writes[MtdError]
}

object NinoFormatError extends MtdError("FORMAT_NINO", "The provided NINO is invalid")
object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object RuleTaxYearRangeInvalid extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required")

object TypeOfBusinessFormatError extends MtdError("FORMAT_TYPE_OF_BUSINESS","The provided type of business is invalid")
object BusinessIdFormatError extends MtdError("FORMAT_BUSINESS_ID","The provided Business ID is invalid")
object StartDateFormatError extends MtdError("FORMAT_START_DATE","The provided Start date is invalid")
object EndDateFormatError extends MtdError("FORMAT_END_DATE","The provided End date is invalid")
object FinalisedFormatError extends MtdError("FORMAT_FINALISED","The provided Finalised value is invalid")
object RangeEndDateBeforeStartDateError extends MtdError("RANGE_END_DATE_BEFORE_START_DATE","The End date must be after the Start date")

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
object NotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")

object DownstreamError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred")

object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request")

object BVRError extends MtdError("BUSINESS_ERROR", "Business validation error")

object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error")

//Authorisation Errors
object UnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised")
object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized")

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")

object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found")

object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body")
