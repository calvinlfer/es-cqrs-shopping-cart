package com.experiments.shopping.cart

import akka.actor.ActorSystem
import akka.util.Timeout
import com.experiments.shopping.cart.actors.ShoppingCart

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App with Repl {
  val system = ActorSystem("shopping-cart-command-system")
  val exampleShoppingCart = system.actorOf(ShoppingCart.props, "example-cart")
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.dispatcher

  commandLoop(exampleShoppingCart)
}
