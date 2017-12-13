package com.experiments.shopping.cart

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill }
import akka.util.Timeout
import akka.pattern.ask
import com.experiments.shopping.cart.actors.ShoppingCart
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.domain.{ Item, ProductId, VendorId }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

object ExampleREPL extends App {
  val system = ActorSystem("shopping-cart-command-system")
  val exampleShoppingCart = system.actorOf(ShoppingCart.props, "example-cart")
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = system.dispatcher

  def commandLoop(): Future[Unit] = {
    val input = scala.io.StdIn.readLine()
    val command = input.split(" ")
    if (command.isEmpty) {
      commandLoop()
    } else {
      val result = command(0) match {
        case "add" =>
          val item = command.lift(1).getOrElse("apple")
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (exampleShoppingCart ? AddItem(Item(ProductId(uuidFromItem), VendorId(uuidFromItem), 1.0, 1))).mapTo[Response]

        case "list" =>
          (exampleShoppingCart ? DisplayContents).mapTo[Response]

        case "adjust" =>
          val item = command.lift(1).getOrElse("apple")
          val quantity = command.lift(2).map(_.toInt).getOrElse(1)
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (exampleShoppingCart ? AdjustQuantity(ProductId(uuidFromItem), quantity)).mapTo[Response]

        case "remove" =>
          val item = command.lift(1).getOrElse("apple")
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (exampleShoppingCart ? RemoveItem(ProductId(uuidFromItem))).mapTo[Response]

        case "checkout" =>
          (exampleShoppingCart ? Checkout).mapTo[Response]

        case "exit" =>
          exampleShoppingCart ! PoisonPill
          system.terminate()
          sys.exit(-1)

        case _ =>
          (exampleShoppingCart ? DisplayContents).mapTo[Response]
      }

      result.flatMap { response =>
        println(response)
        commandLoop()
      }
    }
  }
  commandLoop()
}
