package com.xantoria.auditorium.api

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}

import com.xantoria.auditorium.reporting.ElasticsearchClient

final class API(private val esClient: ElasticsearchClient)(
  private implicit val system: ActorSystem,
  private implicit val mat: Materializer
) extends EventReporting with JsonSupport {
  private implicit val executionContext = system.dispatcher

  private val route: Route = eventRoutes

  def bind(host: String, port: Int): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, host, port)
  }
}
