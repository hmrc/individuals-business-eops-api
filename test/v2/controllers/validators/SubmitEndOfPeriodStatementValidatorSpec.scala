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

import api.models.domain.Nino
import api.models.errors._
import api.models.request.NinoAndJsonBodyRawData
import mocks.MockAppConfig
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.data.SubmitEndOfPeriodStatementData._
import v2.models.request.SubmitEndOfPeriodStatementRequest

class SubmitEndOfPeriodStatementValidatorSpec extends UnitSpec {

  private val validNino   = "AA123456A"
  private val invalidNino = "Darth Sidious"

  private val validRawData: NinoAndJsonBodyRawData =
    NinoAndJsonBodyRawData(
      validNino,
      AnyContentAsJson(
        jsonRequestBody(
          typeOfBusiness = "foreign-property",
          startDate = validRequest.accountingPeriod.startDate,
          endDate = validRequest.accountingPeriod.endDate
        ))
    )

  private val requestWithInvalidNino: NinoAndJsonBodyRawData =
    NinoAndJsonBodyRawData(invalidNino, AnyContentAsJson(jsonRequestBody()))

  private val requestWithInvalidNinoAndBodyFields: NinoAndJsonBodyRawData =
    NinoAndJsonBodyRawData(
      invalidNino,
      AnyContentAsJson(
        jsonRequestBody(
          typeOfBusiness = "invalid-type-of-business",
          businessId = "invalid-business-id",
          startDate = "2023-07-07",
          endDate = "2022-07-07"
        ))
    )

  private val requestWithInvalidBodyFields: NinoAndJsonBodyRawData =
    NinoAndJsonBodyRawData(
      validNino,
      AnyContentAsJson(
        jsonRequestBody(
          businessId = "invalid-business-id",
          startDate = "2023-07-07",
          endDate = "2022-07-07"
        )))

  class Test extends MockAppConfig {
    implicit val correlationId: String = "1234"

    val validator = new SubmitEndOfPeriodStatementValidator()
  }

  "parseAndValidateRequest()" should {
    "return the parsed domain object" when {
      "the request is valid" in new Test {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidateRequest(validRawData)

        result shouldBe Right(SubmitEndOfPeriodStatementRequest(Nino(validNino), validRequest))
      }
    }

    "perform the validation and wrap the error in a response wrapper" when {
      "the request has one error" in new Test {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidateRequest(requestWithInvalidNino)

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "perform the validation and wrap the error in a response wrapper" when {
      "the request has multiple errors caught during preParse" in new Test {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidateRequest(requestWithInvalidNinoAndBodyFields)

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                NinoFormatError,
                TypeOfBusinessFormatError
              ))))
      }
      "the request has multiple errors caught during postParse" in new Test {
        val result: Either[ErrorWrapper, SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidateRequest(requestWithInvalidBodyFields)

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(
              List(
                BusinessIdFormatError,
                RuleEndDateBeforeStartDateError
              ))))
      }
    }
  }

  "parseAndValidate()" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(validRawData)

        result.isRight shouldBe true
      }
    }

    "return errors" when {
      "an invalid nino is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(requestWithInvalidNino)

        result shouldBe Left(
          List(
            NinoFormatError
          ))
      }
      "an empty json object is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(Json.parse("""{}"""))))

        result shouldBe Left(
          List(
            RuleIncorrectOrEmptyBodyError
          ))
      }

      "mandatory fields are missing" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(Json.parse("""{ "finalised": true }"""))))

        result shouldBe Left(
          List(
            RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/accountingPeriod", "/businessId", "/typeOfBusiness")))
          ))
      }

      "an invalid finalised value is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(jsonRequestBody(finalised = "false"))))

        result shouldBe Left(
          List(
            FinalisedFormatError
          ))
      }
      "an invalid typeOfBusiness is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(jsonRequestBody(typeOfBusiness = "Undercover"))))

        result shouldBe Left(
          List(
            TypeOfBusinessFormatError
          ))
      }
      "an invalid businessId is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(jsonRequestBody(businessId = "invalid"))))

        result shouldBe Left(
          List(
            BusinessIdFormatError
          ))
      }
      "an invalid start date is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(jsonRequestBody(startDate = "not a date"))))

        result shouldBe Left(
          List(
            StartDateFormatError
          ))
      }
      "an invalid end date is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(NinoAndJsonBodyRawData(validNino, AnyContentAsJson(jsonRequestBody(endDate = "not a date"))))

        result shouldBe Left(
          List(
            EndDateFormatError
          ))
      }
      "an invalid start date with end date is supplied" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(
            NinoAndJsonBodyRawData(
              validNino,
              AnyContentAsJson(
                jsonRequestBody(
                  startDate = "2020-10-10",
                  endDate = "2020-10-09"
                ))))

        result shouldBe Left(
          List(
            RuleEndDateBeforeStartDateError
          ))
      }

      "multiple fields are invalid" in new Test {
        val result: Either[Seq[MtdError], SubmitEndOfPeriodStatementRequest] =
          validator.parseAndValidate(
            NinoAndJsonBodyRawData(
              validNino,
              AnyContentAsJson(
                jsonRequestBody(
                  typeOfBusiness = "uk-property",
                  businessId = "XXXXXX",
                  startDate = "XXXXXX",
                  endDate = "XXXXXX",
                  finalised = "false"
                ))))

        result shouldBe Left(
          List(
            BusinessIdFormatError,
            StartDateFormatError,
            EndDateFormatError,
            FinalisedFormatError
          ))
      }
    }
  }

}
