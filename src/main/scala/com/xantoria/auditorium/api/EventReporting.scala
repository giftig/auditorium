package com.xantoria.auditorium.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

case class Pong(
  success: Boolean = true,
  message: Option[String] = None
)

trait EventReporting {
  implicit val pongFormat = jsonFormat2(Pong)

  private val pingRoute: Route = path("ping" / Segment / Slash.?) {
    s: String => get {
      complete {
        Pong(message = Some(s"Echoed: $s"))
      }
    }
  }

  protected val eventRoutes: Route = pingRoute
}
