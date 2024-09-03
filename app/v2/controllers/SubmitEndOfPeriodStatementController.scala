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

import api.controllers.{AuditHandler, AuthorisedController, EndpointLogContext, RequestContext, RequestHandler}
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import config.AppConfig
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import routing.{Version, Version2}
import utils.IdGenerator
import v2.controllers.validators.SubmitEndOfPeriodStatementValidatorFactory
import v2.services._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class SubmitEndOfPeriodStatementController @Inject() (val authService: EnrolmentsAuthService,
                                                      val lookupService: MtdIdLookupService,
                                                      val idGenerator: IdGenerator,
                                                      service: SubmitEndOfPeriodStatementService,
                                                      auditService: AuditService,
                                                      validatorFactory: SubmitEndOfPeriodStatementValidatorFactory,
                                                      cc: ControllerComponents)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends AuthorisedController(cc) {

  override val endpointName: String = "submit-end-of-period-statement"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "SubmitEndOfPeriodStatementController", endpointName = "Submit end of period statement")

  def handleRequest(nino: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, request.body)

      val requestHandler =
        RequestHandler
          .withValidator(validator)
          .withService(service.submit)
          .withAuditing(AuditHandler(
            auditService,
            auditType = "SubmitEOPSStatement",
            transactionName = "submit-eops-statement",
            apiVersion = Version.from(request, orElse = Version2),
            params = Map("nino" -> nino),
            Some(request.body)
          ))
      requestHandler.handleRequest()
    }

}
