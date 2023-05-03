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

package api.models.errors

import play.api.http.Status._

object TypeOfBusinessFormatError       extends MtdError("FORMAT_TYPE_OF_BUSINESS", "The provided type of business is invalid", BAD_REQUEST)
object BusinessIdFormatError           extends MtdError("FORMAT_BUSINESS_ID", "The provided Business ID is invalid", BAD_REQUEST)
object StartDateFormatError            extends MtdError("FORMAT_START_DATE", "The provided Start date is invalid", BAD_REQUEST)
object EndDateFormatError              extends MtdError("FORMAT_END_DATE", "The provided End date is invalid", BAD_REQUEST)
object RuleEndDateBeforeStartDateError extends MtdError("RULE_END_DATE_BEFORE_START_DATE", "The End date must be after the Start date", BAD_REQUEST)
object NinoFormatError                 extends MtdError("FORMAT_NINO", "The provided NINO is invalid", BAD_REQUEST)
object TaxYearFormatError              extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid", BAD_REQUEST)
object FinalisedFormatError            extends MtdError("FORMAT_FINALISED", "Finalised must be set to `true`", BAD_REQUEST)

// Rule Errors
object RuleTaxYearRangeInvalid
    extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required", BAD_REQUEST)

object RuleTaxYearNotSupportedError
    extends MtdError("RULE_TAX_YEAR_NOT_SUPPORTED", "The tax year specified does not lie within the supported range", BAD_REQUEST)

object RuleIncorrectOrEmptyBodyError
    extends MtdError("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted", BAD_REQUEST)

object RuleAlreadySubmittedError
    extends MtdError("RULE_ALREADY_SUBMITTED", "An End of Period Statement already exists for this business' accounting period", BAD_REQUEST)

object RuleEarlySubmissionError
    extends MtdError("RULE_EARLY_SUBMISSION", "An End Of Period Statement cannot be submitted before the end of the accounting period", BAD_REQUEST)

object RuleLateSubmissionError extends MtdError("RULE_LATE_SUBMISSION", "The period to finalise has passed", BAD_REQUEST)

object RuleNonMatchingPeriodError
    extends MtdError("RULE_NON_MATCHING_PERIOD", "An End of Period Statement without a matching accounting period cannot be submitted", BAD_REQUEST)

object RuleBusinessValidationFailure {
  val code = "RULE_BUSINESS_VALIDATION_FAILURE"

  def apply(message: String, errorId: String): MtdError =
    MtdError(code = code, message = message, errorId = Some(errorId), httpStatus = BAD_REQUEST)
}

object RuleBusinessValidationFailureTys
    extends MtdError("RULE_BUSINESS_VALIDATION_FAILURE", "There are business validation rule failures.", BAD_REQUEST)

//Standard Errors
object NotFoundError           extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found", NOT_FOUND)
object InternalError           extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred", INTERNAL_SERVER_ERROR)
object BadRequestError         extends MtdError("INVALID_REQUEST", "Invalid request", BAD_REQUEST)
object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error", INTERNAL_SERVER_ERROR)
object InvalidHttpMethodError  extends MtdError("INVALID_HTTP_METHOD", "Invalid HTTP method", METHOD_NOT_ALLOWED)

//Authorisation Errors
object ClientNotAuthenticatedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised", UNAUTHORIZED)

// Authentication OK but not allowed access to the requested resource
object ClientNotAuthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised", FORBIDDEN)
object InvalidBearerTokenError  extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized", UNAUTHORIZED)

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid", NOT_ACCEPTABLE)
object UnsupportedVersionError  extends MtdError("NOT_FOUND", "The requested resource could not be found", NOT_FOUND)
object InvalidBodyTypeError     extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body", UNSUPPORTED_MEDIA_TYPE)
