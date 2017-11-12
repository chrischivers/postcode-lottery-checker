package com.postcodelotterychecker.db.sql

import cats.Eval
import cats.effect.IO
import com.github.mauricio.async.db.{Connection, QueryResult}
import com.postcodelotterychecker.db.Schema
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

trait Table[T <: Connection] extends StrictLogging {
  val db: SqlDb[T]
  val schema: Schema

  protected def createTable: IO[QueryResult]

  def dropTable(implicit executor: ExecutionContext): IO[Unit] = {
    logger.info(s"Dropping ${schema.tableName}")
    val query = s"DROP TABLE IF EXISTS ${schema.tableName}"
    IO.fromFuture(Eval.now(db.connectionPool.sendQuery(query).map(_ => ())))
  }
}
