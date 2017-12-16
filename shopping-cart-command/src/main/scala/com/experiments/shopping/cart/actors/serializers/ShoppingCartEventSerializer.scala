package com.experiments.shopping.cart.actors.serializers

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.experiments.shopping.cart.actors.ShoppingCart._

class ShoppingCartEventSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  import Conversions._

  val ItemAddedManifest = "ItemAdded"
  val ItemRemovedManifest = "ItemRemoved"
  val ItemQuantityIncreasedManifest = "ItemQuantityIncreased"
  val ItemQuantityDecreasedManifest = "ItemQuantityDecreased"
  val ItemsPurchasedManifest = "ItemsPurchased"

  override def identifier: Int = 911911

  override def manifest(o: AnyRef): String = o match {
    case _: ItemAdded => ItemAddedManifest
    case _: ItemRemoved => ItemRemovedManifest
    case _: ItemQuantityIncreased => ItemQuantityIncreasedManifest
    case _: ItemQuantityDecreased => ItemQuantityDecreasedManifest
    case _: ItemsPurchased => ItemsPurchasedManifest
  }

  override def toBinary(o: AnyRef): Array[Byte] =
    o match {
      case i: ItemAdded => protoItemAdded(i).toByteArray
      case i: ItemRemoved => protoItemRemoved(i).toByteArray
      case i: ItemQuantityIncreased => protoItemQuantityIncreased(i).toByteArray
      case i: ItemQuantityDecreased => protoItemQuantityDecreased(i).toByteArray
      case i: ItemsPurchased => protoItemsPurchased(i).toByteArray
    }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    import data.model.{ events => proto }
    manifest match {
      case ItemAddedManifest => domainItemAdded(proto.ItemAdded.parseFrom(bytes))
      case ItemRemovedManifest => domainItemRemoved(proto.ItemRemoved.parseFrom(bytes))
      case ItemQuantityIncreasedManifest => domainQuantityIncreased(proto.ItemQuantityIncreased.parseFrom(bytes))
      case ItemQuantityDecreasedManifest => domainQuantityDecreased(proto.ItemQuantityDecreased.parseFrom(bytes))
      case ItemsPurchasedManifest => domainItemsPurchased(proto.ItemsPurchased.parseFrom(bytes))
    }
  }
}
