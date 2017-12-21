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

  object database {
    val vendorBillingTableName: String = config.getString("database.vendor-query-table-name")
    val offsetTrackingTableName: String = config.getString("database.offset-tracking-table-name")
    val schemaName: String = config.getString("database.schema-name")
  }

  object querySupervision {
    val minBackOff: FiniteDuration = getDuration("app.query-supervision.min-backoff-duration")
    val maxBackOff: FiniteDuration = getDuration("app.query-supervision.max-backoff-duration")
    val noise: Double = config.getDouble("app.query-supervision.noise")
  }
}

object Settings {
  def apply(system: ActorSystem): Settings = new Settings(system)
  def apply(config: Config): Settings = new Settings(config)
}
