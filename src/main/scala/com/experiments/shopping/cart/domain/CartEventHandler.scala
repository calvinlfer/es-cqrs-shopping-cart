package com.experiments.shopping.cart.domain

trait CartEventHandler {
  def itemAdded(item: Item, existing: CartRepr): CartRepr =
    existing + (item.productId -> item)

  def itemRemoved(productId: ProductId, existing: CartRepr): CartRepr =
    existing - productId

  def quantityAdjusted(productId: ProductId, delta: Int, existing: CartRepr): CartRepr = {
    val existingItem = existing(productId)
    val newItem = existingItem.copy(quantity = existingItem.quantity + delta)
    existing + (productId -> newItem)
  }

  def checkedOut: CartRepr = Map.empty
}
