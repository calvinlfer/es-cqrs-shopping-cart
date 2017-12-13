package com.experiments.shopping.cart
import java.util.UUID

package object domain {
  sealed trait ValidationError
  case class ItemNotInCart(productId: ProductId) extends ValidationError
  case class ItemAlreadyInCart(productId: ProductId) extends ValidationError
  case class NegativeQuantity(productId: ProductId) extends ValidationError
  case object EmptyCartCheckout extends ValidationError

  /**
    * Represents a shopping cart with items
    * The cart ID is used to find out what items were purchased together
    * @param id UUID
    */
  final class CartId(val id: UUID) extends AnyVal
  object CartId {
    def apply(id: UUID): CartId = new CartId(id)
    def generate(): CartId = CartId(UUID.randomUUID())
  }

  final class ProductId(val id: UUID) extends AnyVal
  object ProductId {
    def apply(id: UUID): ProductId = new ProductId(id)
  }

  final class VendorId(val id: UUID) extends AnyVal
  object VendorId {
    def apply(id: UUID): VendorId = new VendorId(id)
  }

  final case class Item(productId: ProductId, vendorId: VendorId, price: BigDecimal, quantity: Int)

  /**
    * Represents items in a cart for a session
    * @param cartId id of the cart (used for correlation purposes by analytics)
    * @param items items in cart indexed by product id
    */
  case class CartState(cartId: CartId, items: CartRepr)
  type CartRepr = Map[ProductId, Item]
}
