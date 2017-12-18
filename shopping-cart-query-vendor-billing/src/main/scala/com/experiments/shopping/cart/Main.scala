package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ EventEnvelope, Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import data.model.events.ItemsPurchased

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val journal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  Cluster(system).registerOnMemberUp {
    println(s"Starting query on ${Cluster(system).selfAddress}")
    journal
      .eventsByTag("items-purchased", Offset.noOffset)
      .map {
        case EventEnvelope(t @ TimeBasedUUID(uuid), persistenceId, seqNr, i: ItemsPurchased) =>
          s"offset=(timestamp=${uuid.timestamp()} raw=$t) persistenceId=$persistenceId seqNr=$seqNr event=$i"
      }
      .to(Sink.foreach(println))
      .run()
  }
}
