package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ Offset, PersistenceQuery }
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, ThrottleMode }

import scala.concurrent.duration._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val journal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  println(system.settings.config.getString("akka.remote.netty.tcp.bind-port"))
  Cluster(system).registerOnMemberUp {
    println(s"Starting query on ${Cluster(system).selfAddress}")
    journal
      .eventsByTag("items-purchased", Offset.noOffset)
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .to(Sink.foreach(println))
      .run()
  }
}
