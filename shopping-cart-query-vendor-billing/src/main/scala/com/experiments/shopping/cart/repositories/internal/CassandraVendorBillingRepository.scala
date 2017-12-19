package com.experiments.shopping.cart.repositories.internal

import com.outworkers.phantom.dsl._
import ConsistencyLevel._

import scala.concurrent.Future

abstract class CassandraVendorBillingRepository
    extends Table[CassandraVendorBillingRepository, VendorBillingInformation]
    with VendorBillingRepository {
  object vendorId extends UUIDColumn with PartitionKey
  object year extends IntColumn with PartitionKey
  object month extends IntColumn with ClusteringOrder with Descending
  object balance extends BigDecimalColumn

  override def tableName: String = "balance_by_vendor"

  override def find(vendorId: UUID, year: Int, month: Int): Future[Option[VendorBillingInformation]] =
    select
      .where(_.vendorId eqs vendorId)
      .and(_.year eqs year)
      .and(_.month eqs month)
      .consistencyLevel_=(LOCAL_QUORUM)
      .one()
}
