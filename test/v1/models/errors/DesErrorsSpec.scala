/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.models.errors

import support.UnitSpec

class DesErrorsSpec extends UnitSpec {

  "DesErrorCode.toMtd" should {
    "convert the error to an MtdError" in {
      DesErrorCode("test").toMtd shouldBe MtdError("test", "")
    }
  }

  "DesErrors" should {

    val error  = DesErrorCode("error")
    val errors = Seq(DesErrorCode("ERROR 1"), DesErrorCode("ERROR 2"))

    "read in a singleError" in {
      DesErrors.single(error) shouldBe DesErrors(List(DesErrorCode("error")))
    }

    "read in multiple errors" in {
      DesErrors.multiple(errors) shouldBe DesErrors(List(DesErrorCode("ERROR 1"), DesErrorCode("ERROR 2")))
    }
  }
}
