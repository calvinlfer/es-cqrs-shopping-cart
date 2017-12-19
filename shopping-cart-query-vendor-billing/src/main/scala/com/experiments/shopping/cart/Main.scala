package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.experiments.shopping.cart.actors.VendorBilling
import com.experiments.shopping.cart.repositories.{ AppDatabase, ReadSideRepository }
import com.outworkers.phantom.dsl._

import scala.concurrent.duration._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  val settings = Settings(system)
  val appDatabase = AppDatabase(settings)
  val readSide: ReadSideRepository = appDatabase.readSide

  appDatabase.create(30.seconds)

  Cluster(system).registerOnMemberUp {
    system.log.debug(s"Starting query on {}", Cluster(system).selfAddress)
    system.actorOf(VendorBilling.props(readSide), "vendor-billing-query-actor")
  }
}
