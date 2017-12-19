package com.experiments.shopping.cart.actors

import java.time.ZonedDateTime

import akka.actor.{ ActorLogging, Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion.Passivate
import akka.event.LoggingReceive
import akka.persistence.{ PersistentActor, SnapshotOffer }
import cats.data.NonEmptyList
import com.experiments.shopping.cart.Settings
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.domain._

object ShoppingCart {

  sealed trait Command
  final case class AddItem(item: Item) extends Command
  final case class RemoveItem(productId: ProductId) extends Command
  final case class AdjustQuantity(productId: ProductId, delta: Int) extends Command
  final case object DisplayContents extends Command
  final case object Checkout extends Command
  private final case object Shutdown extends Command

  sealed trait Response
  final case class ValidationErrors(errors: Seq[ValidationError]) extends Response
  final case class CartContents(items: List[Item], cartId: CartId) extends Response
  final case class ItemsPurchased(items: List[Item], timePurchased: ZonedDateTime, cartId: CartId) extends Response

  sealed trait Event
  final case class ItemAdded(item: Item, timeAdded: ZonedDateTime, cartId: CartId) extends Event with Response
  final case class ItemRemoved(item: Item, timeRemoved: ZonedDateTime, cartId: CartId) extends Event with Response
  final case class ItemQuantityIncreased(item: Item, amount: Int, time: ZonedDateTime, cartId: CartId)
      extends Event
      with Response
  final case class ItemQuantityDecreased(item: Item, amount: Int, time: ZonedDateTime, cartId: CartId)
      extends Event
      with Response
  private[actors] final case class ItemPurchased(item: Item, timePurchased: ZonedDateTime, cartId: CartId) extends Event

  def props: Props = Props(new ShoppingCart)
}

class ShoppingCart extends PersistentActor with ActorLogging with CartValidator with CartEventHandler {
  val settings = Settings(context.system).cart
  var cartState = CartState(cartId = CartId.generate(), items = EmptyCart)

  context.setReceiveTimeout(settings.inactivityDuration)

  def passivate(): Unit = context.parent ! Passivate(stopMessage = Shutdown)

  def updateState(event: Event): Unit = event match {
    case ItemAdded(item, _, cartId) =>
      val newItems = itemAdded(item, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemRemoved(item, _, cartId) =>
      val newItems = itemRemoved(item.productId, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemQuantityIncreased(item, amount, _, cartId) =>
      val newItems = quantityAdjusted(item.productId, amount, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemQuantityDecreased(item, amount, _, cartId) =>
      val newItems = quantityAdjusted(item.productId, -amount, cartState.items)
      cartState = CartState(cartId, newItems)

    case ItemPurchased(_, _, _) =>
      val newItems = checkedOut
      cartState = CartState(cartId = CartId.generate(), items = newItems)
  }

  def sendErrors(errors: NonEmptyList[ValidationError]): Unit =
    sender() ! ValidationErrors(errors.toList)

  def now(): ZonedDateTime = ZonedDateTime.now()

  override def persistenceId: String = s"cart-${self.path.name}"

  override def receiveRecover: Receive = {
    case e: Event =>
      updateState(e)

    case SnapshotOffer(_, state: CartState) =>
      cartState = state
  }

  override def receiveCommand: Receive = LoggingReceive {
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

    case AdjustQuantity(productId, deltaAmount) =>
      adjustQuantity(productId, deltaAmount, cartState.items).fold(sendErrors, _ => {
        val item = cartState.items(productId)
        val event =
          if (deltaAmount > 0) ItemQuantityIncreased(item, deltaAmount, now(), cartState.cartId)
          else ItemQuantityDecreased(item, math.abs(deltaAmount), now(), cartState.cartId)
        persist(event) { event =>
          updateState(event)
          sender() ! event
        }
      })

    case Checkout =>
      checkout(cartState.items).fold(sendErrors, _ => {
        val timeNow = now()
        val itemsPurchased: List[ItemPurchased] =
          cartState.items.values.toList.map(item => ItemPurchased(item, now(), cartState.cartId))
        val lastItem = itemsPurchased.last
        val oldCartItems = cartState.items.values.toList

        persistAll(itemsPurchased) { itemPurchased =>
          updateState(itemPurchased)
          // send a response on the last item
          if (itemPurchased == lastItem) {
            sender() ! ItemsPurchased(oldCartItems, now(), cartState.cartId)
            saveSnapshot(CartState.newCart())
          }
        }
      })

    case DisplayContents =>
      sender() ! CartContents(cartState.items.values.toList, cartState.cartId)

    case ReceiveTimeout =>
      log.debug("Passivating {}", persistenceId)
      passivate()

    case Shutdown =>
      context.stop(self)
  }
}
