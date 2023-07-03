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

package api.controllers.validators.validations

import api.controllers.validators.validations.JsonFormatValidation
import api.models.errors.{ MtdError, RuleIncorrectOrEmptyBodyError }
import play.api.libs.json.{ Json, OFormat }
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class JsonFormatValidationSpec extends UnitSpec with JsonErrorValidators {

  case class TestDataObject(fieldOne: String, fieldTwo: String)
  case class TestDataWrapper(arrayField: Option[Seq[TestDataObject]])

  implicit val testDataObjectFormat: OFormat[TestDataObject]   = Json.format[TestDataObject]
  implicit val testDataWrapperFormat: OFormat[TestDataWrapper] = Json.format[TestDataWrapper]

  "validate" should {
    "return no errors" when {
      "when a valid JSON object with all the necessary fields is supplied" in {

        val validJson = Json.parse("""{ "fieldOne" : "Something", "fieldTwo" : "SomethingElse" }""")

        val validationResult = JsonFormatValidation.validate[TestDataObject](validJson)
        validationResult.isRight shouldBe true
      }
    }

    "return an error " when {
      "required field is missing" in {
        val json = Json.parse("""{ "fieldOne" : "Something" }""")

        val result: Either[Seq[MtdError], TestDataObject] =
          JsonFormatValidation.validate[TestDataObject](json)

        withClue("fieldTwo is missing") {
          result shouldBe Left(List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/fieldTwo")))))
        }
      }

      "required field is missing in array object" in {
        val json = Json.parse("""{ "arrayField" : [{}]}""")

        val result: Either[Seq[MtdError], TestDataWrapper] =
          JsonFormatValidation.validate[TestDataWrapper](json)

        withClue("both fields are missing") {
          result shouldBe Left(List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/arrayField/0/fieldTwo", "/arrayField/0/fieldOne")))))
        }
      }

      "required field is missing in multiple array objects" in {
        val json = Json.parse("""{ "arrayField" : [{}, {}]}""")

        val result: Either[Seq[MtdError], TestDataWrapper] =
          JsonFormatValidation.validate[TestDataWrapper](json)

        withClue("both fields are missing") {
          result shouldBe Left(
            List(
              RuleIncorrectOrEmptyBodyError.copy(
                paths = Some(
                  List(
                    "/arrayField/0/fieldTwo",
                    "/arrayField/0/fieldOne",
                    "/arrayField/1/fieldTwo",
                    "/arrayField/1/fieldOne"
                  )))))
        }
      }

      "empty body is submitted" in {
        val json = Json.parse("""{}""")

        val result: Either[Seq[MtdError], TestDataObject] =
          JsonFormatValidation.validate[TestDataObject](json)

        result shouldBe Left(List(RuleIncorrectOrEmptyBodyError))
      }

      "a non-empty body is submitted with no valid data" in {
        val json = Json.parse("""{"aField": "aValue"}""")

        val result: Either[Seq[MtdError], TestDataWrapper] =
          JsonFormatValidation.validate[TestDataWrapper](json)

        result shouldBe Left(List(RuleIncorrectOrEmptyBodyError))
      }

      "a non-empty body is supplied without any expected fields" in {
        val json = Json.parse("""{"field": "value"}""")

        val result: Either[Seq[MtdError], TestDataObject] =
          JsonFormatValidation.validate[TestDataObject](json)

        result shouldBe Left(
          List(
            RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/fieldTwo", "/fieldOne")))
          ))
      }

      "a field is supplied with the wrong data type" in {
        val json = Json.parse("""{"fieldOne": true, "fieldTwo": "value"}""")

        val result: Either[Seq[MtdError], TestDataObject] =
          JsonFormatValidation.validate[TestDataObject](json)

        result shouldBe Left(List(RuleIncorrectOrEmptyBodyError.copy(paths = Some(List("/fieldOne")))))
      }
    }
  }
}