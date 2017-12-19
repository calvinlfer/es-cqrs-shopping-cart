package com.experiments.shopping.cart.serializers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.persistence.query.{ EventEnvelope, TimeBasedUUID }
import com.experiments.shopping.cart.actors.VendorBilling.ItemData
import data.model.events.ItemPurchased

object Conversions {
  import data.model.{ events => proto }

  // Data -> Domain
  def domainUuid(id: proto.Id): UUID =
    UUID.fromString(id.value)

  def domainTime(timeString: String): ZonedDateTime =
    ZonedDateTime.parse(timeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  type CassandraOffset = UUID
  def parseEventEnvelope(ee: EventEnvelope): (CassandraOffset, ItemData) = {
    import data.model.{ events => proto }
    val EventEnvelope(
      TimeBasedUUID(offsetUUID),
      _,
      _,
      ItemPurchased(Some(_), protoTimePurchased, Some(proto.Item(Some(_), Some(protoVendorId), _, price, quantity)))
    ) = ee
    val time = domainTime(protoTimePurchased)
    val vendorId = domainUuid(protoVendorId)
    val totalPrice = price * quantity
    val year = time.getYear
    val month = time.getMonthValue
    (offsetUUID, ItemData(time, vendorId, totalPrice, year, month))
  }
}
