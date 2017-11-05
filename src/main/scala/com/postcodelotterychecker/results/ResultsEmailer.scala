package com.postcodelotterychecker.results

import com.postcodelotterychecker.models.Results.SubscriberResults
import com.postcodelotterychecker.models.Subscriber

trait ResultsEmailer {

  val subscriberResults: Map[Subscriber, SubscriberResults]
  val emailClient: EmailClient



}
