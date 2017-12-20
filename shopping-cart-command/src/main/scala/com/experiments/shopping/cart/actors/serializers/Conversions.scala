package com.experiments.shopping.cart.actors.serializers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.domain._

object Conversions {
  import data.model.{ events => proto }
  import data.model.command.internal.{ snapshots => protoSnapshot }
  // Domain -> Data
  private def protoId(underlyingId: UUID): proto.Id =
    proto.Id(underlyingId.toString)

  private def protoTime(zdt: ZonedDateTime): String = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private def protoItem(item: Item): proto.Item =
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

  def protoItemPurchased(itemPurchased: ItemPurchased): proto.ItemPurchased =
    proto.ItemPurchased(
      cartId = Some(protoId(itemPurchased.cartId.id)),
      timePurchased = protoTime(itemPurchased.timePurchased),
      item = Some(protoItem(itemPurchased.item))
    )

  def protoCartState(cartState: CartState): protoSnapshot.CartState =
    protoSnapshot.CartState(cartId = Some(protoId(cartState.cartId.id)), items = cartState.items.map {
      case (productId, item) => (productId.id.toString, protoItem(item))
    })

  // Data -> Domain
  private def uuid(id: proto.Id): UUID = UUID.fromString(id.value)

  private def productId(id: proto.Id): ProductId = ProductId(uuid(id))

  private def vendorId(id: proto.Id): VendorId = VendorId(uuid(id))

  private def cartId(id: proto.Id): CartId = CartId(uuid(id))

  private def domainTime(timeString: String): ZonedDateTime =
    ZonedDateTime.parse(timeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private def domainItem(item: proto.Item): Item = {
    val proto.Item(Some(protoProductId), Some(protoVendorId), name, price, quantity) = item
    Item(
      productId = productId(protoProductId),
      vendorId = vendorId(protoVendorId),
      name = name,
      price = BigDecimal(price),
      quantity = quantity
    )
  }

  def domainItemAdded(itemAdded: proto.ItemAdded): ItemAdded = {
    val proto.ItemAdded(Some(protoItem), timeAdded, Some(protoCartId)) = itemAdded
    ItemAdded(item = domainItem(protoItem), timeAdded = domainTime(timeAdded), cartId = cartId(protoCartId))
  }

  def domainItemRemoved(itemRemoved: proto.ItemRemoved): ItemRemoved = {
    val proto.ItemRemoved(Some(protoItem), timeRemoved, Some(protoCartId)) = itemRemoved
    ItemRemoved(item = domainItem(protoItem), timeRemoved = domainTime(timeRemoved), cartId = cartId(protoCartId))
  }

  def domainQuantityIncreased(itemQuantityIncreased: proto.ItemQuantityIncreased): ItemQuantityIncreased = {
    val proto.ItemQuantityIncreased(Some(protoItem), amount, time, Some(protoCartId)) = itemQuantityIncreased
    ItemQuantityIncreased(
      item = domainItem(protoItem),
      amount = amount,
      time = domainTime(time),
      cartId = cartId(protoCartId)
    )
  }

  def domainQuantityDecreased(itemQuantityDecreased: proto.ItemQuantityDecreased): ItemQuantityDecreased = {
    val proto.ItemQuantityDecreased(Some(protoItem), amount, time, Some(protoCartId)) = itemQuantityDecreased
    ItemQuantityDecreased(
      item = domainItem(protoItem),
      amount = amount,
      time = domainTime(time),
      cartId = cartId(protoCartId)
    )
  }

  def domainItemPurchased(itemPurchased: proto.ItemPurchased): ItemPurchased = {
    val proto.ItemPurchased(Some(protoCartId), timePurchased, Some(protoItem)) = itemPurchased
    ItemPurchased(item = domainItem(protoItem), timePurchased = domainTime(timePurchased), cartId = cartId(protoCartId))
  }

  def domainCartState(cartState: protoSnapshot.CartState): CartState = {
    val protoSnapshot.CartState(Some(protoCartId), protoItems) = cartState
    CartState(cartId = cartId(protoCartId), items = protoItems.map {
      case (productIdStr, protoItem) => (ProductId(UUID.fromString(productIdStr)), domainItem(protoItem))
    })
  }
}
