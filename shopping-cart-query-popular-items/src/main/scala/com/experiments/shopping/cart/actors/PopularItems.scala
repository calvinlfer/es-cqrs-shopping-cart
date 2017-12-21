package com.experiments.shopping.cart.actors

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, Props, Status }
import akka.pattern.pipe
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.{ ActorMaterializer, KillSwitch, KillSwitches }
import com.experiments.shopping.cart.Settings
import com.experiments.shopping.cart.repositories.{ AppDatabase, ReadSideRepository }
import com.experiments.shopping.cart.repositories.internal.{ ItemInformation, OffsetTrackingInformation }
import com.outworkers.phantom.dsl._
import PopularItems._
import akka.Done
import akka.stream.scaladsl.{ Keep, Sink }
import com.datastax.driver.core.utils.UUIDs
import com.experiments.shopping.cart.serializers.Conversions._

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.duration._

object PopularItems {
  sealed trait Message
  final case object ObtainOffset extends Message
  final case object StopQuery extends Message
  private final case class OffsetEnvelope(optInfo: Option[OffsetTrackingInformation]) extends Message
  private final case object BeginQuery extends Message

  case class EventData(productId: UUID, vendorId: UUID, name: String, quantity: Int, year: Int, month: Int, day: Int)
  case object InfiniteStreamCompletedException extends Exception("Infinite Stream has completed")

  def updateReadSide(
    readSideRepository: ReadSideRepository,
    existing: Option[ItemInformation],
    eventData: EventData,
    offsetTrackingInformation: OffsetTrackingInformation
  ): Future[(ItemInformation, OffsetTrackingInformation)] = {
    val existingQuantity = existing.map(_.quantity).getOrElse(0)
    readSideRepository.update(
      ItemInformation(
        eventData.vendorId,
        eventData.productId,
        eventData.year,
        eventData.month,
        eventData.day,
        eventData.quantity + existingQuantity,
        eventData.name
      ),
      offsetTrackingInformation
    )
  }

  def props: Props = Props(new PopularItems)
}

class PopularItems extends Actor with ActorLogging {
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  val settings = Settings(context.system)
  val journal: CassandraReadJournal =
    PersistenceQuery(context.system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val HydratorId = "popular-items-query"
  val Tag = "item-purchased"

  var appDatabase: AppDatabase = _
  var readSide: ReadSideRepository = _

  override def preStart(): Unit = {
    log.info("Creating a new Cassandra Session")
    appDatabase = AppDatabase(settings)
    appDatabase.create(30.seconds)
    readSide = appDatabase.readSide
  }

  override def postStop(): Unit =
    if (appDatabase != null) {
      // ensure we close connection when an actor is restarted (note preRestart invokes postStop)
      log.info("Closing Cassandra connection")
      appDatabase.shutdown()
      log.info("Cassandra connection closed")
      appDatabase = null
      readSide = null
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
      context.system.scheduler.schedule(initialDelay = 5.seconds, interval = 10.seconds)(self ! ObtainOffset)

    {
      case ObtainOffset =>
        log.info("Obtaining offset for Hydrator ID: {} for tag: {}", HydratorId, Tag)
        readSide.find(HydratorId, Tag).map(OffsetEnvelope) pipeTo self

      case OffsetEnvelope(None) =>
        cancellable.cancel()
        log.info("No offset found for Hydrator ID: {} for tag: {}", HydratorId, Tag)
        beginQuery(Offset.noOffset)

      case OffsetEnvelope(Some(OffsetTrackingInformation(_, _, offset))) =>
        cancellable.cancel()
        log.info(
          "Offset found for Hydrator ID: {} for tag: {} with offset: {} with timestamp: {}",
          HydratorId,
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
      val (killSwitch, done) = journal
        .eventsByTag(Tag, journalOffset)
        .mapAsync(1) { ee =>
          val (offsetUUID, eventData) = parseEventEnvelope(ee)
          val offsetTrackingInfo = OffsetTrackingInformation(HydratorId, Tag, offsetUUID)

          for {
            result <- readSide.find(
              eventData.vendorId,
              eventData.productId,
              eventData.year,
              eventData.month,
              eventData.day
            )
            _ <- updateReadSide(readSide, result, eventData, offsetTrackingInfo)
          } yield ()
        }
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
