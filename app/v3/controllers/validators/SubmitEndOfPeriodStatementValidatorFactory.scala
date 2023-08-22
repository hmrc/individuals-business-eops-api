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

package v3.controllers.validators

import api.controllers.validators.Validator
import api.controllers.validators.resolvers._
import api.models.downstream.TypeOfBusiness
import api.models.errors.{FinalisedFormatError, MtdError, TypeOfBusinessFormatError}
import cats.data.Validated
import cats.data.Validated._
import cats.implicits._
import play.api.libs.json._
import v3.models.request.{SubmitEndOfPeriodRequestBody, SubmitEndOfPeriodStatementRequestData}

import javax.inject.Singleton

@Singleton
class SubmitEndOfPeriodStatementValidatorFactory {

  private val resolveJson = new ResolveJsonObject[SubmitEndOfPeriodRequestBody]()

  def validator(nino: String, body: JsValue): Validator[SubmitEndOfPeriodStatementRequestData] =
    new Validator[SubmitEndOfPeriodStatementRequestData] {

      def validate: Validated[Seq[MtdError], SubmitEndOfPeriodStatementRequestData] =
        validateTypeOfBusiness andThen { _ =>
          (
            ResolveNino(nino),
            resolveJson(body)
          )
            .mapN(SubmitEndOfPeriodStatementRequestData) andThen validateMore
        }

      private def validateTypeOfBusiness: Validated[Seq[MtdError], Unit] = {
        val either = (body \ "typeOfBusiness").validate[String] match {
          case JsSuccess(typeOfBusiness, _) if TypeOfBusiness.parser.isDefinedAt(typeOfBusiness) => Right(())
          case JsSuccess(_, _)                                                                   => Left(List(TypeOfBusinessFormatError))
          case _: JsError                                                                        => Right(())
        }

        Validated.fromEither(either)
      }

      private def validateMore(parsed: SubmitEndOfPeriodStatementRequestData): Validated[Seq[MtdError], SubmitEndOfPeriodStatementRequestData] = {
        import parsed.body._
        List(
          ResolveBusinessId(businessId),
          ResolveDateRange(accountingPeriod.startDate -> accountingPeriod.endDate),
          validateFinalised(finalised)
        )
          .traverse(identity)
          .map(_ => parsed)

      }

      private def validateFinalised(finalised: Boolean): Validated[Seq[MtdError], Unit] = {
        if (finalised) {
          Valid(())
        } else {
          Invalid(List(FinalisedFormatError))
        }
      }

    }

}
