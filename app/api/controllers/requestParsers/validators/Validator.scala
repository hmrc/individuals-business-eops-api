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

import api.models.errors.{ BadRequestError, ErrorWrapper, MtdError }
//import api.models.errors.{ BadRequestError, ErrorWrapper, MtdError, InternalError }
import api.models.request.RawData
import utils.Logging

trait Validator[A <: RawData] extends Logging {

  type ValidationLevel[T] = T => List[MtdError]

  protected def validations: List[A => List[MtdError]]

  def validateRequest(rawData: A): Option[List[MtdError]] = {
    val result = validations.foldLeft(List.empty[MtdError])((errors, validation) => errors ++ validation(rawData))

    result match {
      case Nil => None
      case _   => Some(result)
    }
  }

  def wrapErrors(errors: List[MtdError])(implicit correlationId: String): ErrorWrapper = {
    errors match {
      case err :: Nil =>
        logger.warn(
          message = "[Validator][validateRequest] " +
            s"Validation failed with ${err.code} error for the request with correlationId : $correlationId")
        ErrorWrapper(correlationId, err, None)
      case errs =>
        logger.warn(
          "[Validator][validateRequest] " +
            s"Validation failed with ${errs.map(_.code).mkString(",")} error for the request with correlationId : $correlationId")
        ErrorWrapper(correlationId, BadRequestError, Some(errs))
    }
  }
}
