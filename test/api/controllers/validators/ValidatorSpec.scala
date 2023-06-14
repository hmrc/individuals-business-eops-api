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

package api.controllers.validators

import api.controllers.validators.Validator._
import api.models.errors._
import api.models.request.RawData
import org.scalamock.scalatest.MockFactory
import play.api.http.Status._
import support.UnitSpec

class ValidatorSpec extends UnitSpec with MockFactory {

  // TODO the "level 1/level 2" tests being done here aren't relevant to this API.
  //  A ticket is being raised for the Validator changes to be added to an API that includes
  //  "multi-level" validations in the pre- or post-parser stage, so this test should also be updated then.


  private trait Test {
    implicit val correlationId: String = "1234"

    val validRaw: TestRawData     = TestRawData("ABCDEF", "12345")
    val parsed: TestParsedRequest = TestParsedRequest("ABCDEF", "12345")

    lazy val preParseValidations: PreParseValidationCallers[TestRawData]              = Nil
    lazy val parserValidation: ParserValidationCaller[TestRawData, TestParsedRequest] = _ => Right(parsed)
    lazy val postParseValidations: PostParseValidationCallers[TestParsedRequest]      = Nil

    val validator = new TestValidator(preParseValidations, parserValidation, postParseValidations)
  }

  "validator.parseAndValidateRequest()" should {

    "return the parsed domain object with no errors" when {
      "all data is correct" in new Test {
        val result: Either[ErrorWrapper, TestParsedRequest] = validator.parseAndValidateRequest(validRaw)
        result shouldBe Right(parsed)
      }
    }

    "return a single error" when {
      "just one validation error is found" in new Test {
        override lazy val parserValidation: ParserValidationCaller[TestRawData, TestParsedRequest] =
          _ => Left(List(NinoFormatError))

        val result: Either[ErrorWrapper, TestParsedRequest] = validator.parseAndValidateRequest(validRaw)
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }

    "return multiple errors" when {
      "both the param and body are invalid" in new Test {

        override lazy val preParseValidations: PreParseValidationCallers[TestRawData] =
          List(
            _ => List(NinoFormatError),
            _ => List(RuleIncorrectOrEmptyBodyError)
          )

        val result: Either[ErrorWrapper, TestParsedRequest] = validator.parseAndValidateRequest(validRaw)
        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, RuleIncorrectOrEmptyBodyError))))
      }
    }
  }

  "validator.parseAndValidate()" should {

    "return the parsed domain object with no errors" when {
      "all data is correct" in new Test {
        val levelOneValidationOne = new MockFunctionObject
        val levelOneValidationTwo = new MockFunctionObject

        def levelOneValidations: PostParseValidationCaller[TestParsedRequest] =
          _ =>
            levelOneValidationOne.noErrors() ++
              levelOneValidationTwo.noErrors()

        override lazy val postParseValidations = List(levelOneValidations)

        val result: Either[Seq[MtdError], TestParsedRequest] = validator.parseAndValidate(validRaw)
        result shouldBe Right(parsed)
        levelOneValidationOne.called shouldBe 1
        levelOneValidationTwo.called shouldBe 1
      }
    }

    "return a list of validation errors on level one" when {
      "there are failed validations" in new Test {

        val levelOneValidationOne = new MockFunctionObject
        val levelOneValidationTwo = new MockFunctionObject

        override lazy val postParseValidations = List(levelOneValidations)

        val mockError: MtdError = MtdError("MOCK", "SOME ERROR", CONFLICT)

        def levelOneValidations: PostParseValidationCaller[TestParsedRequest] =
          _ =>
            levelOneValidationOne.noErrors() ++
              levelOneValidationTwo.error(mockError)

        val result: Either[Seq[MtdError], TestParsedRequest] = validator.parseAndValidate(validRaw)
        result shouldBe Left(List(mockError))

        levelOneValidationOne.called shouldBe 1
        levelOneValidationTwo.called shouldBe 1
      }
    }

    "return a list of validation errors on level two" when {
      "there are failed validations only on level two" in new Test {
        val levelOneValidationOne = new MockFunctionObject
        val levelOneValidationTwo = new MockFunctionObject

        val levelTwoValidationOne = new MockFunctionObject
        val levelTwoValidationTwo = new MockFunctionObject

        val mockError: MtdError = MtdError("MOCK", "SOME ERROR ON LEVEL 2", BAD_REQUEST)

        def levelOneValidations: PostParseValidationCaller[TestParsedRequest] =
          _ =>
            levelOneValidationOne.noErrors() ++
              levelOneValidationTwo.noErrors()

        def levelTwoValidations: PostParseValidationCaller[TestParsedRequest] =
          _ =>
            levelTwoValidationOne.noErrors() ++
              levelTwoValidationTwo.error(mockError)

        override lazy val postParseValidations = List(levelOneValidations, levelTwoValidations)

        val result: Either[Seq[MtdError], TestParsedRequest] = validator.parseAndValidate(validRaw)
        result shouldBe Left(List(mockError))

        levelOneValidationOne.called shouldBe 1
        levelOneValidationTwo.called shouldBe 1
        levelTwoValidationOne.called shouldBe 1
        levelTwoValidationTwo.called shouldBe 1
      }
    }
  }
}

class MockFunctionObject {
  var called = 0

  def noErrors(): Seq[MtdError]             = validate(maybeError = None)
  def error(error: MtdError): Seq[MtdError] = validate(maybeError = Some(error))

  private def validate(maybeError: Option[MtdError]): Seq[MtdError] = {
    called = called + 1
    maybeError.toList
  }
}

private case class TestRawData(fieldOne: String, fieldTwo: String) extends RawData
private case class TestParsedRequest(fieldOne: String, fieldTwo: String)

private class TestValidator(
                             override protected val preParserValidations: PreParseValidationCallers[TestRawData],
                             override protected val parserValidation: ParserValidationCaller[TestRawData, TestParsedRequest],
                             override protected val postParserValidations: PostParseValidationCallers[TestParsedRequest]
) extends Validator[TestRawData, TestParsedRequest]
