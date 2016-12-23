package com.xantoria.auditorium.config

import scala.concurrent.duration.{Duration, FiniteDuration}

import akka.http.scaladsl.model.Uri
import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory}

class Config {
  private val cfg: TypesafeConfig = ConfigFactory.load()
  val interface = cfg.getString("api.interface")
  val port = cfg.getInt("api.port")

  val esUri: Uri = {
    val es = cfg.getObject("elasticsearch").toConfig

    Uri(
      scheme = es.getString("scheme"),
      authority = Uri.Authority(
        host = Uri.NamedHost(es.getString("host")),
        port = es.getInt("port")
      )
    )
  }
  val esIndexPrefix = cfg.getString("elasticsearch.index_prefix")

  // The interval at which to ensure current and future indices have been created
  val createIndexInterval = Duration(
    cfg.getString("elasticsearch.create_index_interval")
  ).asInstanceOf[FiniteDuration]
}
