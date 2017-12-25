package com.experiments.shopping.cart

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ EventEnvelope, Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.experiments.shopping.cart.repositories.AppDatabase
import com.experiments.shopping.cart.repositories.internal.ProducerOffsetTrackingInformation
import data.model.events.ItemPurchased
import com.outworkers.phantom.dsl._

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import io.circe._
import io.circe.java8.time._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

/**
  * An initial example of how to migrate all item-purchased events from the event journal into a Kafka topic with
  * at-least-once semantics along with a batching optimization to track offsets (to keep track of all the events we have
  * transferred from the event journal into the Kafka topic)
  *
  * TODO: Use actor to come up with a more resilient implementation
  */
object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val settings = Settings(system)
  val database = AppDatabase(settings)

  val offsetTrackingDb = database.offsetTracking
  val Tag = "item-purchased"
  val ProducerId = "example"
  val PartitionSize = 1

  // TODO: Fix Blocking
  database.create(30.seconds)
  val optionalOffset = Await.result(offsetTrackingDb.find(ProducerId, Tag), 30.seconds)
  val offset = optionalOffset.map(_.offset).map(TimeBasedUUID).getOrElse(Offset.noOffset)

  val journal: CassandraReadJournal =
    PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  case class ItemDetails(time: ZonedDateTime,
                         name: String,
                         price: BigDecimal,
                         quantity: Int,
                         productId: String,
                         vendorId: String,
                         cartId: String,
                         memberId: String)
  case class ItemEnvelope(offset: UUID, item: ItemDetails)

  def itemDetails(cartId: String,
                  memberId: String,
                  timePurchased: ZonedDateTime,
                  item: data.model.events.Item): ItemDetails =
    ItemDetails(
      timePurchased,
      item.name,
      item.price,
      item.quantity,
      item.productId.get.value,
      item.vendorId.get.value,
      cartId,
      memberId
    )

  val producerSettings =
    ProducerSettings(system = system, keySerializer = new StringSerializer, valueSerializer = new StringSerializer)
      .withBootstrapServers(settings.kafka.uris)

  val result: Future[Done] =
    journal
      .eventsByTag(Tag, offset)
      .collect {
        case EventEnvelope(
            TimeBasedUUID(eventOffset),
            persistenceId,
            _,
            ItemPurchased(Some(cartId), timePurchased, Some(item))
            ) =>
          ItemEnvelope(
            eventOffset,
            itemDetails(
              cartId.value,
              persistenceId,
              ZonedDateTime.parse(timePurchased, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
              item
            )
          )
      }
      .map(
        itemEnvelope =>
          ProducerMessage.Message(
            record = new ProducerRecord[String, String](
              settings.kafka.topic,
              itemEnvelope.item.memberId.hashCode % PartitionSize,
              itemEnvelope.item.memberId,
              itemEnvelope.item.asJson.noSpaces
            ),
            passThrough = itemEnvelope
        )
      )
      .via(Producer.flow(producerSettings))
      .map(_.message.passThrough)
      .batch(max = 10, itemEnvelope => itemEnvelope.offset)((accOffset, nextMessage) => nextMessage.offset)
      .mapAsync(1)(offset => offsetTrackingDb.update(ProducerOffsetTrackingInformation(ProducerId, Tag, offset)))
      .runWith(Sink.ignore)
}
