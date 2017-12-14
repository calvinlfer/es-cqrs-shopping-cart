package com.experiments.shopping.cart.actors.sharding

import java.util.UUID

import akka.cluster.sharding.ShardRegion
import com.experiments.shopping.cart.actors.ShoppingCart.Command
import com.experiments.shopping.cart.domain._

object ShoppingCart {
  case class CartEnvelope(memberId: MemberId, command: Command)

  val shardName: String = "shopping-cart-shard"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case CartEnvelope(memberId, command) => (memberId.toString, command)
  }

  private def hash(id: UUID): Int = Math.abs(id.hashCode())
  private def hash(id: String): Int = Math.abs(id.hashCode())

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case CartEnvelope(memberId, _) ⇒ (hash(memberId.id) % numberOfShards).toString
    case ShardRegion.StartEntity(id) ⇒ (hash(id)        % numberOfShards).toString
  }
}
