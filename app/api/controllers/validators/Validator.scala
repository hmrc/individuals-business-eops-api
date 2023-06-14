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
import api.models.errors.{ BadRequestError, ErrorWrapper, MtdError }
import api.models.request.RawData
import utils.Logging

object Validator {
  type PreParseValidationCaller[A <: RawData]  = A => Seq[MtdError]
  type PreParseValidationCallers[A <: RawData] = Seq[PreParseValidationCaller[A]]

  type ParserValidationCaller[A <: RawData, PARSED] = A => Either[Seq[MtdError], PARSED]

  type PostParseValidationCaller[PARSED]  = PARSED => Seq[MtdError]
  type PostParseValidationCallers[PARSED] = Seq[PostParseValidationCaller[PARSED]]
}

trait Validator[RAW <: RawData, PARSED] extends Logging {

  protected val preParserValidations: PreParseValidationCallers[RAW]
  protected val parserValidation: ParserValidationCaller[RAW, PARSED]
  protected val postParserValidations: PostParseValidationCallers[PARSED]

  def parseAndValidateRequest(data: RAW)(implicit correlationId: String): Either[ErrorWrapper, PARSED] = {
    val result = parseAndValidate(data)
    wrap(result)
  }

  def parseAndValidate(data: RAW): Either[Seq[MtdError], PARSED] = {
    for {
      _      <- runPreParseValidations(data)
      parsed <- runParserValidation(data)
      _      <- runPostParseValidations(parsed)

    } yield parsed
  }

  private def runPreParseValidations(data: RAW): Either[Seq[MtdError], RAW] = {
    val errors = preParserValidations.flatMap(_(data))
    errors match {
      case _ if errors.nonEmpty => Left(errors)
      case _                    => Right(data)
    }
  }

  private def runParserValidation(data: RAW): Either[Seq[MtdError], PARSED] = {
    parserValidation(data)
  }

  private def runPostParseValidations(parsed: PARSED): Either[Seq[MtdError], PARSED] = {
    val errors = postParserValidations.flatMap(_(parsed))
    errors match {
      case _ if errors.nonEmpty => Left(errors)
      case _                    => Right(parsed)
    }
  }

  private def wrap(result: Either[Seq[MtdError], PARSED])(implicit correlationId: String): Either[ErrorWrapper, PARSED] = {
    result match {
      case Right(parsed) =>
        logger.info(
          "[RequestParser][parseRequest] " +
            s"Validation successful for the request with CorrelationId: $correlationId")
        Right(parsed)

      case Left(err :: Nil) =>
        logger.warn(
          "[RequestParser][parseRequest] " +
            s"Validation failed with ${err.code} error for the request with CorrelationId: $correlationId")
        Left(ErrorWrapper(correlationId, err, None))
      case Left(errs) =>
        logger.warn(
          "[RequestParser][parseRequest] " +
            s"Validation failed with ${errs.map(_.code).mkString(",")} error for the request with CorrelationId: $correlationId")
        Left(ErrorWrapper(correlationId, BadRequestError, Some(errs)))
    }
  }
}
