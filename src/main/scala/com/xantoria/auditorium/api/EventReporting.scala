package com.xantoria.auditorium.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import com.xantoria.auditorium.reporting._

trait EventReporting {
  import EventReporting._

  implicit val reportFormat = jsonFormat(
    Report.apply,
    "id", "timestamp", "reporter", "event_type", "object_id", "summary", "user"
  )
  implicit val ackFormat = jsonFormat2(Ack.apply)

  private val reportEvent: Route = path("report" ~ Slash.?) {
    post {
      entity(as[Report]) {
        report: Report => {
          val resp: (Int, Ack) = {
            val errors: List[String] = report.configErrors

            if (errors.isEmpty) {
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

  protected val eventRoutes: Route = reportEvent
}

object EventReporting {
  case class Ack(success: Boolean, error: Option[String])

  object Ack {
    def success: Ack = Ack(success = true, error = None)
    def error(msg: String): Ack = Ack(success = false, error = Some(msg))
  }
}
