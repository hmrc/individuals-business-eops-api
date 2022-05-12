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

package v2.controllers.requestParsers.validators

import mocks.MockAppConfig
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.data.SubmitEndOfPeriodStatementData._
import v2.models.errors._
import v2.models.request.SubmitEndOfPeriodStatementRawData

class SubmitEndOfPeriodStatementValidatorSpec extends UnitSpec {

  private val validNino   = "AA123456A"
  private val invalidNino = "Darth Sidious"

  class Test extends MockAppConfig {
    val validator = new SubmitEndOfPeriodStatementValidator()
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
      "an empty json object is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(Json.parse("""{}""")))) shouldBe List(
          RuleIncorrectOrEmptyBodyError
        )
      }

      "mandatory fields are missing" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(Json.parse("""{ "finalised": true }""")))) shouldBe List(
          RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/typeOfBusiness", "/businessId", "/accountingPeriod")))
        )
      }

      "an invalid finalised value is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(finalised = "false")))) shouldBe List(
          FinalisedFormatError
        )
      }
      "an invalid typeOfBusiness is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(typeOfBusiness = "Undercover")))) shouldBe List(
          TypeOfBusinessFormatError
        )
      }
      "an invalid businessId is supplied" in new Test {
        validator.validate(SubmitEndOfPeriodStatementRawData(validNino, AnyContentAsJson(fullValidJson(businessId = "invalid")))) shouldBe List(
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
        validator.validate(
          SubmitEndOfPeriodStatementRawData(validNino,
                                            AnyContentAsJson(
                                              fullValidJson(
                                                startDate = "2020-10-10",
                                                endDate = "2020-10-09"
                                              )))) shouldBe List(
          RuleEndDateBeforeStartDateError
        )
      }

      "multiple fields are invalid" in new Test {
        validator.validate(
          SubmitEndOfPeriodStatementRawData(validNino,
                                            AnyContentAsJson(
                                              fullValidJson(
                                                typeOfBusiness = "uk-property",
                                                businessId = "XXXXXX",
                                                startDate = "XXXXXX",
                                                endDate = "XXXXXX",
                                                finalised = "false"
                                              )))) shouldBe List(
          BusinessIdFormatError,
          StartDateFormatError,
          EndDateFormatError,
          FinalisedFormatError
        )
      }
    }
  }
}
