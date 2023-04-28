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

object RuleTaxYearRangeInvalid
    extends MtdError(
      code = "RULE_TAX_YEAR_RANGE_INVALID",
      message = "Tax year range invalid. A tax year range of one year is required",
      httpStatus = BAD_REQUEST
    )

object TypeOfBusinessFormatError
    extends MtdError(
      code = "FORMAT_TYPE_OF_BUSINESS",
      message = "The provided type of business is invalid",
      httpStatus = BAD_REQUEST
    )

object BusinessIdFormatError
    extends MtdError(
      code = "FORMAT_BUSINESS_ID",
      message = "The provided Business ID is invalid",
      httpStatus = BAD_REQUEST
    )

object StartDateFormatError
    extends MtdError(
      code = "FORMAT_START_DATE",
      message = "The provided Start date is invalid",
      httpStatus = BAD_REQUEST
    )

object EndDateFormatError
    extends MtdError(
      code = "FORMAT_END_DATE",
      message = "The provided End date is invalid",
      httpStatus = BAD_REQUEST
    )

object RuleEndDateBeforeStartDateError
    extends MtdError(
      code = "RULE_END_DATE_BEFORE_START_DATE",
      message = "The End date must be after the Start date",
      httpStatus = BAD_REQUEST
    )

object NinoFormatError
    extends MtdError(
      code = "FORMAT_NINO",
      message = "The provided NINO is invalid",
      httpStatus = BAD_REQUEST
    )

object TaxYearFormatError
    extends MtdError(
      code = "FORMAT_TAX_YEAR",
      message = "The provided tax year is invalid",
      httpStatus = BAD_REQUEST
    )

// Rule Errors
object RuleTaxYearNotSupportedError
    extends MtdError(
      code = "RULE_TAX_YEAR_NOT_SUPPORTED",
      message = "The tax year specified does not lie within the supported range",
      httpStatus = BAD_REQUEST
    )

object RuleIncorrectOrEmptyBodyError
    extends MtdError(
      code = "RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED",
      message = "An empty or non-matching body was submitted",
      httpStatus = BAD_REQUEST
    )

//Standard Errors
object NotFoundError
    extends MtdError(
      code = "MATCHING_RESOURCE_NOT_FOUND",
      message = "Matching resource not found",
      httpStatus = NOT_FOUND
    )

object InternalError
    extends MtdError(
      code = "INTERNAL_SERVER_ERROR",
      message = "An internal server error occurred",
      httpStatus = INTERNAL_SERVER_ERROR
    )

object BadRequestError
    extends MtdError(
      code = "INVALID_REQUEST",
      message = "Invalid request",
      httpStatus = BAD_REQUEST
    )

object ServiceUnavailableError
    extends MtdError(
      code = "SERVICE_UNAVAILABLE",
      message = "Internal server error",
      httpStatus = INTERNAL_SERVER_ERROR
    )

object InvalidHttpMethodError
    extends MtdError(
      code = "INVALID_HTTP_METHOD",
      message = "Invalid HTTP method",
      httpStatus = METHOD_NOT_ALLOWED
    )

//Authorisation Errors
object ClientNotAuthenticatedError
    extends MtdError(
      code = "CLIENT_OR_AGENT_NOT_AUTHORISED",
      message = "The client and/or agent is not authorised",
      httpStatus = UNAUTHORIZED
    )

/** Authentication OK but not allowed access to the requested resource
  */
object ClientNotAuthorisedError
    extends MtdError(
      code = "CLIENT_OR_AGENT_NOT_AUTHORISED",
      message = "The client and/or agent is not authorised",
      httpStatus = FORBIDDEN
    )

object InvalidBearerTokenError
    extends MtdError(
      code = "UNAUTHORIZED",
      message = "Bearer token is missing or not authorized",
      httpStatus = UNAUTHORIZED
    )

// Accept header Errors
object InvalidAcceptHeaderError
    extends MtdError(
      code = "ACCEPT_HEADER_INVALID",
      message = "The accept header is missing or invalid",
      httpStatus = NOT_ACCEPTABLE
    )

object UnsupportedVersionError
    extends MtdError(
      code = "NOT_FOUND",
      message = "The requested resource could not be found",
      httpStatus = NOT_FOUND
    )

object InvalidBodyTypeError
    extends MtdError(
      code = "INVALID_BODY_TYPE",
      message = "Expecting text/json or application/json body",
      httpStatus = UNSUPPORTED_MEDIA_TYPE
    )
