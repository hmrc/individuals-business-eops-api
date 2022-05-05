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

package v2.models.errors

import support.UnitSpec

class IfsErrorsSpec extends UnitSpec {

  "IfsErrorCode.toMtd" should {
    "convert the error to an MtdError" in {
      IfsErrorCode("test").toMtd shouldBe MtdError("test", "")
    }
  }

  "IfsErrors" should {

    val error  = IfsErrorCode("error")
    val errors = Seq(IfsErrorCode("ERROR 1"), IfsErrorCode("ERROR 2"))

    "read in a singleError" in {
      IfsErrors.single(error) shouldBe IfsErrors(List(IfsErrorCode("error")))
    }

    "read in multiple errors" in {
      IfsErrors.multiple(errors) shouldBe IfsErrors(List(IfsErrorCode("ERROR 1"), IfsErrorCode("ERROR 2")))
    }
  }
}