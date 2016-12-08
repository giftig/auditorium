package com.xantoria.auditorium.api

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}

import com.xantoria.auditorium.reporting.ElasticsearchClient

trait API extends EventReporting with JsonSupport {
  protected val esClient: ElasticsearchClient
  protected implicit val system: ActorSystem
  protected implicit val executionContext = system.dispatcher
  protected implicit val mat: Materializer

  protected val route: Route = eventRoutes

  def bind(host: String, port: Int): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, host, port)
  }
}

final class Service(override protected val esClient: ElasticsearchClient)(
  override protected implicit val system: ActorSystem,
  override protected implicit val mat: Materializer
) extends API
