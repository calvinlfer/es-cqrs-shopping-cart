package com.experiments.shopping.cart.serializers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.persistence.query.{ EventEnvelope, TimeBasedUUID }
import com.experiments.shopping.cart.actors.ItemPurchasedManager.ItemDetails
import data.model.events.ItemPurchased

object Conversions {
  import data.model.{ events => proto }

  // Data -> Domain
  def domainUuid(id: proto.Id): UUID =
    UUID.fromString(id.value)

  def domainTime(timeString: String): ZonedDateTime =
    ZonedDateTime.parse(timeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  private def itemDetails(cartId: proto.Id,
                          memberId: String,
                          timePurchased: String,
                          item: data.model.events.Item): ItemDetails =
    ItemDetails(
      domainTime(timePurchased),
      item.name,
      item.price,
      item.quantity,
      domainUuid(item.productId.get),
      domainUuid(item.vendorId.get),
      domainUuid(cartId),
      memberId
    )

  type CassandraOffset = UUID
  def parseEventEnvelope: PartialFunction[EventEnvelope, (CassandraOffset, ItemDetails)] = {
    case EventEnvelope(
        TimeBasedUUID(eventOffset),
        persistenceId,
        _,
        ItemPurchased(Some(cartId), timePurchased, Some(item))
        ) =>
      (eventOffset, itemDetails(cartId, persistenceId, timePurchased, item))
  }
}
