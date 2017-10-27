/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.api.testlogin.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration) extends ServicesConfig {

  private def loadConfig(key: String) = config.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = config.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "MyService"

  lazy val analyticsToken = loadConfig("google-analytics.token")
  lazy val analyticsHost = loadConfig("google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val continueUrl = loadConfig("continue-url")
}
