package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

object Main extends App with StrictLogging {

  def start {
    val postcodeChecker = new PostcodeChecker(ConfigLoader.defaultConfig)
    postcodeChecker.startWithDirectWebAddress

    val dinnerChecker = new DinnerChecker(ConfigLoader.defaultConfig)
    dinnerChecker.startWithDirectWebAddress
  }

  start
}