package com.experiments.shopping.cart.repositories.internal

import com.outworkers.phantom.dsl._
import ConsistencyLevel._

import scala.concurrent.Future

abstract class CassandraPopularItemsRepository
    extends Table[CassandraPopularItemsRepository, ItemInformation]
    with PopularItemsRepository {

  /**
    * CREATE TABLE item_quantity_by_day (
    *   vendorid uuid,
    *   productid uuid,
    *   year int,
    *   month int,
    *   day int,
    *   quantity int,
    *   name string
    *   PRIMARY KEY((vendorid, productid, year, month), day)
    * ) WITH CLUSTERING ORDER BY (day ASC);
    *
    * WARNING: ensure that sequence in which the mappings below are defined match those in the case class if you
    * want to use the store functionality (LabelledGeneric based) offered by Phantom's Table
    */
  object vendorId extends UUIDColumn with PartitionKey
  object productId extends UUIDColumn with PartitionKey
  object year extends IntColumn with PartitionKey
  object month extends IntColumn with PartitionKey
  object day extends IntColumn with ClusteringOrder with Ascending
  object quantity extends IntColumn
  object name extends StringColumn

  override def tableName: String = "item_quantity_by_day"

  override def find(vendorId: UUID, productId: UUID, year: Int, month: Int, day: Int): Future[Option[ItemInformation]] =
    select
      .where(_.vendorId eqs vendorId)
      .and(_.productId eqs productId)
      .and(_.year eqs year)
      .and(_.month eqs month)
      .and(_.day eqs day)
      .consistencyLevel_=(LOCAL_QUORUM)
      .one()
}
