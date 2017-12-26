package com.experiments.shopping.cart.actors

import java.time.ZonedDateTime
import java.util.UUID

import akka.Done
import akka.actor.{ Actor, ActorLogging, Props, Status }
import akka.kafka.scaladsl.Producer
import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.pattern.pipe
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.scaladsl.{ Keep, Sink }
import akka.stream.{ ActorMaterializer, KillSwitch, KillSwitches }
import com.datastax.driver.core.utils.UUIDs
import com.experiments.shopping.cart.Settings
import com.experiments.shopping.cart.actors.ItemPurchasedManager._
import com.experiments.shopping.cart.repositories.AppDatabase
import com.experiments.shopping.cart.repositories.internal.{
  ProducerOffsetTrackingInformation,
  ProducerOffsetTrackingRepository
}
import com.experiments.shopping.cart.serializers.Conversions._
import com.outworkers.phantom.dsl._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import io.circe._
import io.circe.java8.time._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object ItemPurchasedManager {

  sealed trait Message
  final case object ObtainEventJournalOffset extends Message
  final case object StopQuery extends Message
  private final case class EventJournalOffsetEnvelope(optInfo: Option[ProducerOffsetTrackingInformation])
      extends Message
  private final case object BeginQuery extends Message

  case class ItemDetails(time: ZonedDateTime,
                         name: String,
                         price: BigDecimal,
                         quantity: Int,
                         productId: UUID,
                         vendorId: UUID,
                         cartId: UUID,
                         memberId: String)

  case object InfiniteStreamCompletedException extends Exception("Infinite Stream has completed")

  def kafkaTopicPartitioner(partitionSize: Int, itemDetails: ItemDetails): Int =
    itemDetails.memberId.hashCode % partitionSize

  def props: Props = Props(new ItemPurchasedManager)
}

class ItemPurchasedManager extends Actor with ActorLogging {
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  val settings = Settings(context.system)
  val journal: CassandraReadJournal =
    PersistenceQuery(context.system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val kafkaProducerSettings: ProducerSettings[String, String] =
    ProducerSettings(
      system = context.system,
      keySerializer = new StringSerializer,
      valueSerializer = new StringSerializer
    ).withBootstrapServers(settings.kafka.uris)
  val ProducerId = "kafka-producer-example"
  val Tag = "item-purchased"

  var appDatabase: AppDatabase = _
  var offsetTracker: ProducerOffsetTrackingRepository = _

  override def preStart(): Unit = {
    log.info("Creating a new Cassandra Session")
    appDatabase = AppDatabase(settings)
    appDatabase.create(30.seconds) // WARNING: This blocks
    offsetTracker = appDatabase.offsetTracking
  }

  override def postStop(): Unit =
    if (appDatabase != null) {
      // ensure we close connection when an actor is restarted (note preRestart invokes postStop)
      log.info("Closing Cassandra connection")
      appDatabase.shutdown()
      log.info("Cassandra connection closed")
      appDatabase = null
      offsetTracker = null
    } else {
      log.warning("AppDatabase (Cassandra Connection) is already null")
    }

  override def receive: Receive = obtainOffset

  def beginQuery(offset: Offset): Unit = {
    log.info("Transitioning to queryingJournal")
    context.become(queryingJournal(offset))
    self ! BeginQuery
  }

  def obtainOffset: Receive = {
    val cancellable =
      context.system.scheduler.schedule(initialDelay = 5.seconds, interval = 10.seconds)(
        self ! ObtainEventJournalOffset
      )

    {
      case ObtainEventJournalOffset =>
        log.info("Obtaining offset for Producer ID: {} for tag: {}", ProducerId, Tag)
        offsetTracker.find(ProducerId, Tag).map(EventJournalOffsetEnvelope) pipeTo self

      case EventJournalOffsetEnvelope(None) =>
        cancellable.cancel()
        log.info("No offset found for Producer ID: {} for tag: {}", ProducerId, Tag)
        beginQuery(Offset.noOffset)

      case EventJournalOffsetEnvelope(Some(ProducerOffsetTrackingInformation(_, _, offset))) =>
        cancellable.cancel()
        log.info(
          "Offset found for Producer ID: {} for tag: {} with offset: {} with timestamp: {}",
          ProducerId,
          Tag,
          offset,
          UUIDs.unixTimestamp(offset)
        )
        beginQuery(TimeBasedUUID(offset))

      case Status.Failure(exception) =>
        cancellable.cancel()
        log.error(exception, "Failed to obtain an offset from the Offset Tracking Table, failing fast")
        throw exception

      case StopQuery =>
        context.stop(self)
    }
  }

  def queryingJournal(journalOffset: Offset, optKillSwitch: Option[KillSwitch] = None): Receive = {
    case BeginQuery =>
      log.info("Starting eventsByTag query: eventsByTag({}, {})", Tag, journalOffset)
      val (killSwitch, done) =
        journal
          .eventsByTag(Tag, journalOffset)
          .collect(parseEventEnvelope)
          .map {
            case (eventJournalOffset, itemDetails) =>
              ProducerMessage.Message(
                record = new ProducerRecord[String, String](
                  settings.kafka.topic,
                  kafkaTopicPartitioner(settings.kafka.partitionSize, itemDetails),
                  itemDetails.memberId,
                  itemDetails.asJson.noSpaces
                ),
                passThrough = eventJournalOffset
              )
          }
          .via(Producer.flow(kafkaProducerSettings))
          .map(_.message.passThrough)
          .batch(max = 10, identity)((accOffset, nextOffset) => nextOffset) // when aggregating, discard old event offsets
          .mapAsync(1)(offset => offsetTracker.update(ProducerOffsetTrackingInformation(ProducerId, Tag, offset)))
          .viaMat(KillSwitches.single)(Keep.right)
          .toMat(Sink.ignore)(Keep.both)
          .run()

      done pipeTo self
      context.become(queryingJournal(journalOffset, Some(killSwitch)))

    case Status.Failure(exception) =>
      log.error(exception, "eventsByTag query has failed, failing fast")
      throw exception

    case Done =>
      log.error("An infinite stream (eventsByTag) should not complete, failing")
      throw InfiniteStreamCompletedException

    case StopQuery =>
      log.warning("Received StopQuery, stopping query stream and actor")
      optKillSwitch.foreach(_.shutdown())
      context.stop(self)
  }
}
