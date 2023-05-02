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

package api.connectors.httpparsers

import api.connectors.DownstreamOutcome
import api.connectors.httpparsers.StandardDownstreamHttpParser.SuccessCode
import api.models.errors
import api.models.errors.{ DownstreamBvrError, DownstreamErrorCode, DownstreamErrors, DownstreamValidationRuleFailure, OutboundError }
import api.models.outcomes.ResponseWrapper
import play.api.http.Status._
import play.api.libs.json._
import support.UnitSpec
import uk.gov.hmrc.http.{ HttpReads, HttpResponse }

// WLOG if Reads tested elsewhere
case class SomeModel(data: String)

object SomeModel {
  implicit val reads: Reads[SomeModel] = Json.reads
}

class StandardDownstreamHttpParserSpec extends UnitSpec {

  val method = "POST"
  val url    = "test-url"

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val data                  = "someData"
  val expectedJson: JsValue = Json.obj("data" -> data)
  val model: SomeModel      = SomeModel(data)

  "The generic HTTP parser" when {
    implicit val httpReads: HttpReads[DownstreamOutcome[SomeModel]] = StandardDownstreamHttpParser.reads[SomeModel]

    "return a Right Downstream response containing the model object if the response json corresponds to a model object" in {
      val httpResponse = HttpResponse(OK, expectedJson, Map("CorrelationId" -> Seq(correlationId)))

      httpReads.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, model))
    }

    "return an outbound error if a model object cannot be read from the response json" in {
      val badFieldTypeJson = Json.obj("something" -> 1234)
      val httpResponse     = HttpResponse(OK, badFieldTypeJson, Map("CorrelationId" -> Seq(correlationId)))

      httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(errors.InternalError)))
    }

    handleErrorsCorrectly
    handleInternalErrorsCorrectly
    handleUnexpected4xxResponseCorrectly
  }

  "The generic HTTP parser for an empty downstream response" when {
    implicit val httpReads: HttpReads[DownstreamOutcome[Unit]] = StandardDownstreamHttpParser.readsEmpty(successCode = SuccessCode(ACCEPTED))

    "receiving a 202 response" should {
      "return a Right Downstream Response with the correct correlationId and no responseData" in {
        val httpResponse = HttpResponse(ACCEPTED, "", Map("CorrelationId" -> Seq(correlationId)))

        val result = httpReads.read(method, url, httpResponse)
        result shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    handleErrorsCorrectly
    handleInternalErrorsCorrectly
    handleUnexpected4xxResponseCorrectly
  }

  private def bvrErrorJson(types: String*) = {
    val items = types.zipWithIndex.map {
      case (tpe, i) =>
        Json.parse(s"""
          |{
          |   "id": "ID $i", "type":"$tpe", "text":"MESSAGE $i"
          |}
          |""".stripMargin)
    }

    Json.parse(s"""
      |{
      |   "bvrfailureResponseElement":{
      |      "code":"CODE",
      |      "reason":"Ignored top-level reason",
      |      "validationRuleFailures": ${JsArray(items)}
      |   }
      |}""".stripMargin)
  }

  val standardErrorJson: JsValue =
    Json.parse("""
      |{
      |   "failures": [
      |       {
      |           "code": "CODE", "reason": "ignored"
      |       }
      |   ]
      |}
      |""".stripMargin)

  private def handleErrorsCorrectly[A](implicit httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, CONFLICT, UNPROCESSABLE_ENTITY).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "be able to parse a standard error" in {
          val httpResponse = HttpResponse(responseCode, standardErrorJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, DownstreamErrors(DownstreamErrorCode("CODE"))))
        }

        "return an outbound error when the error returned doesn't match the Error model" in {
          val malformedErrorJson = Json.obj("something" -> 1234)
          val httpResponse       = HttpResponse(responseCode, malformedErrorJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(errors.InternalError)))
        }

        "trim any non-ERR items from a BVR error" in {
          val downstreamErrorsJson: JsValue = bvrErrorJson("INFO", "ERR", "WARN")
          val httpResponse                  = HttpResponse(responseCode, downstreamErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(
            ResponseWrapper(correlationId,
                            DownstreamBvrError(
                              "CODE",
                              List(DownstreamValidationRuleFailure("ID 1", "MESSAGE 1"))
                            )))
        }

        "return an outbound error if no ERR items are present in a BVR error" in {
          val downstreamErrorsJson: JsValue = bvrErrorJson("WARN")
          val httpResponse                  = HttpResponse(responseCode, downstreamErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(errors.InternalError)))
        }
    })

  private def handleInternalErrorsCorrectly[A](implicit httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach(responseCode =>
      s"receiving a $responseCode response" should returnAnOutboundDownstreamError(responseCode))

  private def handleUnexpected4xxResponseCorrectly[A](implicit httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    "receiving an unexpected response" should returnAnOutboundDownstreamError(IM_A_TEAPOT)

  private def returnAnOutboundDownstreamError[A](responseCode: Int)(implicit httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    "return an outbound DownstreamError" in {
      val errorJsonIgnored: JsValue = JsObject.empty
      val httpResponse              = HttpResponse(responseCode, errorJsonIgnored, Map("CorrelationId" -> Seq(correlationId)))

      httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(errors.InternalError)))
    }
}
