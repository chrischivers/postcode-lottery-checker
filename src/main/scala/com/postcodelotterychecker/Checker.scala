package com.postcodelotterychecker

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.scalalogging.StrictLogging

import scala.beans.BeanProperty
import scala.concurrent.Future

case class Postcode(value: String)
case class DinnerUserName(value: String)
case class User(email: String, postCodesWatching: Option[List[Postcode]], dinnerUsersWatching: Option[List[DinnerUserName]], emojiSetsWatching: Option[List[Set[Emoji]]])
case class Emoji(id: String)


case class Request(@BeanProperty var uuid: String){
  def this() = this(uuid = "")
}

case class Response(@BeanProperty var success: Boolean){
  def this() = this(success = false)
}

trait Checker[A] extends StrictLogging {

  val htmlUnitWebClient = new HtmlUnitWebClient

  type UserResults = Map[User, Option[Boolean]]

  val todaysDate: String = new SimpleDateFormat("dd/MM/yyyy").format(new Date())

  def run: Future[(UserResults, A)]

  def getWinningResult(webAddress: String): A

}
