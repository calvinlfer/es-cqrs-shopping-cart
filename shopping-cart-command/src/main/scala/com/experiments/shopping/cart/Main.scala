package com.experiments.shopping.cart

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import actors.sharding.{ ShoppingCart => ShardInfo }
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.util.Timeout
import com.experiments.shopping.cart.actors.ShoppingCart
import com.experiments.shopping.cart.domain.MemberId

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App with Repl {
  val system = ActorSystem("shopping-cart-system")
  val settings = Settings(system)
  val cluster = Cluster(system)

  val shoppingCartShard = ClusterSharding(system).start(
    typeName = ShardInfo.shardName,
    entityProps = ShoppingCart.props,
    settings = ClusterShardingSettings(system),
    extractEntityId = ShardInfo.extractEntityId,
    extractShardId = ShardInfo.extractShardId(settings.cluster.numberOfShards)
  )

  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.dispatcher

  cluster.registerOnMemberUp {
    ClusterHttpManagement(cluster).start()

    val memberId = MemberId(UUID.randomUUID())
    commandLoop(shoppingCartShard, memberId)
  }
}
