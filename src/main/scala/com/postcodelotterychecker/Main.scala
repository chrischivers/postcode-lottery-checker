package com.postcodelotterychecker

import com.typesafe.scalalogging.StrictLogging

object Main extends App with StrictLogging {

  val postcodeChecker = new PostcodeChecker(ConfigLoader.defaultConfig)
  postcodeChecker.startWithDirectWebAddress

}