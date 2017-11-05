package com.postcodelotterychecker.checkers

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.postcodelotterychecker.{CheckerConfig, HtmlUnitWebClient}
import com.postcodelotterychecker.checkers.CheckerRequestHandler.{Request, Response}
import com.postcodelotterychecker.checkers.DinnerChecker.config
import com.typesafe.scalalogging.StrictLogging

import scala.beans.BeanProperty

trait CheckerRequestHandler[A] extends RequestHandler[Request, Response] with StrictLogging {

  val config: CheckerConfig
  val htmlUnitWebClient: HtmlUnitWebClient

  override def handleRequest(input: CheckerRequestHandler.Request, context: Context) = {

    (for {
      result <- getResult
      _ <- sendResult(result)
    } yield Response(true)).unsafeRunSync()
  }

  def getResult: IO[A]

  def sendResult(result: A): IO[Unit]

  def generateWebAddress = {
    config.directWebAddressPrefix + config.directWebAddressSuffix + config.uuid
  }
}

object CheckerRequestHandler {

  case class Request(@BeanProperty var uuid: String){
    def this() = this(uuid = "")
  }

  case class Response(@BeanProperty var success: Boolean){
    def this() = this(success = false)
  }
}
