package com.xantoria.auditorium.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl._

import com.xantoria.auditorium.reporting._

trait EventReporting {
  this: API =>

  import EventReporting._

  private val reportEventRoute: Route = path("report" ~ Slash.?) {
    post {
      entity(as[Report]) {
        report: Report => {
          val resp: (Int, Ack) = {
            val errors: List[String] = report.configErrors

            if (errors.isEmpty) {
              Source.single(report).via(esClient.fileReport).runWith(Sink.ignore)
              202 -> Ack.success
            } else {
              400 -> Ack.error(s"Validation errors: ${errors.mkString(", ")}")
            }
          }
          complete(resp)
        }
      }
    }
  }

  protected val eventRoutes: Route = reportEventRoute
}

object EventReporting {
  case class Ack(success: Boolean, error: Option[String])

  object Ack {
    def success: Ack = Ack(success = true, error = None)
    def error(msg: String): Ack = Ack(success = false, error = Some(msg))
  }
}
