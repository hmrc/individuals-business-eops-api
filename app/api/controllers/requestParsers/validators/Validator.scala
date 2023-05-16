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
import api.models.request.RawData
import utils.Logging

trait Validator[A <: RawData] extends Logging {

  type Validation[T <: RawData] = T => Seq[MtdError]

  protected def validations: Seq[Validation[A]]

  def validateRequest(rawData: A): Option[Seq[MtdError]] = {
    validations.view
      .map(validation => validation(rawData))
      .find(_.nonEmpty)
      .map(handleErrors)
  }

  private def handleErrors(errors: Seq[MtdError]): Seq[MtdError] = {
    errors
      .groupBy(_.code)
      .values
      .map(mergeErrors)
      .toList
  }

  private def mergeErrors(errors: Seq[MtdError]): MtdError = {
    errors.reduce { (e1, e2) =>
      e1.copy(paths = Some((e1.paths.toList.flatten ++ e2.paths.toList.flatten).distinct))
    }
  }

  def wrapErrors(errors: Seq[MtdError])(implicit correlationId: String): ErrorWrapper = {
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
