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

package uk.gov.hmrc.testlogin.controllers


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, anyString, refEq}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Session}
import play.api.test.FakeRequest
import uk.gov.hmrc.api.testlogin.config.AppConfig
import uk.gov.hmrc.api.testlogin.controllers.LoginController
import uk.gov.hmrc.api.testlogin.models.{LoginFailed, LoginRequest}
import uk.gov.hmrc.api.testlogin.services.{ContinueUrlService, LoginService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.failed
import uk.gov.hmrc.api.testlogin.controllers.ErrorHandler
import play.api.mvc.MessagesControllerComponents
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.api.testlogin.views.html._

class LoginControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {


  trait Setup {
    implicit val materializer = ActorMaterializer.create(ActorSystem.create())
    private val csrfAddToken = fakeApplication.injector.instanceOf[play.filters.csrf.CSRFAddToken]

    val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    val loginService: LoginService = mock[LoginService]
    val continueUrlService: ContinueUrlService = mock[ContinueUrlService]
    implicit val appConfig: AppConfig = fakeApplication.injector.instanceOf[AppConfig]
    val errorHandler: ErrorHandler = fakeApplication.injector.instanceOf[ErrorHandler]
    val mcc = fakeApplication.injector.instanceOf[MessagesControllerComponents]
    val loginView = fakeApplication.injector.instanceOf[LoginView]

    val underTest = new LoginController(loginService, errorHandler, continueUrlService, mcc, loginView)

    def execute[T <: play.api.mvc.AnyContent](action: Action[AnyContent], request: FakeRequest[T] = FakeRequest()) = await(csrfAddToken(action)(request))
  }

  "showLoginPage" should {

    "display the login page" in new Setup {

      given(continueUrlService.isValidContinueUrl(anyString())).willReturn(true)

      val result = execute(underTest.showLoginPage("/continueUrl"))

      bodyOf(result) should include("Sign in")
    }

    "return a 400 if the continue URL is invalid" in new Setup {

      given(continueUrlService.isValidContinueUrl(anyString())).willReturn(false)

      val result = execute(underTest.showLoginPage("/continueUrl"))

      status(result) shouldBe 400
    }

    "display the Create Test User link which opens a new browser-tab" in new Setup {

      given(continueUrlService.isValidContinueUrl(anyString())).willReturn(true)

      val result = execute(underTest.showLoginPage("/continueUrl"))

      bodyOf(result) should include("<a href=\"http://localhost:9680/api-test-user\" target=\"_blank\" rel=\"external\">Don't have Test User credentials</a>")
    }
  }

  "login" should {

    val loginRequest = LoginRequest("aUser", "aPassword")
    val continueUrl = "/continueUrl"
    val request = FakeRequest()
      .withFormUrlEncodedBody("userId" -> loginRequest.username, "password" -> loginRequest.password, "continue" -> continueUrl)

    "display invalid userId or password when the credentials are invalid" in new Setup {
      given(continueUrlService.isValidContinueUrl(anyString())).willReturn(true)
      given(loginService.authenticate(refEq(loginRequest))(any[HeaderCarrier](), any[ExecutionContext])).willReturn(failed(LoginFailed("")))

      val result = execute(underTest.login(), request = request)

      bodyOf(result) should include("Invalid user ID or password. Try again.")
    }

    "return a 400 when the continue URL is not valid" in new Setup {
      given(continueUrlService.isValidContinueUrl(anyString())).willReturn(false)

      val result = execute(underTest.login(), request = request)

      status(result) shouldBe 400
    }

    "redirect to the continueUrl with the session when the credentials are valid and the continue URL is valid" in new Setup {
      given(continueUrlService.isValidContinueUrl(anyString())).willReturn(true)
      val session = Session(Map("authBearerToken" -> "Bearer AUTH_TOKEN"))
      given(loginService.authenticate(refEq(loginRequest))(any[HeaderCarrier](), any[ExecutionContext])).willReturn(session)

      val result = await(underTest.login()(request))

      status(result) shouldBe 303
      result.header.headers("Location") shouldEqual continueUrl
      println(result.header.headers.keySet)
      println(result.newSession)
      result.newSession.flatMap(_.get("authBearerToken")) shouldBe Some("Bearer AUTH_TOKEN")
    }
  }
}