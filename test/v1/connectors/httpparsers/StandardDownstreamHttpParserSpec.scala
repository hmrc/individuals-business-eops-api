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

package v1.connectors.httpparsers

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, Reads}
import support.UnitSpec
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import v1.connectors.DownstreamOutcome
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper

// WLOG if Reads tested elsewhere
case class SomeModel(data: String)

object SomeModel {
  implicit val reads: Reads[SomeModel] = Json.reads
}

class StandardDownstreamHttpParserSpec extends UnitSpec {

  val method = "POST"
  val url = "test-url"

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  import v1.connectors.httpparsers.StandardDownstreamHttpParser._

  val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

  val data = "someData"
  val expectedJson: JsValue = Json.obj("data" -> data)

  val model: SomeModel = SomeModel(data)
  val response: ResponseWrapper[SomeModel] = ResponseWrapper(correlationId, model)

  "The generic HTTP parser" when {
    val httpReads: HttpReads[DownstreamOutcome[SomeModel]] = implicitly

    "return a Right Downstream response containing the model object if the response json corresponds to a model object" in {
      val httpResponse = HttpResponse(OK, expectedJson, Map("CorrelationId" -> Seq(correlationId)))

      httpReads.read(method, url, httpResponse) shouldBe Right(response)
    }

    "return an outbound error if a model object cannot be read from the response json" in {
      val badFieldTypeJson: JsValue = Json.obj("incomeSourceId" -> 1234, "incomeSourceName" -> 1234)
      val httpResponse              = HttpResponse(OK, badFieldTypeJson, Map("CorrelationId" -> Seq(correlationId)))
      val expected                  = ResponseWrapper(correlationId, OutboundError(DownstreamError))

      httpReads.read(method, url, httpResponse) shouldBe Left(expected)
    }

    handleErrorsCorrectly(httpReads)
    handleInternalErrorsCorrectly(httpReads)
    handleUnexpectedResponse(httpReads)
  }

  "The generic HTTP parser for empty response" when {
    val httpReads: HttpReads[DownstreamOutcome[Unit]] = implicitly

    "receiving a 204 response" should {
      "return a Right Downstream Response with the correct correlationId and no responseData" in {
        val httpResponse = HttpResponse(NO_CONTENT, "", Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }

    handleErrorsCorrectly(httpReads)
    handleInternalErrorsCorrectly(httpReads)
    handleUnexpectedResponse(httpReads)
  }

  val singleErrorJson: JsValue = Json.parse(
    """
      |{
      |   "code": "CODE",
      |   "reason": "MESSAGE"
      |}
    """.stripMargin
  )

  val multipleErrorsJson: JsValue = Json.parse(
    """
      |{
      |   "failures": [
      |       {
      |           "code": "CODE 1",
      |           "reason": "MESSAGE 1"
      |       },
      |       {
      |           "code": "CODE 2",
      |           "reason": "MESSAGE 2"
      |       }
      |   ]
      |}
    """.stripMargin
  )

  val malformedErrorJson: JsValue = Json.parse(
    """
      |{
      |   "coed": "CODE",
      |   "resaon": "MESSAGE"
      |}
    """.stripMargin
  )

  private def handleErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, CONFLICT, UNPROCESSABLE_ENTITY).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "be able to parse a single error" in {
          val httpResponse = HttpResponse(responseCode, singleErrorJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, IfsErrors.single(IfsErrorCode("CODE"))))
        }

        "be able to parse multiple errors" in {
          val httpResponse = HttpResponse(responseCode, multipleErrorsJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe {
            Left(ResponseWrapper(correlationId, IfsErrors(List(IfsErrorCode("CODE 1"), IfsErrorCode("CODE 2")))))
          }
        }

        "return an outbound error when the error returned doesn't match the Error model" in {
          val httpResponse = HttpResponse(responseCode, malformedErrorJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(DownstreamError)))
        }
      }
    )

  private def handleInternalErrorsCorrectly[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach(responseCode =>
      s"receiving a $responseCode response" should {
        "return an outbound error when the error returned matches the Error model" in {
          val httpResponse = HttpResponse(responseCode, singleErrorJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(DownstreamError)))
        }

        "return an outbound error when the error returned doesn't match the Error model" in {
          val httpResponse = HttpResponse(responseCode, malformedErrorJson, Map("CorrelationId" -> Seq(correlationId)))

          httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(DownstreamError)))
        }
      }
    )

  private def handleUnexpectedResponse[A](httpReads: HttpReads[DownstreamOutcome[A]]): Unit =
    "receiving an unexpected response" should {
      val responseCode = 499
      "return an outbound error when the error returned matches the Error model" in {
        val httpResponse = HttpResponse(responseCode, singleErrorJson, Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(DownstreamError)))
      }

      "return an outbound error when the error returned doesn't match the Error model" in {
        val httpResponse = HttpResponse(responseCode, malformedErrorJson, Map("CorrelationId" -> Seq(correlationId)))

        httpReads.read(method, url, httpResponse) shouldBe Left(ResponseWrapper(correlationId, OutboundError(DownstreamError)))
      }
    }
}