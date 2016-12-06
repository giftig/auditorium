package com.xantoria.auditorium.reporting

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl._

trait ElasticsearchReporting {
  protected val fileReport: Flow[Report, (HttpRequest, Int), NotUsed] = {
    Flow.fromFunction[Report, (HttpRequest, Int)] {
      // TODO: Fill in the HttpRequest properly
      // TODO: Figure out what to do with "context"
      report: Report => HttpRequest() -> 0
    }
  }
}
