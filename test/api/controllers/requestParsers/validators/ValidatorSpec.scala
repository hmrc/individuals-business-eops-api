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

package api.controllers.requestParsers.validators

import api.models.errors.{ BadRequestError, ErrorWrapper, MtdError, NinoFormatError, TaxYearFormatError }
import api.models.request.RawData
import org.scalamock.scalatest.MockFactory
import play.api.http.Status.BAD_REQUEST
import support.UnitSpec

class ValidatorSpec extends UnitSpec with MockFactory {

  case class TestRawData() extends RawData

  private class TestValidator(val validations: Seq[TestRawData => Seq[MtdError]]) extends Validator[TestRawData]

  private trait Test {
    val validations: Seq[TestRawData => Seq[MtdError]]
    val rawData: TestRawData          = TestRawData()
    lazy val validator: TestValidator = new TestValidator(validations)

    implicit val correlationId: String = "test correlation id"
  }

  "validateRequest" should {
    "return None when there are no validation errors" in new Test {
      override val validations: Seq[TestRawData => Seq[MtdError]] = List(_ => List())
      validator.validateRequest(rawData) shouldBe None
    }

    "return Some List of errors" when {
      "a single validation error occurs" in new Test {
        override val validations: Seq[TestRawData => Seq[MtdError]] = List(_ => List(NinoFormatError))
        validator.validateRequest(rawData) shouldBe Some(List(NinoFormatError))
      }

      "an error occurs during the first of multiple validations" in new Test {
        override val validations: Seq[TestRawData => Seq[MtdError]] = List(_ => List(NinoFormatError), _ => List(TaxYearFormatError))
        validator.validateRequest(rawData) shouldBe Some(List(NinoFormatError))
      }

      "merge errors with the same code but different paths into a single error" in new Test {
        private val TestError = MtdError("TEST_ERROR", "error message", BAD_REQUEST, Some(Seq("path 1")))
        override val validations: Seq[TestRawData => Seq[MtdError]] =
          List(_ => List(NinoFormatError, TestError, TestError.copy(paths = Some(Seq("path 2")))))

        validator.validateRequest(rawData) shouldBe Some(List(NinoFormatError, TestError.copy(paths = Some(Seq("path 1", "path 2")))))
      }
    }
  }

  "wrapErrors" should {
    "return an ErrorWrapper with a single error when there is only one error" in new Test {
      override val validations: Seq[TestRawData => Seq[MtdError]] = List(_ => List())
      validator.wrapErrors(List(NinoFormatError)) shouldBe ErrorWrapper(correlationId, NinoFormatError, None)
    }

    "return an ErrorWrapper with a BadRequestError and multiple errors when there are multiple errors" in new Test {
      override val validations: Seq[TestRawData => Seq[MtdError]] = List(_ => List(NinoFormatError, TaxYearFormatError))
      val expectedOutcome: ErrorWrapper =
        ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, TaxYearFormatError)))

      validator.wrapErrors(List(NinoFormatError, TaxYearFormatError)) shouldBe expectedOutcome
    }
  }
}
