package com.experiments.shopping.cart

import akka.actor.ActorSystem
import com.typesafe.config.Config

class Settings(config: Config) {
  def this(system: ActorSystem) = this(system.settings.config)

  object cluster {
    val numberOfShards: Int = config.getInt("app.cart.number-of-shards")
  }
}

object Settings {
  def apply(system: ActorSystem): Settings = new Settings(system)
  def apply(config: Config): Settings = new Settings(config)
}
