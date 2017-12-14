package com.experiments.shopping.cart.actors.event.adapters

import akka.actor.ExtendedActorSystem
import akka.event.Logging
import akka.persistence.journal.{ Tagged, WriteEventAdapter }
import com.experiments.shopping.cart.actors.ShoppingCart._

/**
  * Tagging Event Adapter that is responsible for tagging each Shopping Cart event before it is persisted to the journal
  * in order to support different types of eventsByTag queries to support various read-sides
  *
  * @param system is the ActorSystem needed for logging
  */
class ShoppingCartTaggingEventAdapter(system: ExtendedActorSystem) extends WriteEventAdapter {
  val ItemAdded = "item-added"
  val ItemQuantityIncreased = "item-quantity-increased"
  val ItemQuantityDecreased = "item-quantity-decreased"
  val ItemsPurchased = "items-purchased"
  val ItemRemoved = "item-removed"
  val GeneralCart = "cart-event"

  private val log = Logging.getLogger(system, this)

  // No manifest needed
  override def manifest(event: Any): String = ""

  // Events coming from the Persistent Actor to the Event Journal
  override def toJournal(event: Any): Any = event match {
    case i: ItemAdded =>
      Tagged(i, Set(ItemAdded, GeneralCart))

    case i: ItemRemoved =>
      Tagged(i, Set(ItemRemoved, GeneralCart))

    case i: ItemQuantityIncreased =>
      Tagged(i, Set(ItemQuantityIncreased, GeneralCart))

    case i: ItemQuantityDecreased =>
      Tagged(i, Set(ItemQuantityDecreased, GeneralCart))

    case i: ItemsPurchased =>
      Tagged(i, Set(ItemsPurchased, GeneralCart))

    case otherEvent =>
      log.warning(s"Message: $otherEvent is being persisted to the event journal is not tagged")
      otherEvent
  }
}
