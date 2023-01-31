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

package v2.controllers

import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import utils.{ IdGenerator, Logging }
import v2.controllers.requestParsers.SubmitEndOfPeriodStatementParser
import v2.models.request.SubmitEndOfPeriodStatementRawData
import v2.services._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class SubmitEndOfPeriodStatementController @Inject()(val authService: EnrolmentsAuthService,
                                                     val lookupService: MtdIdLookupService,
                                                     val idGenerator: IdGenerator,
                                                     nrsProxyService: NrsProxyService,
                                                     service: SubmitEndOfPeriodStatementService,
                                                     requestParser: SubmitEndOfPeriodStatementParser,
                                                     cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "SubmitEndOfPeriodStatementController", endpointName = "Submit end of period statement")

  def handleRequest(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = SubmitEndOfPeriodStatementRawData(nino, AnyContentAsJson(request.body))

      val requestHandler =
        RequestHandler
          .withParser(requestParser)
          .withService { parsedRequest =>
            nrsProxyService.submit(nino, parsedRequest.submitEndOfPeriod)
            service.submit(parsedRequest)
          }

      requestHandler.handleRequest(rawData)
    }
}
