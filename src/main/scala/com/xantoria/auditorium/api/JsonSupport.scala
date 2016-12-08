package com.xantoria.auditorium.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

import com.xantoria.auditorium.reporting.Report

/**
 * Spray JSON support with all the relevant structures supported
 */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit protected val reportFormat = jsonFormat(
    Report.apply,
    "id", "timestamp", "reporter", "event_type", "object_id", "summary", "user"
  )
  implicit protected val ackFormat = jsonFormat2(EventReporting.Ack.apply)
}
