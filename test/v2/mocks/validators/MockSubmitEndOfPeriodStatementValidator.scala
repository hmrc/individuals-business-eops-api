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

package v2.mocks.validators

import api.models.errors.{ ErrorWrapper, MtdError }
import org.scalamock.handlers.{ CallHandler, CallHandler1 }
import org.scalamock.scalatest.MockFactory
import v2.controllers.requestParsers.validators.SubmitEndOfPeriodStatementValidator
import v2.models.request.SubmitEndOfPeriodStatementRawData

trait MockSubmitEndOfPeriodStatementValidator extends MockFactory {

  val mockValidator: SubmitEndOfPeriodStatementValidator = mock[SubmitEndOfPeriodStatementValidator]

  object MockSubmitEndOfPeriodStatementValidator {

    def validate(data: SubmitEndOfPeriodStatementRawData): CallHandler1[SubmitEndOfPeriodStatementRawData, Option[Seq[MtdError]]] = {
      (mockValidator.validateRequest(_: SubmitEndOfPeriodStatementRawData)).expects(data)
    }

    def wrapErrors(errors: Seq[MtdError])(implicit correlationId: String): CallHandler[ErrorWrapper] = {
      (mockValidator.wrapErrors(_: Seq[MtdError])(_: String)).expects(errors, correlationId)
    }
  }
}
