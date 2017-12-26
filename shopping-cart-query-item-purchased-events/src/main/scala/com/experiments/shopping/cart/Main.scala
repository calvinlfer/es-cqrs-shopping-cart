package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.singleton.{ ClusterSingletonManager, ClusterSingletonManagerSettings }
import akka.pattern.BackoffSupervisor
import com.experiments.shopping.cart.actors.ItemPurchasedManager
import com.experiments.shopping.cart.actors.ItemPurchasedManager.StopQuery

/**
  * An example of how to migrate all item-purchased events from the event journal into a Kafka topic with
  * at-least-once semantics along with a batching optimization to track offsets (to keep track of all the events we have
  * transferred from the event journal into the Kafka topic)
  */
object Main extends App {
  implicit val system: ActorSystem = ActorSystem("shopping-cart-system")
  val settings = Settings(system)

  Cluster(system).registerOnMemberUp {
    val supervisorSettings = settings.querySupervision
    val supervisor = BackoffSupervisor.props(
      childProps = ItemPurchasedManager.props,
      childName = "item-purchased-query-actor",
      minBackoff = supervisorSettings.minBackOff,
      maxBackoff = supervisorSettings.maxBackOff,
      randomFactor = supervisorSettings.noise // noise to vary intervals (mitigate the thundering herd problem)
    )
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = supervisor,
        terminationMessage = StopQuery,
        settings = ClusterSingletonManagerSettings(system)
      )
    )
  }
}
