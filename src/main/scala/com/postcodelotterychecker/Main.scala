package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

object Main extends App with StrictLogging {

  def start {
    val postcodeChecker = new PostcodeChecker(ConfigLoader.defaultConfig)
    postcodeChecker.startWithEmailChecker
  }

  start
}