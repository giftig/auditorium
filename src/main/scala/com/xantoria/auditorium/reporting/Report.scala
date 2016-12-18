package com.xantoria.auditorium.reporting

import scala.util.matching.Regex

/**
 * Represents a report of an action to be logged
 *
 * @param timestamp When the event happened, as a UTC ISO 8601 string like 2000-01-01T00:00:00Z
 * @param reporter An identifier for the service reporting the event
 * @param eventType The type of event which occurred; possible options are configurable
 * @param objectId The ID of the object on which the action was taken. This is assumed to uniquely
 *                 identify a resource in a manner which all subscribers will understand (or at
 *                 at least know whether to ignore if it's not an identifier they care about)
 * @param summary A text summary of the action which was taken, intended as a human-readable
 *                description.
 * @param user If this action can be attributed to a particular user, provide that user's ID
 */
case class Report(
  id: String,
  timestamp: String,
  reporter: String,
  eventType: String,
  objectId: String,
  summary: String,
  user: Option[String]
) {
  /**
   * A human-readable list of reasons this Report does not pass validation
   */
  lazy val configErrors: List[String] = {
    val checks: List[(Boolean, String)] = List(
      (
        timestamp match {
          case Report.timestampPattern() => true
          case _ => false
        },
        "Bad timestamp pattern"
      ),
      // TODO: Make this one of a configurable list of options (typesafe config)
      List("CREATE", "DELETE", "EDIT", "OTHER").contains(eventType) -> "Unsupported event type",
      (id.length <= Report.maxFieldLength) -> "Bad length: `id`",
      (objectId.length <= Report.maxFieldLength) -> "Bad length: `object_id`",
      (summary.length <= Report.maxFieldLength) -> "Bad length: `summary`",
      (user.map { _.length <= Report.maxFieldLength } getOrElse true) -> "Bad length: `user`"
    )

    checks flatMap {
      case (passed: Boolean, errorMsg: String) => if (passed) None else Some(errorMsg)
    }
  }
}

case class ReportWithIndex(r: Report, index: String) {
  override def toString: String = s"Report ${r.id} on index $index"
}

object Report {
  val timestampPattern: Regex = """^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$""".r
  val maxFieldLength: Int = 512 // Fairly generous general limit, mostly to allow long summaries
}
