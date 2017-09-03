package com.postcodelotterychecker.utils

import com.postcodelotterychecker.Postcode
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

object Utils extends StrictLogging {

  // Code adapted from http://stackoverflow.com/questions/7930814/whats-the-scala-way-to-implement-a-retry-able-call-like-this-one
  def retry[T](totalNumberOfAttempts: Int, secondsBetweenAttempts: Int)(fn: => T): T = {

    @annotation.tailrec
    def retryHelper(n: Int)(fn: => T): T = {
      Try {
        logger.info(s"attempting to run function. Attempt $n of $totalNumberOfAttempts")
        fn
      } match {
        case Success(x) => x
        case Failure(e) if n < totalNumberOfAttempts => {
          logger.error("Error occured during execution", e)
          logger.info(s"Attempt ${n + 1} of $totalNumberOfAttempts failed. Retrying operation after $secondsBetweenAttempts seconds.")
          Thread.sleep(secondsBetweenAttempts * 1000)
          retryHelper(n + 1)(fn)
        }
        case Failure(e) => throw e
      }
    }
    retryHelper(n = 0)(fn)
  }

  def validatePostcodeAgainstRegex(postcode: Postcode): Boolean = {
    val postcodeRegEx = "^([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([AZa-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z])))) [0-9][A-Za-z]{2})$".r
    postcodeRegEx.findFirstIn(postcode.value).isDefined
  }
}
