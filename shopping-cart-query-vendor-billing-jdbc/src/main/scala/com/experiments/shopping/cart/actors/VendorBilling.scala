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
import com.experiments.shopping.cart.repositories.ReadSideRepository
import com.experiments.shopping.cart.repositories.internal._
import com.experiments.shopping.cart.serializers.Conversions._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.DatabaseDef

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

object VendorBilling {

  sealed trait Message
  final case object ObtainOffset extends Message
  final case object StopQuery extends Message
  private final case class OffsetEnvelope(optInfo: Option[OffsetTrackingInformationRow]) extends Message
  private final case object BeginQuery extends Message

  case class EventData(timePurchased: ZonedDateTime, vendorId: UUID, totalPrice: BigDecimal, year: Int, month: Int)
  case object InfiniteStreamCompletedException extends Exception("Infinite Stream has completed")

  def updateReadSide(
    readSide: ReadSideRepository,
    existing: Option[VendorBillingInformationRow],
    itemData: EventData,
    offsetInformation: OffsetTrackingInformationRow
  ): Future[(VendorBillingInformationRow, OffsetTrackingInformationRow)] = {
    val existingBalance = existing.map(_.balance).getOrElse(BigDecimal(0))
    readSide.update(
      VendorBillingInformationRow(
        itemData.vendorId,
        itemData.year,
        itemData.month,
        existingBalance + itemData.totalPrice
      ),
      offsetInformation
    )
  }

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

  var database: DatabaseDef = _
  var readSide: ReadSideRepository = _

  override def preStart(): Unit = {
    log.info("Creating a new Cassandra Session")
    database = Database.forConfig("db")
    val vendorBillingQuery: TableQuery[VendorBillingTable] = TableQuery(
      VendorBillingTable(
        tableName = settings.database.vendorBillingTableName,
        tableSchema = settings.database.schemaName
      )
    )
    val offsetTrackingQuery: TableQuery[OffsetTrackingTable] = TableQuery(
      OffsetTrackingTable(
        tableName = settings.database.offsetTrackingTableName,
        tableSchema = settings.database.schemaName
      )
    )
    readSide = new ReadSideRepository(offsetTrackingQuery, vendorBillingQuery)(ec, database)
  }

  override def postStop(): Unit =
    if (database != null) {
      // ensure we close connection when an actor is restarted (note preRestart invokes postStop)
      log.info("Closing PostgreSQL connection")
      database.close()
      database = null
      readSide = null
    } else {
      log.warning("database (PostgreSQL Connection) is already null")
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

      case OffsetEnvelope(Some(OffsetTrackingInformationRow(_, _, offset))) =>
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
          val (offsetUUID, itemData) = parseEventEnvelope(ee)
          val offsetTrackingInfo = OffsetTrackingInformationRow(HydratorId, Tag, offsetUUID)

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

    case StopQuery =>
      log.warning("Received StopQuery, stopping query stream and actor")
      optKillSwitch.foreach(_.shutdown())
      context.stop(self)
  }
}
