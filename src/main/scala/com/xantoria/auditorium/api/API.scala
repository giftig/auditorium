package com.xantoria.auditorium.api

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

final class API(implicit val system: ActorSystem) extends EventReporting {
  private implicit val executionContext = system.dispatcher
  private implicit val materializer = ActorMaterializer()

  private val route: Route = eventRoutes

  def bind(host: String, port: Int): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, host, port)
  }
}
