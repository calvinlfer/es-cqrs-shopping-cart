package com.experiments.shopping.cart.repositories.internal

import java.util.UUID
import com.outworkers.phantom.dsl._
import ConsistencyLevel._

import scala.concurrent.Future

case class ProducerOffsetTrackingInformation(producerId: String, tag: String, offset: UUID)

/**
  * Used by the Kafka producer to keep track of the events that it has published from the event journal
  *
  * See consistency level options:
  * http://docs.datastax.com/en/archived/cassandra/2.0/cassandra/dml/dml_config_consistency_c.html
  */
abstract class ProducerOffsetTrackingRepository
    extends Table[ProducerOffsetTrackingRepository, ProducerOffsetTrackingInformation] {
  object producerId extends StringColumn with PartitionKey
  object tag extends StringColumn with PartitionKey
  object offset extends TimeUUIDColumn

  override def tableName: String = "producer_offset_tracking"

  def find(producerId: String, tag: String): Future[Option[ProducerOffsetTrackingInformation]] =
    select
      .where(_.producerId eqs producerId)
      .and(_.tag eqs tag)
      .consistencyLevel_=(EACH_QUORUM)
      .one()

  def update(info: ProducerOffsetTrackingInformation): Future[ProducerOffsetTrackingInformation] =
    insert
      .value(_.producerId, info.producerId)
      .value(_.tag, info.tag)
      .value(_.offset, info.offset)
      .consistencyLevel_=(LOCAL_ONE)
      .future()
      .map(_ => info)
}
