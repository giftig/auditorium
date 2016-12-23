package com.xantoria.auditorium.schedule

import scala.concurrent.duration.{Duration, FiniteDuration}

import akka.actor.Actor
import akka.stream.Materializer
import akka.stream.scaladsl._

import com.xantoria.auditorium.reporting.ElasticsearchClient

/**
 * Schedules a periodic job to ensure necessary elasticsearch indices are created ahead of time
 *
 * By default, indices are monthly. This will ensure the current and next months' indices are
 * created. Elasticsearch supports autocreating indices, but it applies permissive mappings, and
 * since we want to impose a more rigid mapping, index autocreation should be turned off in ES and
 * this mechanism will ensure indices always exist.
 *
 * Upon starting, a check will be scheduled immediately, and then further checks will be scheduled
 * at the defined interval thereafter. It's necessary to ensure that indices are created in advance
 * of rolling over to the new index, or reports are going to fail in the meantime.
 */
class ElasticsearchIndexCreator(
  client: ElasticsearchClient, interval: FiniteDuration
)(
  implicit mat: Materializer
) extends Actor {
  import context.dispatcher
  import ElasticsearchIndexCreator._

  override def preStart(): Unit = context.system.scheduler.schedule(
    Duration.Zero, interval, self, createIndices
  )

  def receive = {
    case createIndices => {
      val indices = Source(Stream(client.indexName(), client.indexName(offset = 1)))
      indices via client.createIndex runWith Sink.ignore
    }
  }
}

object ElasticsearchIndexCreator {
  val createIndices = "create_indices"
}
