package com.experiments.shopping.cart.repositories

import com.experiments.shopping.cart.repositories.internal._
import com.outworkers.phantom.connectors.RootConnector
import com.outworkers.phantom.dsl._

import scala.concurrent.Future

abstract class CassandraReadSideRepository(offsetTracking: CassandraOffsetTrackingRepository,
                                           popularItems: CassandraPopularItemsRepository)
    extends ReadSideRepository
    with RootConnector {
  override def update(info: ItemInformation,
                      offset: OffsetTrackingInformation): Future[(ItemInformation, OffsetTrackingInformation)] =
    Batch.logged
      .add(popularItems.store(info))
      .add(offsetTracking.store(offset))
      .future()
      .map(_ => (info, offset))

  override def find(hydratorId: String, tag: String): Future[Option[OffsetTrackingInformation]] =
    offsetTracking.find(hydratorId, tag)

  override def find(vendorId: UUID, productId: UUID, year: Int, month: Int, day: Int): Future[Option[ItemInformation]] =
    popularItems.find(vendorId, productId, year, month, day)
}
