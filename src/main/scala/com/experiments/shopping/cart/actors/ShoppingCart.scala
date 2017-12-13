package com.experiments.shopping.cart.actors

import java.time.ZonedDateTime

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import cats.data.NonEmptyList
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.domain._

object ShoppingCart {

  sealed trait Command
  final case class AddItem(item: Item) extends Command
  final case class RemoveItem(productId: ProductId) extends Command
  final case class AdjustQuantity(productId: ProductId, delta: Int) extends Command
  final case object Checkout extends Command

  sealed trait Response
  case class ValidationErrors(errors: Seq[ValidationError]) extends Response

  sealed trait Event
  final case class ItemAdded(item: Item, timeAdded: ZonedDateTime, cartId: CartId) extends Event with Response
  final case class ItemRemoved(item: Item, timeRemoved: ZonedDateTime, cartId: CartId) extends Event with Response
  final case class ItemQuantityIncreased(item: Item, delta: Int, cartId: CartId) extends Event with Response
  final case class ItemQuantityDecreased(item: Item, delta: Int, cartId: CartId) extends Event with Response
  final case class ItemsPurchased(items: List[Item], timePurchased: ZonedDateTime, cartId: CartId)
      extends Event
      with Response
}

class ShoppingCart extends PersistentActor with ActorLogging with CartValidator with CartEventHandler {
  var cartState = CartState(cartId = CartId.generate(), items = Map.empty)

  def updateState(event: Event): Unit = event match {
    case ItemAdded(item, _, cartId) =>
      val newItems = itemAdded(item, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemRemoved(item, _, cartId) =>
      val newItems = itemRemoved(item.productId, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemQuantityIncreased(item, delta, cartId) =>
      val newItems = quantityAdjusted(item.productId, delta, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemQuantityDecreased(item, delta, cartId) =>
      val newItems = quantityAdjusted(item.productId, -delta, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemsPurchased(_, _, _) =>
      val newItems = checkedOut
      cartState = CartState(cartId = CartId.generate(), items = newItems)
  }

  def sendErrors(errors: NonEmptyList[ValidationError]): Unit =
    sender() ! ValidationErrors(errors.toList)

  def now(): ZonedDateTime = ZonedDateTime.now()

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case AddItem(item) =>
      addItem(item, cartState.items).fold(
        sendErrors,
        _ =>
          persist(ItemAdded(item, now(), cartState.cartId)) { event =>
            updateState(event)
            sender() ! event
        }
      )

    case RemoveItem(productId) =>
      removeItem(productId, cartState.items).fold(sendErrors, _ => {
        val item = cartState.items(productId)
        persist(ItemRemoved(item, now(), cartState.cartId)) { event =>
          updateState(event)
          sender() ! event
        }
      })

    // TODO: continue with the rest of the Commands
    // TODO: implement snapshotting which gives the cart a new ID to represent a new session
  }

  override def persistenceId: String = s"cart-${self.path.name}"
}
