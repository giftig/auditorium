package com.xantoria.auditorium.reporting

import java.util.{Calendar, GregorianCalendar}
import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import org.slf4j.LoggerFactory
import spray.json._

import com.xantoria.auditorium.api.JsonSupport

trait ElasticsearchReporting extends JsonSupport {
  protected val uri: Uri
  protected val indexPrefix: String

  private val logger = LoggerFactory.getLogger(classOf[ElasticsearchReporting])

  // The concrete class must provide a connection pool to serve requests
  protected val connPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), HostConnectionPool]

  lazy val fileReport: Flow[Report, (Try[HttpResponse], Int), NotUsed] = {
    Flow.fromFunction[Report, (HttpRequest, Int)] {
      report: Report => {
        val now = new GregorianCalendar()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1

        val indexName = s"$indexPrefix$year$month"

        logger.info(s"Filing report ${report.id} in elasticsearch (index ${indexName})")

        val finalUri = uri.withPath(Uri.Path(s"/$indexName/${report.id}"))
        logger.debug(s"Hitting URI $finalUri")
        HttpRequest(
          method = HttpMethods.POST,
          uri = finalUri,
          entity = report.toJson.compactPrint
        ) -> 0 // TODO: Figure out what to do with "context"
      }
    }
  } via connPool map {
    resp: (Try[HttpResponse], Int) => {
      resp._1 match {
        case Failure(t: Throwable) => logger.error(s"Elasticsearch request failed: $t")
        case _ => ()
      }
      resp
    }
  }
}

class ElasticsearchClient(
  override protected val uri: Uri,
  override protected val indexPrefix: String
)(
  implicit system: ActorSystem,
  mat: Materializer
) extends ElasticsearchReporting {
  protected val connPool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), HostConnectionPool] =
    Http().cachedHostConnectionPool[Int](
      host = uri.authority.host.toString,
      port = uri.authority.port
    )
}
