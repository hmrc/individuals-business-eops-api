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

package v1.controllers.requestParsers.validators

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors.{BusinessIdFormatError, EndDateFormatError, FinalisedFormatError, MtdError, NinoFormatError, RangeEndDateBeforeStartDateError, RuleNotFinalisedError, StartDateFormatError, TypeOfBusinessFormatError}
import v1.models.requestData.SubmitEndOfPeriodStatementRawData

class SubmitEndOfPeriodStatementValidatorSpec extends UnitSpec with MockAppConfig {

  private val validNino = "AA123456A"
  private val invalidNino = "Darth Sidious"

  class Test extends MockAppConfig {
    val validator = new SubmitEndOfPeriodStatementValidator(mockAppConfig)

    val emptyRequestBodyJson: JsValue = Json.parse("""{}""")
    def fullValidJson(typeOfBusiness: String = "self-employment",
                      businessId: String = "XAIS12345678910",
                      startDate: String = "2021-04-06",
                      endDate: String = "2022-04-05",
                      finalised: String = "true"

                     ): JsValue = Json.parse(
      s"""{
        | "typeOfBusiness":"$typeOfBusiness",
        | "businessId":"$businessId",
        | "accountingPeriod": {
        |   "startDate": "$startDate",
        |   "endDate":"$endDate"
        | },
        | "finalised": $finalised
        |}
        |""".stripMargin
    )
  }

  "Running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson()))) shouldBe Nil
      }
    }

    "return errors" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(invalidNino, AnyContentAsJson(fullValidJson()))) shouldBe List(
          NinoFormatError
        )
      }
      "an invalid format finalised is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(finalised = "\"true\"")))) shouldBe List(
          FinalisedFormatError
        )
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(finalised = "\"false\"")))) shouldBe List(
          FinalisedFormatError
        )
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(finalised = "\"asdfgh\"")))) shouldBe List(
          FinalisedFormatError
        )
      }

      "an invalid rule for finalised is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(finalised = "false")))) shouldBe List(
          RuleNotFinalisedError
        )
      }
      "an invalid typeOfBusiness is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(typeOfBusiness = "Undercover")))) shouldBe List(
          TypeOfBusinessFormatError
        )
      }
      "an invalid businessId is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(businessId = "wow much id")))) shouldBe List(
          BusinessIdFormatError
        )
      }
      "an invalid start date is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(startDate = "not a date")))) shouldBe List(
          StartDateFormatError
        )
      }
      "an invalid end date is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(endDate = "not a date")))) shouldBe List(
          EndDateFormatError
        )
      }
      "an invalid start date with end date is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(
          startDate = "2020-10-10",
          endDate = "2020-10-09"
        )))) shouldBe List(
          RangeEndDateBeforeStartDateError
        )
      }

      "multiple fields are invalid that wont map the request body to the model" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(
          typeOfBusiness = "XXXXXX",
          businessId = "XXXXXX",
          startDate = "XXXXXX",
          endDate = "XXXXXX",
          finalised = "\"XXXXXX\""
        )))) shouldBe List(
          FinalisedFormatError,StartDateFormatError,EndDateFormatError,TypeOfBusinessFormatError,BusinessIdFormatError
        )
      }
      "multiple fields are invalid that do map to the request body to the model" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(
          typeOfBusiness = "uk-property",
          businessId = "XXXXXX",
          startDate = "XXXXXX",
          endDate = "XXXXXX",
          finalised = "false"
        )))) shouldBe List(
          BusinessIdFormatError,StartDateFormatError,RuleNotFinalisedError,EndDateFormatError
        )
      }
    }
  }
}
