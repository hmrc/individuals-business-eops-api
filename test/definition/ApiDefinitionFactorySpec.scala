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

package definition

import api.mocks.MockHttpClient
import cats.implicits.catsSyntaxValidatedId
import config.{ConfidenceLevelConfig, MockAppConfig}
import config.Deprecation.NotDeprecated
import definition.APIStatus.{ALPHA, BETA}
import play.api.Configuration
import routing.{Version2, Version3}
import support.UnitSpec
import uk.gov.hmrc.auth.core.ConfidenceLevel

class ApiDefinitionFactorySpec extends UnitSpec {

  class Test extends MockHttpClient with MockAppConfig {
    val apiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)
    MockedAppConfig.apiGatewayContext returns "mtd/template"
  }

  private val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200

  "definition" when {
    "called" should {
      "return a valid Definition case class when confidence level 200 checking is enforced" in {
        testDefinitionWithConfidence(ConfidenceLevelConfig(confidenceLevel = confidenceLevel, definitionEnabled = true, authValidationEnabled = true))
      }

      "return a valid Definition case class when confidence level checking 50 is enforced" in {
        testDefinitionWithConfidence(
          ConfidenceLevelConfig(confidenceLevel = confidenceLevel, definitionEnabled = false, authValidationEnabled = false))
      }

      def testDefinitionWithConfidence(confidenceLevelConfig: ConfidenceLevelConfig): Unit = new Test {

        MockedAppConfig.featureSwitches.returns(Configuration.empty).anyNumberOfTimes()
        MockedAppConfig.confidenceLevelConfig.returns(confidenceLevelConfig).anyNumberOfTimes()

        List(Version2, Version3).foreach { version =>
          MockedAppConfig.apiStatus(version).returns("BETA")
          MockedAppConfig.endpointsEnabled(version).returns(true).anyNumberOfTimes()
          MockedAppConfig.deprecationFor(version).returns(NotDeprecated.valid).anyNumberOfTimes()
        }

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individuals Business End of Period Statement (MTD)",
              description = "This is a draft spec for the Individuals Business End of Period Statement API",
              context = "mtd/template",
              categories = Seq("INCOME_TAX_MTD"),
              versions = Seq(
                APIVersion(
                  version = Version2,
                  status = BETA,
                  endpointsEnabled = true
                ),
                APIVersion(
                  version = Version3,
                  status = BETA,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      List(
        (Version2, BETA),
        (Version3, BETA)
      ).foreach { case (version, status) =>
        s"return the correct $status for $version " in new Test {
          MockedAppConfig.apiStatus(version) returns status.toString
          MockedAppConfig
            .deprecationFor(version)
            .returns(NotDeprecated.valid)
            .anyNumberOfTimes()
          apiDefinitionFactory.buildAPIStatus(version) shouldBe status
        }
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      List(Version2, Version3).foreach { version =>
        s"default to alpha for $version " in new Test {
          MockedAppConfig.apiStatus(version) returns "ALPHO"
          MockedAppConfig
            .deprecationFor(version)
            .returns(NotDeprecated.valid)
            .anyNumberOfTimes()
          apiDefinitionFactory.buildAPIStatus(version) shouldBe ALPHA
        }
      }
    }

    "the 'deprecatedOn' parameter is missing for a deprecated version" should {
      "throw exception" in new Test {
        MockedAppConfig.apiStatus(Version3) returns "DEPRECATED"
        MockedAppConfig
          .deprecationFor(Version3)
          .returns("deprecatedOn date is required for a deprecated version".invalid)
          .anyNumberOfTimes()

        val exception: Exception = intercept[Exception] {
          apiDefinitionFactory.buildAPIStatus(Version3)
        }

        val exceptionMessage: String = exception.getMessage
        exceptionMessage shouldBe "deprecatedOn date is required for a deprecated version"
      }
    }

  }

  "confidenceLevel" when {
    Seq(
      (true, ConfidenceLevel.L250, ConfidenceLevel.L250),
      (true, ConfidenceLevel.L200, ConfidenceLevel.L200),
      (false, ConfidenceLevel.L200, ConfidenceLevel.L50)
    ).foreach { case (definitionEnabled, configCL, expectedDefinitionCL) =>
      s"confidence-level-check.definition.enabled is $definitionEnabled and confidence-level = $configCL" should {
        s"return confidence level $expectedDefinitionCL" in new Test {
          MockedAppConfig.confidenceLevelConfig returns ConfidenceLevelConfig(
            confidenceLevel = configCL,
            definitionEnabled = definitionEnabled,
            authValidationEnabled = true)
          apiDefinitionFactory.confidenceLevel shouldBe expectedDefinitionCL
        }
      }
    }
  }

}
