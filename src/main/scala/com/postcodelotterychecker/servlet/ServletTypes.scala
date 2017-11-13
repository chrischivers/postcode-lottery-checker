package com.postcodelotterychecker.servlet

object ServletTypes {

  case class JsonResponse(`type`: String, message: String)

  sealed trait NotifyWhen {
    val value: String
    override def toString: String = value
  }

  case object EveryDay extends NotifyWhen {
    override val value: String = "EVERY_DAY"
  }

  case object OnlyWhenWon extends NotifyWhen {
    override val value: String = "ONLY_WHEN_WON"
  }

  object NotifyWhen {
    def fromString(str: String) = str match {
      case "EVERY_DAY" => EveryDay
      case "ONLY_WHEN_WON" => OnlyWhenWon
      case other => throw new RuntimeException(s"Unknown 'NotifyWhen' type [$other]")
    }
  }

}
