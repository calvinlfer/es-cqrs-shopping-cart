package com.experiments.shopping.cart.repositories.internal

import com.outworkers.phantom.dsl._
import ConsistencyLevel._

import scala.concurrent.Future

abstract class CassandraVendorBillingRepository
    extends Table[CassandraVendorBillingRepository, VendorBillingInformation]
    with VendorBillingRepository {

  /**
    * CREATE TABLE balance_by_vendor (
    *   vendorId uuid,
    *   year int,
    *   month int,
    *   balance decimal,
    *   PRIMARY KEY ((vendorId, year), month)
    * ) WITH CLUSTERING ORDER BY (month DESC)
    *
    * WARNING: ensure that sequence in which the mappings below are defined match those in the case class if you
    * want to use the store functionality (LabelledGeneric based) offered by Phantom's Table
    */
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
