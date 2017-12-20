package com.experiments.shopping.cart.repositories.internal

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class OffsetTrackingInformationRow(hydratorId: String, tag: String, offset: UUID)

object OffsetTrackingTable {
  def apply(tableName: String, tableSchema: String)(tableTag: Tag): OffsetTrackingTable =
    new OffsetTrackingTable(tableName, tableSchema, tableTag)
}

final class OffsetTrackingTable(tableName: String, tableSchema: String, tableTag: Tag)
    extends Table[OffsetTrackingInformationRow](tableTag, Some(tableSchema), tableName) {

  val hydratorId = column[String]("hydrator_id", O.Length(255, varying = true), O.PrimaryKey)
  val tag = column[String]("tag", O.Length(255, varying = true), O.PrimaryKey)
  val offset = column[UUID]("offset", O.Length(36, varying = false))

  override def * =
    (hydratorId, tag, offset) <> (OffsetTrackingInformationRow.tupled, OffsetTrackingInformationRow.unapply)
}
