package com.experiments.shopping.cart.repositories

import com.experiments.shopping.cart.repositories.internal._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.DatabaseDef

import scala.concurrent.{ ExecutionContext, Future }

class ReadSideRepository(
  offsetTrackingTableQuery: TableQuery[OffsetTrackingTable],
  vendorBillingTableQuery: TableQuery[VendorBillingTable]
)(implicit ec: ExecutionContext, db: DatabaseDef) {

  def update(
    info: VendorBillingInformationRow,
    offset: OffsetTrackingInformationRow
  ): Future[(VendorBillingInformationRow, OffsetTrackingInformationRow)] = {
    val res: DBIO[Unit] = for {
      _ <- offsetTrackingTableQuery += offset
      _ <- vendorBillingTableQuery += info
    } yield ()

    db.run(res.transactionally).map(_ => (info, offset))
  }
}
