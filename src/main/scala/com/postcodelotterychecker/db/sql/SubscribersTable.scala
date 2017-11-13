package com.postcodelotterychecker.db.sql

import cats.Eval
import cats.effect.IO
import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.postcodelotterychecker.db.SubscriberSchema
import com.postcodelotterychecker.models.{DinnerUserName, Emoji, Postcode, Subscriber}
import com.postcodelotterychecker.servlet.ServletTypes.NotifyWhen
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


class SubscribersTable(val db: SqlDb[PostgreSQLConnection], val schema: SubscriberSchema, createNewTable: Boolean = false)(implicit ec: ExecutionContext) extends Table[PostgreSQLConnection] {

  if (createNewTable) {
    (for {
      _ <- dropTable
      newTable <- createTable
    } yield newTable).unsafeRunSync()
  }

  override def createTable: IO[QueryResult] = {
    logger.info(s"Creating Table ${schema.tableName}")
    IO.fromFuture(Eval.now(for {
      _ <- db.connectToDB
      queryResult <- db.connectionPool.sendQuery(
        s"""
           |CREATE TABLE IF NOT EXISTS
           |${schema.tableName} (
           |    ${schema.userId} varchar NOT NULL,
           |    ${schema.email} varchar NOT NULL,
           |    ${schema.notifyWhen} varchar NOT NULL,
           |    ${schema.postcodesWatching} text,
           |    ${schema.dinnerUsersWatching} text,
           |    ${schema.emojisWatching} text,
           |    ${schema.lastUpdated} timestamp NOT NULL,
           |    PRIMARY KEY(${schema.primaryKey.mkString(",")})
           |);
        """.stripMargin)
    } yield queryResult))
  }

  def insertSubscriber(subscriber: Subscriber): IO[QueryResult] = {
    val dBSubscriber = subscriber.toDB
    val statement =
      s"INSERT INTO ${schema.tableName} " +
        s"(${schema.userId}, ${schema.email}, ${schema.notifyWhen}, ${schema.postcodesWatching}, " +
        s"${schema.dinnerUsersWatching}, ${schema.emojisWatching}, ${schema.lastUpdated}) " +
        "VALUES (?,?,?,?,?,?,'now')"

    IO.fromFuture(Eval.now(db.connectionPool.sendPreparedStatement(statement,
      List(dBSubscriber.uuid,
        dBSubscriber.email,
        dBSubscriber.notifyWhen,
        dBSubscriber.postcodesWatching.asJson.noSpaces,
        dBSubscriber.dinnerUsersWatching.asJson.noSpaces,
        dBSubscriber.emojiSetsWatching.asJson.noSpaces))))

  }

  def deleteSubscriber(uuid: String): IO[QueryResult] = {
    val statement =
      s"DELETE FROM ${schema.tableName} " +
        s"WHERE ${schema.userId} = ?"

    IO.fromFuture(Eval.now(db.connectionPool.sendPreparedStatement(statement, List(uuid))))
  }

  def getSubscribers(): IO[List[Subscriber]] = {
    val query =
      s"SELECT * " +
        s"FROM ${schema.tableName}"
    IO.fromFuture(Eval.now(for {
      _ <- db.connectToDB
      queryResult <- db.connectionPool.sendPreparedStatement(query)
    } yield {
      queryResult.rows match {
        case Some(resultSet) => resultSet.map(res => {
          val userId = res(schema.userId).asInstanceOf[String]
          val email = res(schema.email).asInstanceOf[String]
          val notifyWhen = res(schema.notifyWhen).asInstanceOf[String]
          val postcodesWatching = noneIfNullString(res(schema.postcodesWatching).asInstanceOf[String])
          val dinnerUsersWatching = noneIfNullString(res(schema.dinnerUsersWatching).asInstanceOf[String])
          val emojiSetsWatching = noneIfNullString(res(schema.emojisWatching).asInstanceOf[String])


          Subscriber(
            userId,
            email,
            NotifyWhen.fromString(notifyWhen),
            parseAndDecodePostcodesWatching(postcodesWatching),
            parseAndDecodeDinnerUsers(dinnerUsersWatching),
            parseAndDecodeEmojiSetsWatching(emojiSetsWatching))
        }).toList
        case None => List.empty
      }
    }))
  }

  private def parseAndDecodePostcodesWatching(strOpt: Option[String]): Option[List[Postcode]] = {
    strOpt.flatMap { str =>
      (for {
        json <- parse(str)
        decoded <- json.as[List[String]]
        postcodesDecoded = decoded.map(Postcode)
      } yield postcodesDecoded).toOption
    }
  }

  private def parseAndDecodeDinnerUsers(strOpt: Option[String]): Option[List[DinnerUserName]] = {
    strOpt.flatMap { str =>
      (for {
        json <- parse(str)
        decoded <- json.as[List[String]]
        dinnerUsersDecoded = decoded.map(DinnerUserName)
      } yield dinnerUsersDecoded).toOption
    }
  }

  private def parseAndDecodeEmojiSetsWatching(strOpt: Option[String]): Option[List[Set[Emoji]]] = {

    strOpt.flatMap { str =>
      (for {
        json <- parse(str)
        decoded <- json.as[List[List[String]]]
        emojiSetsDecoded = decoded.map(_.map(Emoji))
      } yield emojiSetsDecoded.map(_.toSet)).toOption
    }
  }

  private def noneIfNullString(str: String): Option[String] =
    if (str == "null") None else Some(str)
}