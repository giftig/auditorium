package com.xantoria.auditorium.reporting

import java.io.FileNotFoundException
import java.util.{Calendar, GregorianCalendar}
import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
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
  protected val reportConnPool: Flow[
    (HttpRequest, ReportWithIndex),
    (Try[HttpResponse], ReportWithIndex),
    HostConnectionPool
  ]
  protected val indexConnPool: Flow[
    (HttpRequest, String),
    (Try[HttpResponse], String),
    HostConnectionPool
  ]


  /**
   * Handle a response from elasticsearch by logging any errors which occur
   */
  private def recordResponse[T](r: (Try[HttpResponse], T)): (Try[HttpResponse], T) = {
    r._1 match {
      case Failure(t: Throwable) => logger.error(
        s"Elasticsearch request failed: $t; context: ${r._2}"
      )
      case Success(resp: HttpResponse) if resp.status.isFailure => logger.error(
        s"Elasticsearch request failed: status ${resp.status.intValue}; context: ${r._2}"
      )
    }
    r
  }

  private lazy val attachIndex: Flow[Report, ReportWithIndex, NotUsed] = Flow.fromFunction {
    val now = new GregorianCalendar()
    val year = now.get(Calendar.YEAR)
    val month = now.get(Calendar.MONTH) + 1
    ReportWithIndex(_, s"$indexPrefix$year$month")
  }

  private lazy val fileIndexedReport: Flow[
    ReportWithIndex, (Try[HttpResponse], ReportWithIndex), NotUsed
  ] = {
    Flow.fromFunction[ReportWithIndex, (HttpRequest, ReportWithIndex)] {
      indexedReport: ReportWithIndex => {
        val (report, indexName) = ReportWithIndex.unapply(indexedReport).get

        logger.info(s"Filing report ${report.id} in elasticsearch (index ${indexName})")

        HttpRequest(
          method = HttpMethods.POST,
          uri = uri.withPath(Uri.Path(s"/$indexName/${Elasticsearch.DOC_TYPE}/${report.id}")),
          entity = report.toJson.compactPrint
        ) -> indexedReport
      }
    }
  } via reportConnPool map recordResponse[ReportWithIndex]

  lazy val fileReport: Flow[Report, (Try[HttpResponse], ReportWithIndex), NotUsed] = {
    attachIndex via fileIndexedReport
  }

  lazy val createIndex: Flow[String, (Try[HttpResponse], String), NotUsed] = {
    Flow.fromFunction[String, (HttpRequest, String)] {
      indexName: String => {
        logger.info(s"Creating index $indexName in elasticsearch (default index definition)")

        HttpRequest(
          method = HttpMethods.POST,
          uri = uri.withPath(Uri.Path(s"/$indexName")),
          entity = Elasticsearch.indexDefinition
        ) -> indexName
      }
    }
  } via indexConnPool map recordResponse[String]
}

class ElasticsearchClient(
  override protected val uri: Uri,
  override protected val indexPrefix: String
)(
  implicit system: ActorSystem,
  mat: Materializer
) extends ElasticsearchReporting {
  protected val reportConnPool: Flow[
    (HttpRequest, ReportWithIndex),
    (Try[HttpResponse], ReportWithIndex),
    HostConnectionPool
  ] = Http().cachedHostConnectionPool[ReportWithIndex](
    host = uri.authority.host.toString,
    port = uri.authority.port
  )

  protected val indexConnPool: Flow[
    (HttpRequest, String),
    (Try[HttpResponse], String),
    HostConnectionPool
  ] = Http().cachedHostConnectionPool[String](
    host = uri.authority.host.toString,
    port = uri.authority.port
  )
}

object Elasticsearch {
  final val DOC_TYPE: String = "report"

  val indexDefinition: String = {
    val f = "/index-definition.json"
    Option(getClass.getResourceAsStream(f)) map {
      io.Source.fromInputStream(_).getLines.mkString
    } getOrElse { throw new FileNotFoundException(f) }
  }
}
