package com.experiments.shopping.cart

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

class Settings(config: Config) {
  def this(system: ActorSystem) = this(system.settings.config)

  private def getDuration(key: String): FiniteDuration = {
    val duration = config.getDuration(key)
    FiniteDuration(duration.toMillis, MILLISECONDS)
  }

  object cassandra {
    val host: String = config.getString("cassandra.host")
    val port: Int = config.getInt("cassandra.port")
    val keyspace: String = config.getString("cassandra.keyspace")
    val trustStorePath: String = config.getString("cassandra.truststore-path")
    val trustStorePass: String = config.getString("cassandra.truststore-password")
    val username: Option[String] = {
      val user = config.getString("cassandra.username")
      if (user.nonEmpty) Some(user) else None
    }
    val password: Option[String] = {
      val pass = config.getString("cassandra.password")
      if (pass.nonEmpty) Some(pass) else None
    }
    val autoInitKeyspace: Boolean = config.getBoolean("cassandra.initialize-keyspace")
  }

  object kafka {
    val uris: String = config.getString("kafka.uris")
    val topic: String = config.getString("kafka.topic")
  }
}

object Settings {
  def apply(system: ActorSystem): Settings = new Settings(system)
  def apply(config: Config): Settings = new Settings(config)
}
