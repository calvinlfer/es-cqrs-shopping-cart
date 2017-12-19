package com.experiments.shopping.cart.repositories.internal

import com.outworkers.phantom.dsl._
import ConsistencyLevel._

import scala.concurrent.Future

abstract class CassandraOffsetTrackingRepository
    extends Table[CassandraOffsetTrackingRepository, OffsetTrackingInformation]
    with OffsetTrackingRepository {
  object hydratorId extends StringColumn with PartitionKey
  object tag extends StringColumn with PartitionKey
  object offset extends TimeUUIDColumn

  override def tableName: String = "offset_tracking"

  override def find(hydratorId: String, tag: String): Future[Option[OffsetTrackingInformation]] =
    select
      .where(_.hydratorId eqs hydratorId)
      .and(_.tag eqs tag)
      .consistencyLevel_=(LOCAL_QUORUM)
      .one()
}
