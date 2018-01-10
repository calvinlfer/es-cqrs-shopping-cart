package com.experiments.shopping.cart

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import actors.sharding.{ ShoppingCart => ShardInfo }
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.experiments.shopping.cart.actors.ShoppingCart
import com.experiments.shopping.cart.domain.MemberId

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object Main extends App with Repl with Rest {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  val settings = Settings(system)
  val cluster = Cluster(system)
  val log = system.log

  val shoppingCartShard = ClusterSharding(system).start(
    typeName = ShardInfo.shardName,
    entityProps = ShoppingCart.props,
    settings = ClusterShardingSettings(system),
    extractEntityId = ShardInfo.extractEntityId,
    extractShardId = ShardInfo.extractShardId(settings.cluster.numberOfShards)
  )

  override val cartRouter: ActorRef = shoppingCartShard
  override implicit val timeout: Timeout = Timeout(5.seconds)
  override implicit val ec: ExecutionContext = system.dispatcher

  cluster.registerOnMemberUp {
    ClusterHttpManagement(cluster).start()

    // start REST server
    Http()
      .bindAndHandle(routes, settings.server.host, settings.server.port)
      .onComplete {
        case Success(binding) =>
          log.info("Server online at http://{}:{}", binding.localAddress.getHostName, binding.localAddress.getPort)
        case Failure(ex) =>
          log.error(ex, "Failed to start server. Shutting down actor system")
          system.terminate()
      }

    val memberId = MemberId(UUID.randomUUID())
    commandLoop(shoppingCartShard, memberId)
  }
}
