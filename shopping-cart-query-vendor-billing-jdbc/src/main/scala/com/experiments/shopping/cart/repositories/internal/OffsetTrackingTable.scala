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

  /**
    * CREATE TABLE offset_tracking
    * (
    *   hydrator_id VARCHAR(255) NOT NULL,
    *   tag         VARCHAR(255) NOT NULL,
    *   "offset"    UUID         NOT NULL,
    *   CONSTRAINT "hydratorId_tag_pk"
    *   PRIMARY KEY (hydrator_id, tag)
    * );
    *
    * usually you would define this as
    * hydratorId = column[String]("hydrator_id", O.Length(255, varying = true), O.PrimaryKey)
    * tag = column[String]("tag", O.Length(255, varying = true), O.PrimaryKey)
    * but this is a Postgres limitation so we rely on primaryKey
    */
  val hydratorId = column[String]("hydrator_id", O.Length(255, varying = true))
  val tag = column[String]("tag", O.Length(255, varying = true))
  val offset = column[UUID]("offset", O.Length(36, varying = false))
  val pk = primaryKey(s"hydratorId_tag_pk", (hydratorId, tag))

  override def * =
    (hydratorId, tag, offset) <> (OffsetTrackingInformationRow.tupled, OffsetTrackingInformationRow.unapply)
}
