package com.postcodelotterychecker

import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.Future

case class Postcode(value: String)
case class DinnerUserName(value: String)
case class User(email: String, postCodesWatching: Option[List[Postcode]], dinnerUsersWatching: Option[List[DinnerUserName]])

trait Checker[A] {

  val todaysDate: String = new SimpleDateFormat("dd/MM/yyyy").format(new Date())

  def run: Future[Map[User, Option[Boolean]]]

  def getWinningResult(webAddress: String): A

}
