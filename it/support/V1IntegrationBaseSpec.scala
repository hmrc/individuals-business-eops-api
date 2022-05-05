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

package support

trait V1IntegrationBaseSpec extends IntegrationBaseSpec {

  override def servicesConfig: Map[String, Any] = Map(
    "microservice.services.des.host" -> mockHost,
    "microservice.services.des.port" -> mockPort,
    "microservice.services.ifs.host" -> mockHost,
    "microservice.services.ifs.port" -> mockPort,
    "microservice.services.mtd-id-lookup.host" -> mockHost,
    "microservice.services.mtd-id-lookup.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.mtd-api-nrs-proxy.host" -> mockHost,
    "microservice.services.mtd-api-nrs-proxy.port" -> mockPort,
    "auditing.consumer.baseUri.port" -> mockPort,
    "feature-switch.version-1.enabled" -> "true",
    "feature-switch.version-2.enabled" -> "false"
  )
}