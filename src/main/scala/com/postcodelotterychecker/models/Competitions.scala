package com.postcodelotterychecker.models

import com.postcodelotterychecker.models.ResultTypes.ResultType

object Competitions {

  sealed trait Competition {
    val name: String
  }

  case object PostcodeCompetition extends Competition {
    override val name: String = "Postcode Lottery"
  }

  case object DinnerCompetition extends Competition {
    override val name: String = "Dinner Lottery"
  }

  case object StackpotCompetition extends Competition {
    override val name: String = "Stackpot Lottery"
  }

  case object SurveyDrawCompetition extends Competition {
    override val name: String = "Survey Draw Lottery"
  }

  case object EmojiCompetition extends Competition {
    override val name: String = "Emoji Lottery"
  }

  case object QuidcoHitterCompetition extends Competition {
    override val name: String = "Quidco Hitter"
  }
}
