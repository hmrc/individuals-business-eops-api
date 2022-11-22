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

package v2.support

import support.UnitSpec
import utils.Logging
import v2.controllers.EndpointLogContext
import v2.models.errors._
import v2.models.outcomes.ResponseWrapper

class DownstreamResponseMappingSupportSpec extends UnitSpec {

  implicit val logContext: EndpointLogContext                = EndpointLogContext("ctrl", "ep")
  val mapping: DownstreamResponseMappingSupport with Logging = new DownstreamResponseMappingSupport with Logging {}

  val correlationId = "someCorrelationId"

  object Error1 extends MtdError("msg", "code1")

  object Error2 extends MtdError("msg", "code2")

  val errorCodeMap: PartialFunction[String, MtdError] = {
    case "ERR1" => Error1
    case "ERR2" => Error2
    case "DS"   => InternalError
  }

  "mapping errors from downstream" when {
    "standard error with single error code" when {
      "the error code is in the map provided" must {
        "use the mapping and wrap" in {
          mapping.mapDownstreamErrors(errorCodeMap)(ResponseWrapper(correlationId, DownstreamStandardError(DownstreamErrorCode("ERR1")))) shouldBe
            ErrorWrapper(correlationId, Error1)
        }
      }

      "the error code is not in the map provided" must {
        "default to DownstreamError and wrap" in {
          mapping.mapDownstreamErrors(errorCodeMap)(ResponseWrapper(correlationId, DownstreamStandardError(DownstreamErrorCode("UNKNOWN")))) shouldBe
            ErrorWrapper(correlationId, InternalError)
        }
      }
    }

    "standard error with multiple errors codes" when {
      "all error codes are in the map provided" must {
        "use the mapping and wrap with main error type of BadRequest" in {
          mapping.mapDownstreamErrors(errorCodeMap)(
            ResponseWrapper(correlationId, DownstreamStandardError(DownstreamErrorCode("ERR1"), DownstreamErrorCode("ERR2")))) shouldBe
            ErrorWrapper(correlationId, BadRequestError, Some(Seq(Error1, Error2)))
        }
      }

      "an error code is not in the map provided" must {
        "default main error to DownstreamError ignore other errors" in {
          mapping.mapDownstreamErrors(errorCodeMap)(
            ResponseWrapper(correlationId, DownstreamStandardError(DownstreamErrorCode("ERR1"), DownstreamErrorCode("UNKNOWN")))) shouldBe
            ErrorWrapper(correlationId, InternalError)
        }
      }

      "one of the mapped errors is DownstreamError" must {
        "wrap the errors with main error type of DownstreamError" in {
          mapping.mapDownstreamErrors(errorCodeMap)(
            ResponseWrapper(correlationId, DownstreamStandardError(DownstreamErrorCode("ERR1"), DownstreamErrorCode("DS")))) shouldBe
            ErrorWrapper(correlationId, InternalError)
        }
      }
    }

    "the error is an OutboundError with a single MTD error" must {
      "return the MTD error in an ErrorWrapper" in {
        mapping.mapDownstreamErrors(errorCodeMap)(ResponseWrapper(correlationId, OutboundError(Error1))) shouldBe
          ErrorWrapper(correlationId, Error1)
      }
    }

    "the error is an OutboundError with a main and secondary errors" must {
      "return the MTD errors in the same structure in an ErrorWrapper" in {
        mapping.mapDownstreamErrors(errorCodeMap)(ResponseWrapper(correlationId, OutboundError(Error1, Some(Seq(Error2))))) shouldBe
          ErrorWrapper(correlationId, Error1, Some(Seq(Error2)))
      }
    }

    "bvr errors are returned" must {
      "map to a DownstreamError" in {
        mapping.mapDownstreamErrors(errorCodeMap)(
          ResponseWrapper(correlationId, DownstreamBvrError("ERR1", List(DownstreamValidationRuleFailure("ID 1", "message 1"))))) shouldBe
          ErrorWrapper(correlationId, InternalError)
      }
    }
  }
}
