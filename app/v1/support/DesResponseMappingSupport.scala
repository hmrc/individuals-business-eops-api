/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.support

import utils.Logging
import v1.controllers.EndpointLogContext
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper

trait DesResponseMappingSupport {
  self: Logging =>

  final def mapDesErrors[D](errorCodeMap: PartialFunction[String, MtdError])(desResponseWrapper: ResponseWrapper[DesError])(
    implicit logContext: EndpointLogContext): ErrorWrapper = {

    lazy val defaultErrorCodeMapping: String => MtdError = { code =>
      logger.warn(s"[${logContext.controllerName}] [${logContext.endpointName}] - No mapping found for error code $code")
      DownstreamError
    }

    desResponseWrapper match {
      case ResponseWrapper(correlationId, DesErrors(error :: Nil)) =>
        ErrorWrapper(correlationId, errorCodeMap.applyOrElse(error.code, defaultErrorCodeMapping), None)

      case ResponseWrapper(correlationId, DesErrors(errorCodes)) =>
        val mtdErrors = errorCodes.map(error => errorCodeMap.applyOrElse(error.code, defaultErrorCodeMapping))

        if (mtdErrors.contains(DownstreamError)) {
          logger.warn(
            s"[${logContext.controllerName}] [${logContext.endpointName}] [CorrelationId - $correlationId]" +
              s" - downstream returned ${errorCodes.map(_.code).mkString(",")}. Revert to ISE")
          ErrorWrapper(correlationId, DownstreamError, None)
        } else {
              mtdErrors.headOption map {
                case RuleConsolidatedExpensesError => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case RuleMismatchedStartDateError => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case RuleMismatchedEndDateError => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case RuleClass4Over16Error => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case RuleClass4PensionAge => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case RuleFHLPrivateUseAdjustment => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case RuleNonFHLPrivateUseAdjustment => ErrorWrapper(correlationId, BVRError, Some(mtdErrors))
                case _ => ErrorWrapper(correlationId, BadRequestError, Some(mtdErrors))
              }

        }

      case ResponseWrapper(correlationId, OutboundError(error, errors)) =>
        ErrorWrapper(correlationId, error, errors)
    }
  }
}
