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

package uk.gov.hmrc.api.testlogin

import uk.gov.hmrc.api.testlogin.helpers.BaseSpec
import uk.gov.hmrc.api.testlogin.pages.{ContinuePage, LoginPage}
import uk.gov.hmrc.api.testlogin.stubs.{ApiPlatformTestUserStub, ContinuePageStub}
import uk.gov.hmrc.api.testlogin.models.{AuthenticatedSession, LoginRequest, TestIndividual}
import uk.gov.hmrc.domain._

import scala.collection.JavaConverters._

class LoginSpec extends BaseSpec {

  val testUser = TestIndividual("543212311772", SaUtr("1097172564"), Nino("AA100010B"))
  val password = "validPassword"
  val authenticatedSession = AuthenticatedSession("Bearer 1234", "/auth/oid/123", "GG_TOKEN", "Individual")

  feature("User Login") {

    scenario("Successful login") {

      When("A test user")
      ApiPlatformTestUserStub.willSucceedAuthenticationWith(LoginRequest(testUser.userId, password), authenticatedSession)

      When("I login with the user's credentials")
      goOn(LoginPage)
      textField("userId").value = testUser.userId
      pwdField("password").value = password
      clickOnSubmit()

      Then("I am redirected to the continue URL")
      on(ContinuePage)

      And("The cookie is set in the session")
      val encryptedMdtpCookie = webDriver.manage().getCookies.asScala.toSet.find(_.getName == "mdtp")
      encryptedMdtpCookie should be ('defined)
    }

    scenario("Failed login") {

      When("I try to login with the wrong userId or password")
      goOn(LoginPage)
      textField("userId").value = testUser.userId
      pwdField("password").value = "wrongPassword"
      clickOnSubmit()

      Then("I am on the login page")
      on(LoginPage)

      And("An error message is displayed")
      verifyText("govuk-error-message", "Error:Invalid user ID or password. Try again.")
    }
  }

  override protected def beforeEach() = {
    super.beforeEach()
    ContinuePageStub.whenContinuePageIsUp()
    ApiPlatformTestUserStub.willFailAuthenticationByDefault()
  }
}
