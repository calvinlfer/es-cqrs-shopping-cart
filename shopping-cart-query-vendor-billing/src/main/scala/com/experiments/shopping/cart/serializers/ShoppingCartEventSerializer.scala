package com.experiments.shopping.cart.serializers

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest

class ShoppingCartEventSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  val ItemAddedManifest = "ItemAdded"
  val ItemRemovedManifest = "ItemRemoved"
  val ItemQuantityIncreasedManifest = "ItemQuantityIncreased"
  val ItemQuantityDecreasedManifest = "ItemQuantityDecreased"
  val ItemsPurchasedManifest = "ItemsPurchased"

  override def identifier: Int = 911911

  override def manifest(o: AnyRef): String = ???

  override def toBinary(o: AnyRef): Array[Byte] = ???

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    import data.model.{ events => proto }
    manifest match {
      case ItemAddedManifest => proto.ItemAdded.parseFrom(bytes)
      case ItemRemovedManifest => proto.ItemRemoved.parseFrom(bytes)
      case ItemQuantityIncreasedManifest => proto.ItemQuantityIncreased.parseFrom(bytes)
      case ItemQuantityDecreasedManifest => proto.ItemQuantityDecreased.parseFrom(bytes)
      case ItemsPurchasedManifest => proto.ItemsPurchased.parseFrom(bytes)
    }
  }
}
