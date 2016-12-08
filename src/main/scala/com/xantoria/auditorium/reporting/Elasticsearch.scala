package com.xantoria.auditorium.reporting

import java.util.{Calendar, GregorianCalendar}
import scala.util.Try

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import spray.json._

import com.xantoria.auditorium.api.JsonSupport

trait ElasticsearchReporting extends JsonSupport {
  protected val uri: Uri
  protected val indexPrefix: String

  // The concrete class must provide a connection pool to serve requests
  protected val connPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), HostConnectionPool]

  val fileReport: Flow[Report, (Try[HttpResponse], Int), NotUsed] = {
    Flow.fromFunction[Report, (HttpRequest, Int)] {
      report: Report => {
        val now = new GregorianCalendar()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1

        val indexName = s"$indexPrefix$year$month"
        HttpRequest(
          method = HttpMethods.POST,
          uri = uri.withPath(Uri.Path(s"/$indexName/${report.id}")),
          entity = report.toJson.compactPrint
        ) -> 0 // TODO: Figure out what to do with "context"
      }
    }
  } via connPool
}

class ElasticsearchClient(
  override protected val uri: Uri,
  override protected val indexPrefix: String
)(
  implicit system: ActorSystem,
  mat: Materializer
) extends ElasticsearchReporting {
  protected val connPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), HostConnectionPool] =
    Http().cachedHostConnectionPool[Int](uri.authority.host.toString)
}
