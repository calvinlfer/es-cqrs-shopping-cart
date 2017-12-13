package com.experiments.shopping.cart

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, PoisonPill }
import akka.pattern.ask
import akka.util.Timeout
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.domain.{ Item, ProductId, VendorId }

import scala.concurrent.{ ExecutionContext, Future }

trait Repl {
  val system: ActorSystem
  implicit val timeout: Timeout
  implicit val ec: ExecutionContext

  def usage(): Unit =
    println("""
              |Usage:
              |add <item-name>
              |remove <item-name>
              |list
              |adjust <item-name> <quantity>
              |checkout
              |exit
              |help
            """.stripMargin)

  def commandLoop(exampleShoppingCart: ActorRef): Future[Unit] = {
    val input = scala.io.StdIn.readLine()
    val command = input.split(" ")
    if (command.isEmpty) {
      commandLoop(exampleShoppingCart)
    } else {
      val result = command(0) match {
        case "add" =>
          val item = command.lift(1).getOrElse("apple")
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (exampleShoppingCart ? AddItem(Item(ProductId(uuidFromItem), VendorId(uuidFromItem), item, 1.0, 1)))
            .mapTo[Response]

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

        case "help" =>
          usage()
          Future.successful("---")

        case _ =>
          (exampleShoppingCart ? DisplayContents).mapTo[Response]
      }

      result.flatMap { response =>
        println(response)
        commandLoop(exampleShoppingCart)
      }
    }
  }
}
