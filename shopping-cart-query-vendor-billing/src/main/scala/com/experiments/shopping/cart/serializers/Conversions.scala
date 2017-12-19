package com.experiments.shopping.cart.serializers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object Conversions {
  import data.model.{ events => proto }

  // Data -> Domain
  def domainUuid(id: proto.Id): UUID =
    UUID.fromString(id.value)

  def domainTime(timeString: String): ZonedDateTime =
    ZonedDateTime.parse(timeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
