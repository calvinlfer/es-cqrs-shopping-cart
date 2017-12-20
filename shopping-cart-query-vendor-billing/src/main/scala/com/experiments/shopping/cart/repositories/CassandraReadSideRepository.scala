package com.experiments.shopping.cart.repositories

import com.experiments.shopping.cart.repositories.internal._
import com.outworkers.phantom.connectors.RootConnector
import com.outworkers.phantom.dsl._

import scala.concurrent.Future

abstract class CassandraReadSideRepository(offsetTracking: CassandraOffsetTrackingRepository,
                                           billing: CassandraVendorBillingRepository)
    extends ReadSideRepository
    with RootConnector {

  /**
    * Atomically update the read-side table (balance_by_vendor) and the offset tracking table (offset_tracking) to
    * ensure exactly-once delivery semantics
    * @param info Vendor Billing Information
    * @param offset Offset Tracking Information
    * @return
    */
  override def update(
    info: VendorBillingInformation,
    offset: OffsetTrackingInformation
  ): Future[(VendorBillingInformation, OffsetTrackingInformation)] =
    Batch.logged
      .add(billing.store(info))
      .add(offsetTracking.store(offset))
      .future()
      .map(_ => (info, offset))

  override def find(vendorId: UUID, year: Int, month: Int): Future[Option[VendorBillingInformation]] =
    billing.find(vendorId, year, month)

  override def find(hydratorId: String, tag: String): Future[Option[OffsetTrackingInformation]] =
    offsetTracking.find(hydratorId, tag)

}
