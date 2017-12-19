package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.pattern.BackoffSupervisor
import com.experiments.shopping.cart.actors.VendorBilling

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  val settings = Settings(system)

  Cluster(system).registerOnMemberUp {
    val supervisorSettings = settings.querySupervision
    val supervisor = BackoffSupervisor.props(
      childProps = VendorBilling.props,
      childName = "vendor-billing-query-actor",
      minBackoff = supervisorSettings.minBackOff,
      maxBackoff = supervisorSettings.maxBackOff,
      randomFactor = supervisorSettings.noise // 10% noise to vary intervals (mitigate the thundering herd problem)
    )
    system.actorOf(supervisor, "vendor-billing-query-supervisor")
  }
}
