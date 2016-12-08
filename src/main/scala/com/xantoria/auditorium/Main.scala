package com.xantoria.auditorium

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import org.slf4j.LoggerFactory

import com.xantoria.auditorium.api.API
import com.xantoria.auditorium.config.Config
import com.xantoria.auditorium.reporting.ElasticsearchClient

object Main {
  private val logger = LoggerFactory.getLogger("main")
  private val interface: String = "0.0.0.0"
  private val port: Int = 8000

  def main(args: Array[String]): Unit = {
    val cfg = new Config()

    logger.info("Service auditorium starting...")
    logger.info(s"Starting HTTP interface on http://${cfg.interface}:${cfg.port}...")

    implicit val system = ActorSystem("auditorium")
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val elasticsearch = new ElasticsearchClient(cfg.esUri, cfg.esIndexPrefix)
    val binding = new API(elasticsearch).bind(cfg.interface, cfg.port)

    // FIXME: Clearly this shouldn't be kept
    // Shut down after receiving a new line
    import scala.io.StdIn
    StdIn.readLine()
    binding flatMap { _.unbind() } onComplete { _ => system.terminate() }
  }
}
