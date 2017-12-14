package com.experiments.shopping.cart

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.duration._

class Settings(config: Config) {
  def this(system: ActorSystem) = this(system.settings.config)

  private def getDuration(key: String): FiniteDuration = {
    val duration = config.getDuration(key)
    FiniteDuration(duration.toMillis, MILLISECONDS)
  }

  object cluster {
    val numberOfShards: Int = config.getInt("app.cart.number-of-shards")
  }

  object cart {
    val inactivityDuration: FiniteDuration = getDuration("app.cart.inactivity-duration")
  }
}

object Settings {
  def apply(system: ActorSystem): Settings = new Settings(system)
  def apply(config: Config): Settings = new Settings(config)
}
