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

package v3.controllers.validators

import api.models.domain.Nino
import api.models.errors._
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v3.data.SubmitEndOfPeriodStatementData._
import v3.models.request.{SubmitEndOfPeriodRequestBody, SubmitEndOfPeriodStatementRequestData}

class SubmitEndOfPeriodStatementValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino   = "AA123456A"
  private val invalidNino = "Darth Sidious"

  private val parsedNino = Nino(validNino)

  private val validSubmitEngOfPeriodJson = jsonRequestBody(
    typeOfBusiness = "foreign-property",
    startDate = validRequest.accountingPeriod.startDate,
    endDate = validRequest.accountingPeriod.endDate
  )

  private val parsedValidSubmitEndOfPeriodBody = validSubmitEngOfPeriodJson.as[SubmitEndOfPeriodRequestBody]

  val validatorFactory = new SubmitEndOfPeriodStatementValidatorFactory

  private def validator(nino: String, body: JsValue) = validatorFactory.validator(nino, body)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, validSubmitEngOfPeriodJson).validateAndWrapResult()

        result shouldBe Right(
          SubmitEndOfPeriodStatementRequestData(parsedNino, parsedValidSubmitEndOfPeriodBody)
        )
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(invalidNino, validSubmitEngOfPeriodJson).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }

      "an empty json object is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, Json.parse("""{}""")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }

      "mandatory fields are missing" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, Json.parse("""{ "finalised": true }""")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/accountingPeriod", "/businessId", "/typeOfBusiness"))))
        )
      }

      "an invalid finalised value is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, jsonRequestBody(finalised = "false")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, FinalisedFormatError)
        )
      }

      "an invalid typeOfBusiness is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, jsonRequestBody(typeOfBusiness = "Undercover")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, TypeOfBusinessFormatError)
        )
      }

      "an invalid businessId is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, jsonRequestBody(businessId = "invalid")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, BusinessIdFormatError)
        )
      }

      "an invalid start date is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, jsonRequestBody(startDate = "not a date")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, StartDateFormatError)
        )
      }

      "an invalid end date is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(validNino, jsonRequestBody(endDate = "not a date")).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, EndDateFormatError)
        )
      }

      "an invalid start date before end date is supplied" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(
            validNino,
            jsonRequestBody(
              startDate = "2020-10-10",
              endDate = "2020-10-09"
            )).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(correlationId, RuleEndDateBeforeStartDateError)
        )
      }
    }

    "return multiple errors" when {
      "multiple fields are invalid" in {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequestData] =
          validator(
            validNino,
            jsonRequestBody(
              typeOfBusiness = "uk-property",
              businessId = "XXXXXX",
              startDate = "XXXXXX",
              endDate = "XXXXXX",
              finalised = "false"
            )).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                BusinessIdFormatError,
                StartDateFormatError,
                EndDateFormatError,
                FinalisedFormatError
              ))
          ))
      }
    }
  }

}
