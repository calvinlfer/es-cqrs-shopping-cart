package com.experiments.shopping.cart.actors.serializers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.domain._
import com.google.protobuf.ByteString

class ShoppingCartEventSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {
  val ItemAddedManifest = "ItemAdded"
  val ItemRemovedManifest = "ItemRemoved"
  val ItemQuantityIncreasedManifest = "ItemQuantityIncreased"
  val ItemQuantityDecreasedManifest = "ItemQuantityDecreased"
  val ItemsPurchasedManifest = "ItemsPurchased"

  object conversions {
    import data.model.{ events => proto }
    // Domain -> Data
    def protoId(underlyingId: UUID): proto.Id =
      proto.Id(ByteString.copyFrom(underlyingId.toString.getBytes()))

    def protoTime(zdt: ZonedDateTime): String = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    def protoItem(item: Item): proto.Item =
      proto.Item(
        productId = Some(protoId(item.productId.id)),
        vendorId = Some(protoId(item.vendorId.id)),
        name = item.name,
        price = item.price.doubleValue(),
        quantity = item.quantity
      )

    def protoItemAdded(itemAdded: ItemAdded): proto.ItemAdded =
      proto.ItemAdded(
        item = Some(protoItem(itemAdded.item)),
        timeAdded = protoTime(itemAdded.timeAdded),
        cartId = Some(protoId(itemAdded.cartId.id))
      )

    def protoItemRemoved(itemRemoved: ItemRemoved): proto.ItemRemoved =
      proto.ItemRemoved(
        item = Some(protoItem(itemRemoved.item)),
        timeRemoved = protoTime(itemRemoved.timeRemoved),
        cartId = Some(protoId(itemRemoved.cartId.id))
      )

    def protoItemQuantityIncreased(quantityIncreased: ItemQuantityIncreased): proto.ItemQuantityIncreased =
      proto.ItemQuantityIncreased(
        item = Some(protoItem(quantityIncreased.item)),
        amount = quantityIncreased.amount,
        time = protoTime(quantityIncreased.time),
        cartId = Some(protoId(quantityIncreased.cartId.id))
      )

    def protoItemQuantityDecreased(quantityDecreased: ItemQuantityDecreased): proto.ItemQuantityDecreased =
      proto.ItemQuantityDecreased(
        item = Some(protoItem(quantityDecreased.item)),
        amount = quantityDecreased.amount,
        time = protoTime(quantityDecreased.time),
        cartId = Some(protoId(quantityDecreased.cartId.id))
      )

    def protoItemsPurchased(itemsPurchased: ItemsPurchased): proto.ItemsPurchased =
      proto.ItemsPurchased(
        cartId = Some(protoId(itemsPurchased.cartId.id)),
        timePurchased = protoTime(itemsPurchased.timePurchased),
        items = itemsPurchased.items.map(protoItem)
      )

    // Data -> Domain
    private def uuid(id: proto.Id): UUID = UUID.nameUUIDFromBytes(id.value.toByteArray)

    def productId(id: proto.Id): ProductId = ProductId(uuid(id))

    def vendorId(id: proto.Id): VendorId = VendorId(uuid(id))

    def cartId(id: proto.Id): CartId = CartId(uuid(id))

    def dataItem(item: proto.Item): Item = ???

    def dataItemAdded(itemAdded: proto.ItemAdded): ItemAdded = ???
  }

  override def identifier: Int = 911911

  override def manifest(o: AnyRef): String = o match {
    case _: ItemAdded => ItemAddedManifest
    case _: ItemRemoved => ItemRemovedManifest
    case _: ItemQuantityIncreased => ItemQuantityIncreasedManifest
    case _: ItemQuantityDecreased => ItemQuantityDecreasedManifest
    case _: ItemsPurchased => ItemsPurchasedManifest
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    import conversions._
    o match {
      case i: ItemAdded => protoItemAdded(i).toByteArray
      case i: ItemRemoved => protoItemRemoved(i).toByteArray
      case i: ItemQuantityIncreased => protoItemQuantityIncreased(i).toByteArray
      case i: ItemQuantityDecreased => protoItemQuantityDecreased(i).toByteArray
      case i: ItemsPurchased => protoItemsPurchased(i).toByteArray
    }
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    import conversions._
    import data.model.{ events => proto }
    manifest match {
      case ItemAddedManifest =>
        proto.ItemAdded.parseFrom(bytes)
    }
  }
}
