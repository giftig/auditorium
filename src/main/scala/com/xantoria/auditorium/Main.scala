package com.xantoria.auditorium

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.slf4j.LoggerFactory

import com.xantoria.auditorium.api.API

object Main {
  private val logger = LoggerFactory.getLogger("main")
  private val interface: String = "0.0.0.0"
  private val port: Int = 8000

  def main(args: Array[String]): Unit = {
    logger.info("com.xantoria:auditorium:0.0.1 skeleton commit")
    logger.info(s"Starting HTTP interface on http://$interface:$port...")

    implicit val system = ActorSystem("auditorium")
    implicit val ec = system.dispatcher

    val binding = new API().bind(interface, port)

    import io.StdIn
    StdIn.readLine()

    // Shut down after receiving a new line
    binding flatMap { _.unbind() } onComplete { _ => system.terminate() }
  }
}
