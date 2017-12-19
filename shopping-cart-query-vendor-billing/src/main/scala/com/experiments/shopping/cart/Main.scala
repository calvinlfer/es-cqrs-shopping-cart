package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ EventEnvelope, Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.experiments.shopping.cart.repositories.{ AppDatabase, ReadSideRepository }
import com.outworkers.phantom.dsl._
import com.typesafe.config.ConfigFactory
import data.model.events.ItemPurchased

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val settings = Settings(ConfigFactory.load())
  val appDatabase = AppDatabase(settings)
  val readSide: ReadSideRepository = appDatabase.readSide

  appDatabase.create(30.seconds)
  //  val result: Future[(VendorBillingInformation, OffsetTrackingInformation)] =
  //    for {
  //      Some(billing) <- readSide.find(UUID.fromString("398a9f5f-f7fb-4ac6-b948-ca175fb385ab"), 2017, 12)
  //      ans <- readSide.update(
  //        VendorBillingInformation(
  //          UUID.fromString("398a9f5f-f7fb-4ac6-b948-ca175fb385ab"),
  //          2017,
  //          12,
  //          billing.balance + 100.10
  //        ),
  //        OffsetTrackingInformation("vendor-processor", "items-purchased", UUIDs.timeBased())
  //      )
  //    } yield ans
  //  val blocking = Await.result(result, 30.seconds)
  //  println(blocking)

  val journal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  Cluster(system).registerOnMemberUp {
    println(s"Starting query on ${Cluster(system).selfAddress}")
    journal
      .eventsByTag("item-purchased", Offset.noOffset)
      .map {
        case EventEnvelope(t @ TimeBasedUUID(uuid), persistenceId, seqNr, i: ItemPurchased) =>
          s"offset=(timestamp=${uuid.timestamp()} raw=$t) persistenceId=$persistenceId seqNr=$seqNr event=$i"
      }
      .to(Sink.foreach(println))
      .run()
  }
}
