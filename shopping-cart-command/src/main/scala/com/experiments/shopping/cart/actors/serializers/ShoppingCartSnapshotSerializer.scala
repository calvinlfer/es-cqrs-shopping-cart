package com.experiments.shopping.cart.actors.serializers

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.experiments.shopping.cart.domain.CartState

class ShoppingCartSnapshotSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import Conversions._

  val CartSnapshotManifest = "CartState"

  override def identifier: Int = 911912

  override def manifest(o: AnyRef): String = o match {
    case _: CartState => CartSnapshotManifest
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case c: CartState => protoCartState(c).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    import data.model.command.internal.{ snapshots => proto }
    manifest match {
      case CartSnapshotManifest => domainCartState(proto.CartState.parseFrom(bytes))
    }
  }
}
