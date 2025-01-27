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

package uk.gov.hmrc.api.testlogin.helpers

import java.net.URL

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}
import org.openqa.selenium.WebDriver

import scala.util.{Properties, Try}
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.chrome.ChromeOptions

trait Env {
  val driver: WebDriver = createWebDriver
  lazy val port = 6001
  lazy val createWebDriver: WebDriver = {
    Properties.propOrElse("test_driver", "chrome") match {
      case "chrome" => createChromeDriver()
      case "firefox" => createFirefoxDriver()
      case "remote-chrome" => createRemoteChromeDriver()
      case "remote-firefox" => createRemoteFirefoxDriver()
      case other => throw new IllegalArgumentException(s"target browser $other not recognised")
    }
  }

  def createRemoteChromeDriver() = {
    new RemoteWebDriver(new URL(s"http://localhost:4444/wd/hub"), DesiredCapabilities.chrome)
  }

  def createRemoteFirefoxDriver() = {
    new RemoteWebDriver(new URL(s"http://localhost:4444/wd/hub"), DesiredCapabilities.firefox)
  }

  def createChromeDriver(): WebDriver = {
    val options = new ChromeOptions()
    options.addArguments("--headless")
    new ChromeDriver(options)
  }

  def createFirefoxDriver(): WebDriver = {
    val fOpts = new FirefoxOptions().setAcceptInsecureCerts(true)
    new FirefoxDriver(fOpts)
  }

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env
