package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.experiments.shopping.cart.repositories.internal.{ OffsetTrackingInformation, VendorBillingInformation }
import com.experiments.shopping.cart.repositories.{ AppDatabase, ReadSideRepository }
import com.experiments.shopping.cart.serializers.Conversions._
import com.outworkers.phantom.dsl._
import com.typesafe.config.ConfigFactory
import data.model.events.ItemPurchased
import data.model.{ events => proto }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  val settings = Settings(ConfigFactory.load())
  val journal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val appDatabase = AppDatabase(settings)
  val readSide: ReadSideRepository = appDatabase.readSide
  val hydratorId = "vendor-billing-query"
  val tag = "item-purchased"

  appDatabase.create(30.seconds)
  val offset = readSide.find(hydratorId, tag)

  def updateReadSide(existing: Option[VendorBillingInformation],
                     vendorId: UUID,
                     year: Int,
                     month: Int,
                     balanceDelta: BigDecimal,
                     offsetInformation: OffsetTrackingInformation) =
    existing.fold(
      ifEmpty = readSide.update(VendorBillingInformation(vendorId, year, month, balanceDelta), offsetInformation)
    )(
      existingBillingInfo =>
        readSide.update(
          VendorBillingInformation(vendorId, year, month, balanceDelta + existingBillingInfo.balance),
          offsetInformation
      )
    )

  Cluster(system).registerOnMemberUp {
    system.log.debug(s"Starting query on {}", Cluster(system).selfAddress)
    offset.onComplete {
      case Success(optionOffset) =>
        val eventsByTagQuery = optionOffset
          .map { info =>
            system.log.info("Resuming query: {}", info)
            journal.eventsByTag(info.tag, TimeBasedUUID(info.offset))
          }
          .getOrElse(journal.eventsByTag(tag, Offset.noOffset))

        eventsByTagQuery
          .mapAsync(1) {
            case EventEnvelope(
                TimeBasedUUID(offsetUUID),
                persistenceId,
                seqNr,
                ItemPurchased(
                  Some(protoCartId),
                  protoTimePurchased,
                  Some(proto.Item(Some(_), Some(protoVendorId), _, price, quantity))
                )
                ) =>
              val time = domainTime(protoTimePurchased)
              val itemVendor = domainUuid(protoVendorId)
              val totalPrice = price * quantity
              val year = time.getYear
              val month = time.getMonthValue
              val offsetInformation = OffsetTrackingInformation(hydratorId, tag, offsetUUID)

              for {
                result <- readSide.find(itemVendor, year, month)
                _ <- updateReadSide(result, itemVendor, year, month, totalPrice, offsetInformation)
              } yield ()
          }
          .to(Sink.foreach(println))
          .run()

      case Failure(exception) =>
        system.log.error(exception, "Failed to contact offset tracking table")
    }
  }
}
