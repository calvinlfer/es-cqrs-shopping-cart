package com.experiments.shopping.cart.repositories.internal

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class VendorBillingInformationRow(vendorId: UUID, year: Int, month: Int, balance: BigDecimal)

object VendorBillingTable {
  def apply(tableName: String, tableSchema: String)(tableTag: Tag): VendorBillingTable =
    new VendorBillingTable(tableName, tableSchema, tableTag)
}

final class VendorBillingTable(tableName: String, tableSchema: String, tableTag: Tag)
    extends Table[VendorBillingInformationRow](tableTag, Some(tableSchema), tableName) {
  // usually you would define this as
  // vendorId = column[UUID]("vendor_id", O.Length(36, varying = false), O.PrimaryKey)
  // year = column[Int]("year", O.PrimaryKey)
  // month = column[Int]("month", O.PrimaryKey)
  // but this is a Postgres limitation so we rely on primaryKey
  val vendorId = column[UUID]("vendor_id", O.Length(36, varying = false))
  val year = column[Int]("year")
  val month = column[Int]("month")
  val balance = column[BigDecimal]("balance")
  val pk = primaryKey(s"vendorId_year_month_pk", (vendorId, year, month))

  override def * =
    (vendorId, year, month, balance) <> (VendorBillingInformationRow.tupled, VendorBillingInformationRow.unapply)
}
