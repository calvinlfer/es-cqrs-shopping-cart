package com.experiments.shopping.cart.actors

import java.time.ZonedDateTime
import java.util.UUID

import akka.Done
import akka.actor.{ Actor, ActorLogging, Props, Status }
import akka.pattern.pipe
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.scaladsl.{ Keep, Sink }
import akka.stream.{ ActorMaterializer, KillSwitch, KillSwitches }
import com.datastax.driver.core.utils.UUIDs
import com.experiments.shopping.cart.Settings
import com.experiments.shopping.cart.actors.VendorBilling._
import com.experiments.shopping.cart.repositories.internal.{ OffsetTrackingInformation, VendorBillingInformation }
import com.experiments.shopping.cart.repositories.{ AppDatabase, ReadSideRepository }
import com.experiments.shopping.cart.serializers.Conversions._
import com.outworkers.phantom.dsl._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

object VendorBilling {

  sealed trait Message
  final case object ObtainOffset extends Message
  private final case class OffsetEnvelope(optInfo: Option[OffsetTrackingInformation]) extends Message
  private final case object BeginQuery extends Message

  case class ItemData(timePurchased: ZonedDateTime, vendorId: UUID, totalPrice: BigDecimal, year: Int, month: Int)
  case object InfiniteStreamCompletedException extends Exception("Infinite Stream has completed")

  def updateReadSide(
    readSide: ReadSideRepository,
    existing: Option[VendorBillingInformation],
    itemData: ItemData,
    offsetInformation: OffsetTrackingInformation
  ): Future[(VendorBillingInformation, OffsetTrackingInformation)] =
    existing.fold(
      ifEmpty = readSide.update(
        VendorBillingInformation(itemData.vendorId, itemData.year, itemData.month, itemData.totalPrice),
        offsetInformation
      )
    )(
      existingBillingInfo =>
        readSide.update(
          VendorBillingInformation(
            itemData.vendorId,
            itemData.year,
            itemData.month,
            itemData.totalPrice + existingBillingInfo.balance
          ),
          offsetInformation
      )
    )

  def props: Props = Props(new VendorBilling)
}

class VendorBilling extends Actor with ActorLogging {
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  val settings = Settings(context.system)
  val journal: CassandraReadJournal =
    PersistenceQuery(context.system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val HydratorId = "vendor-billing-query"
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
      appDatabase.connector.session.getCluster.close()
      appDatabase = null
      readSide = null
    } else {
      log.warning("AppDatabase (Cassandra Connection) is already null")
    }

  override def receive: Receive = obtainOffset

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
        context.become(queryingJournal(Offset.noOffset))
        self ! BeginQuery

      case OffsetEnvelope(Some(OffsetTrackingInformation(_, _, offset))) =>
        cancellable.cancel()
        log.info(
          "Offset found for Hydrator ID: {} for tag: {} with offset: {} with timestamp: {}",
          HydratorId,
          Tag,
          offset,
          UUIDs.unixTimestamp(offset)
        )
        context.become(queryingJournal(TimeBasedUUID(offset)))
        self ! BeginQuery

      case Status.Failure(exception) =>
        cancellable.cancel()
        log.error(exception, "Failed to obtain an offset from the Offset Tracking Table, failing fast")
        throw exception
    }
  }

  def queryingJournal(journalOffset: Offset, optKillSwitch: Option[KillSwitch] = None): Receive = {
    case BeginQuery =>
      val (killSwitch, done) = journal
        .eventsByTag(Tag, journalOffset)
        .mapAsync(1) { ee =>
          val (offsetUUID, itemData) = parseEventEnvelope(ee)
          val offsetTrackingInfo = OffsetTrackingInformation(HydratorId, Tag, offsetUUID)

          for {
            result <- readSide.find(itemData.vendorId, itemData.year, itemData.month)
            _ <- updateReadSide(readSide, result, itemData, offsetTrackingInfo)
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
  }
}
