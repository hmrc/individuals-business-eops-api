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

import config.ConfidenceLevelConfig
import definition.APIStatus.{ ALPHA, BETA }
import definition.Versions.{ VERSION_1, VERSION_2 }
import mocks.MockAppConfig
import play.api.Configuration
import support.UnitSpec
import uk.gov.hmrc.auth.core.ConfidenceLevel
import v1.mocks.MockHttpClient

class ApiDefinitionFactorySpec extends UnitSpec {

  class Test extends MockHttpClient with MockAppConfig {
    val apiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)
    MockAppConfig.apiGatewayContext returns "mtd/template"
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
        MockAppConfig.featureSwitches.returns(Configuration.empty).anyNumberOfTimes()
        MockAppConfig.apiStatus("1.0").returns("BETA")
        MockAppConfig.apiStatus("2.0").returns("ALPHA")
        MockAppConfig.endpointsEnabled("1.0").returns(true).anyNumberOfTimes()
        MockAppConfig.endpointsEnabled("2.0").returns(true).anyNumberOfTimes()
        MockAppConfig.confidenceLevelCheckEnabled.returns(confidenceLevelConfig).anyNumberOfTimes()

        val readScope: String                = "read:self-assessment"
        val writeScope: String               = "write:self-assessment"
        val confidenceLevel: ConfidenceLevel = if (confidenceLevelConfig.authValidationEnabled) ConfidenceLevel.L200 else ConfidenceLevel.L50

        apiDefinitionFactory.definition shouldBe
          Definition(
            scopes = Seq(
              Scope(
                key = readScope,
                name = "View your Self Assessment information",
                description = "Allow read access to self assessment data",
                confidenceLevel
              ),
              Scope(
                key = writeScope,
                name = "Change your Self Assessment information",
                description = "Allow write access to self assessment data",
                confidenceLevel
              )
            ),
            api = APIDefinition(
              name = "Individuals Business End of Period Statement (MTD)",
              description = "This is a draft spec for the Individuals Business End of Period Statement API",
              context = "mtd/template",
              categories = Seq("INCOME_TAX_MTD"),
              versions = Seq(
                APIVersion(
                  version = VERSION_1,
                  status = BETA,
                  endpointsEnabled = true
                ),
                APIVersion(
                  version = VERSION_2,
                  status = ALPHA,
                  endpointsEnabled = true
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "confidenceLevel" when {
    Seq(
      (true, ConfidenceLevel.L250, ConfidenceLevel.L250),
      (true, ConfidenceLevel.L200, ConfidenceLevel.L200),
      (false, ConfidenceLevel.L200, ConfidenceLevel.L50)
    ).foreach {
      case (definitionEnabled, configCL, expectedDefinitionCL) =>
        s"confidence-level-check.definition.enabled is $definitionEnabled and confidence-level = $configCL" should {
          s"return confidence level $expectedDefinitionCL" in new Test {
            MockAppConfig.confidenceLevelCheckEnabled returns ConfidenceLevelConfig(confidenceLevel = configCL,
                                                                                    definitionEnabled = definitionEnabled,
                                                                                    authValidationEnabled = true)
            apiDefinitionFactory.confidenceLevel shouldBe expectedDefinitionCL
          }
        }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the correct status" in new Test {
        MockAppConfig.apiStatus("1.0") returns "BETA"
        apiDefinitionFactory.buildAPIStatus("1.0") shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      "default to alpha" in new Test {
        MockAppConfig.apiStatus("1.0") returns "ALPHO"
        apiDefinitionFactory.buildAPIStatus("1.0") shouldBe ALPHA
      }
    }
  }
}
