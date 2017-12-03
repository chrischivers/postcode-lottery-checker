package com.postcodelotterychecker.local

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import fs2.Scheduler
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

object CoordinatorLocalScheduler extends App with StrictLogging {

  private def runLocalCoordinator = {
    val localCoordinator = CoordinatorLocal()
    for {
      _ <- localCoordinator.triggerCheckers()
      _ = logger.info("Trigger checkers stated")
      _ <- localCoordinator.aggregateAndProcessResults()
      _ = logger.info("Results processing complete")
    } yield ()
  }

  import java.util.Calendar

  val cal = Calendar.getInstance
  if (cal.get(Calendar.HOUR_OF_DAY) >= 17) cal.add(Calendar.DAY_OF_MONTH, 1)
  cal.set(Calendar.HOUR_OF_DAY, 17)
  cal.set(Calendar.MINUTE, 0)

  val initialDelayBeforeStart =  cal.getTimeInMillis - System.currentTimeMillis()

  logger.info(s"Sleeping for $initialDelayBeforeStart before starting")
  Thread.sleep(initialDelayBeforeStart)


  val app: fs2.Stream[IO, Unit] = for {
    scheduler <- Scheduler[IO](1)
    result <- scheduler.awakeEvery[IO](24 hours)
      .map( _ => runLocalCoordinator)
  } yield result.unsafeRunSync()

  app.run.unsafeRunSync()

}
