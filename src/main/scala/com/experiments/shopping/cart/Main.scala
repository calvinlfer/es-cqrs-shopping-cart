package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import actors.sharding.{ ShoppingCart => ShardInfo }
import akka.util.Timeout
import com.experiments.shopping.cart.actors.ShoppingCart

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App with Repl {
  val system = ActorSystem("shopping-cart-command-system")
  val settings = Settings(system)
  // TODO: use Shopping cart shard in REPL
  val shoppingCartShard = ClusterSharding(system).start(
    typeName = ShardInfo.shardName,
    entityProps = ShoppingCart.props,
    settings = ClusterShardingSettings(system),
    extractEntityId = ShardInfo.extractEntityId,
    extractShardId = ShardInfo.extractShardId(settings.cluster.numberOfShards)
  )

  val exampleShoppingCart = system.actorOf(ShoppingCart.props, "example-cart")
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.dispatcher

  commandLoop(exampleShoppingCart)
}
