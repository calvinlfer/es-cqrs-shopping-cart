package com.experiments.shopping.cart.repositories.internal

import com.outworkers.phantom.dsl._
import ConsistencyLevel._

import scala.concurrent.Future

abstract class CassandraOffsetTrackingRepository
    extends Table[CassandraOffsetTrackingRepository, OffsetTrackingInformation]
    with OffsetTrackingRepository {

  /**
    * CREATE TABLE offset_tracking (
    *   hydratorId text,
    *   tag text,
    *   offset timeuuid,
    *   PRIMARY KEY ((hydratorId, tag))
    * )
    *
    * WARNING: ensure that sequence in which the mappings below are defined match those in the case class if you
    * want to use the store functionality (LabelledGeneric based) offered by Phantom's Table
    */
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
