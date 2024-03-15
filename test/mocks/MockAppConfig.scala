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

package mocks

import cats.data.Validated
import config.{AppConfig, ConfidenceLevelConfig, Deprecation}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import routing.Version

trait MockAppConfig extends MockFactory {

  implicit val mockAppConfig: AppConfig = mock[AppConfig]

  object MockAppConfig {

    // MTD ID Lookup Config
    def mtdIdBaseUrl: CallHandler[String] = (() => mockAppConfig.mtdIdBaseUrl).expects()

    // DES Config
    def desBaseUrl: CallHandler[String]                         = (() => mockAppConfig.desBaseUrl).expects()
    def desToken: CallHandler[String]                           = (() => mockAppConfig.desToken).expects()
    def desEnvironment: CallHandler[String]                     = (() => mockAppConfig.desEnv).expects()
    def desEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.desEnvironmentHeaders).expects()

    // Tax Year Specific IFS Config
    def tysIfsBaseUrl: CallHandler[String]                         = (() => mockAppConfig.tysIfsBaseUrl).expects()
    def tysIfsToken: CallHandler[String]                           = (() => mockAppConfig.tysIfsToken).expects()
    def tysIfsEnvironment: CallHandler[String]                     = (() => mockAppConfig.tysIfsEnv).expects()
    def tysIfsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.tysIfsEnvironmentHeaders).expects()

    // IFS Config
    def ifsBaseUrl: CallHandler[String]                         = (() => mockAppConfig.ifsBaseUrl).expects()
    def ifsToken: CallHandler[String]                           = (() => mockAppConfig.ifsToken).expects()
    def ifsEnvironment: CallHandler[String]                     = (() => mockAppConfig.ifsEnv).expects()
    def ifsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifsEnvironmentHeaders).expects()

    // API Config
    def featureSwitches: CallHandler[Configuration]                                   = (() => mockAppConfig.featureSwitches).expects()
    def apiGatewayContext: CallHandler[String]                                        = (() => mockAppConfig.apiGatewayContext).expects()
    def apiStatus(version: Version): CallHandler[String]                              = (mockAppConfig.apiStatus(_: Version)).expects(version)
    def endpointsEnabled(version: Version): CallHandler[Boolean]                      = (mockAppConfig.endpointsEnabled(_: Version)).expects(version)
    def deprecationFor(version: Version): CallHandler[Validated[String, Deprecation]] = (mockAppConfig.deprecationFor(_: Version)).expects(version)
    def apiDocumentationUrl(): CallHandler[String]                                    = (() => mockAppConfig.apiDocumentationUrl: String).expects()

    def confidenceLevelCheckEnabled: CallHandler[ConfidenceLevelConfig] =
      (() => mockAppConfig.confidenceLevelConfig).expects()

  }

}
