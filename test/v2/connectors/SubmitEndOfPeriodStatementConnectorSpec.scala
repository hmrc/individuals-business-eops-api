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

package v2.connectors

import api.connectors.{ ConnectorSpec, DownstreamOutcome }
import api.models.domain.{ Nino, TaxYear }
import api.models.outcomes.ResponseWrapper
import org.scalamock.handlers.CallHandler
import play.api.libs.json.JsObject
import v2.data.SubmitEndOfPeriodStatementData._
import v2.models.downstream.TypeOfBusiness
import v2.models.errors._
import v2.models.request.SubmitEndOfPeriodStatementRequest

import scala.concurrent.Future

class SubmitEndOfPeriodStatementConnectorSpec extends ConnectorSpec {

  val nino: String          = "AA123456A"
  private val preTysTaxYear = TaxYear.fromMtd("2022-23")
  private val tysTaxYear    = TaxYear.fromMtd("2023-24")

  trait Test {
    _: ConnectorTest =>
    def taxYear: TaxYear

    val connector: SubmitEndOfPeriodStatementConnector = new SubmitEndOfPeriodStatementConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val request: SubmitEndOfPeriodStatementRequest = SubmitEndOfPeriodStatementRequest(
      nino = Nino(nino),
      validRequest
    )

    val tysRequest: SubmitEndOfPeriodStatementRequest = SubmitEndOfPeriodStatementRequest(
      nino = Nino(nino),
      validTysRequest
    )

    protected def stubHttpResponse(outcome: DownstreamOutcome[Unit]): CallHandler[Future[DownstreamOutcome[Unit]]]#Derived = {
      willPost(
        url = s"$baseUrl/income-tax/income-sources/nino/$nino/$incomeSourceType/" +
          s"$accountingPeriodStartDate/$accountingPeriodEndDate/declaration?incomeSourceId=$incomeSourceId",
        body = JsObject.empty,
      ).returns(Future.successful(outcome))
    }

    protected def stubTysHttpResponse(outcome: DownstreamOutcome[Unit]): CallHandler[Future[DownstreamOutcome[Unit]]]#Derived = {
      willPostEmpty(
        url = s"$baseUrl/income-tax/income-sources/${taxYear.asTysDownstream}/" +
          s"$nino/$incomeSourceId/${TypeOfBusiness.toTys(incomeSourceType)}/$accountingPeriodStartDateTys/$accountingPeriodEndDateTys/declaration",
      ).returns(Future.successful(outcome))
    }
  }

  "Submit end of period statement" when {

    val outcome = Right(ResponseWrapper(correlationId, ()))

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        def taxYear: TaxYear = preTysTaxYear
        stubHttpResponse(outcome)

        val result: DownstreamOutcome[Unit] = await(connector.submitPeriodStatement(request))
        result shouldBe outcome
      }
    }

    "a valid request is supplied for a Tax Year Specific tax year" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        def taxYear: TaxYear = tysTaxYear
        stubTysHttpResponse(outcome)

        val result: DownstreamOutcome[Unit] = await(connector.submitPeriodStatement(tysRequest))
        result shouldBe outcome
      }
    }

    "a request returning a single error" should {

      val downstreamErrorResponse: DownstreamStandardError =
        DownstreamStandardError(List(DownstreamErrorCode("FORMAT_NINO")))
      val outcome = Left(ResponseWrapper(correlationId, downstreamErrorResponse))

      "return an unsuccessful response with the correct correlationId and a single error" in new IfsTest with Test {
        def taxYear: TaxYear = preTysTaxYear
        stubHttpResponse(outcome)

        val result: DownstreamOutcome[Unit] = await(connector.submitPeriodStatement(request))
        result shouldBe outcome
      }

      "return an unsuccessful response with the correct correlationId and a single error given a TYS tax year request" in new TysIfsTest with Test {
        def taxYear: TaxYear = tysTaxYear
        stubTysHttpResponse(outcome)

        val result: DownstreamOutcome[Unit] = await(connector.submitPeriodStatement(tysRequest))
        result shouldBe outcome
      }
    }

    "a request returning multiple errors" should {

      val downstreamErrorResponse: DownstreamStandardError =
        DownstreamStandardError(List(DownstreamErrorCode("FORMAT_NINO"), DownstreamErrorCode("INTERNAL_SERVER_ERROR")))
      val outcome = Left(ResponseWrapper(correlationId, downstreamErrorResponse))

      "return an unsuccessful response with the correct correlationId and multiple errors" in new IfsTest with Test {
        def taxYear: TaxYear = preTysTaxYear
        stubHttpResponse(outcome)

        val result: DownstreamOutcome[Unit] = await(connector.submitPeriodStatement(request))
        result shouldBe outcome
      }

      "return an unsuccessful response with the correct correlationId and multiple errors given a TYS tax year request" in new TysIfsTest with Test {
        def taxYear: TaxYear = tysTaxYear
        stubTysHttpResponse(outcome)

        val result: DownstreamOutcome[Unit] = await(connector.submitPeriodStatement(tysRequest))
        result shouldBe outcome
      }
    }
  }
}
