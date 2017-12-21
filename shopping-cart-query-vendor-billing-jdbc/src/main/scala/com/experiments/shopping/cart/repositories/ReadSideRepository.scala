package com.experiments.shopping.cart.repositories

import java.util.UUID

import com.experiments.shopping.cart.repositories.internal._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.DatabaseDef

import scala.concurrent.{ ExecutionContext, Future }

class ReadSideRepository(
  offsetTrackingTableQuery: TableQuery[OffsetTrackingTable],
  vendorBillingTableQuery: TableQuery[VendorBillingTable]
)(implicit ec: ExecutionContext, db: DatabaseDef) {

  def find(vendorId: UUID, year: Int, month: Int): Future[Option[VendorBillingInformationRow]] = {
    val query = vendorBillingTableQuery
      .filter(_.vendorId === vendorId)
      .filter(_.year === year)
      .filter(_.month === month)
      .take(1)
      .result

    db.run(query)
      .map(_.headOption)
  }

  def find(hydratorId: String, tag: String): Future[Option[OffsetTrackingInformationRow]] = {
    val query = offsetTrackingTableQuery
      .filter(_.hydratorId === hydratorId)
      .filter(_.tag === tag)
      .take(1)
      .result

    db.run(query)
      .map(_.headOption)
  }

  def update(
    info: VendorBillingInformationRow,
    offset: OffsetTrackingInformationRow
  ): Future[(VendorBillingInformationRow, OffsetTrackingInformationRow)] = {
    val res: DBIO[Unit] = for {
      _ <- offsetTrackingTableQuery.insertOrUpdate(offset)
      _ <- vendorBillingTableQuery.insertOrUpdate(info)
    } yield ()

    db.run(res.transactionally)
      .map(_ => (info, offset))
  }
}
