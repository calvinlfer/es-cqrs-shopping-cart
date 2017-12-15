package com.experiments.shopping.cart

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, PoisonPill }
import akka.pattern.ask
import akka.util.Timeout
import com.experiments.shopping.cart.actors.ShoppingCart
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.actors.sharding.ShoppingCart.CartEnvelope
import com.experiments.shopping.cart.domain.{ Item, MemberId, ProductId, VendorId }

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
              |change-member <friendly-username>
              |current-member
              |exit
              |help
            """.stripMargin)

  def showItem(item: Item): String =
    s"""
      |Name: ${item.name}
      |Price: ${item.price}
      |Quantity: ${item.quantity}
    """.stripMargin

  case class ChangeMember(memberId: MemberId)

  def commandLoop(cartRouter: ActorRef, memberId: MemberId): Future[Unit] = {
    def envelopeFn(cmd: ShoppingCart.Command) = CartEnvelope(memberId, cmd)

    val input = scala.io.StdIn.readLine()
    val command = input.split(" ")
    if (command.isEmpty) {
      commandLoop(cartRouter, memberId)
    } else {
      val result = command(0) match {
        case "add" =>
          val item = command.lift(1).getOrElse("apple")
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (cartRouter ? envelopeFn(AddItem(Item(ProductId(uuidFromItem), VendorId(uuidFromItem), item, 1.0, 1))))
            .mapTo[Response]

        case "list" =>
          (cartRouter ? envelopeFn(DisplayContents)).mapTo[Response]

        case "adjust" =>
          val item = command.lift(1).getOrElse("apple")
          val quantity = command.lift(2).map(_.toInt).getOrElse(1)
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (cartRouter ? envelopeFn(AdjustQuantity(ProductId(uuidFromItem), quantity))).mapTo[Response]

        case "remove" =>
          val item = command.lift(1).getOrElse("apple")
          val uuidFromItem = UUID.nameUUIDFromBytes(item.getBytes())
          (cartRouter ? envelopeFn(RemoveItem(ProductId(uuidFromItem)))).mapTo[Response]

        case "checkout" =>
          (cartRouter ? envelopeFn(Checkout)).mapTo[Response]

        case "exit" =>
          cartRouter ! PoisonPill
          system.terminate()
          sys.exit(-1)

        case "change-member" =>
          val userId = command.lift(1).getOrElse("calvin")
          val memberId = MemberId(UUID.nameUUIDFromBytes(userId.getBytes()))
          Future.successful(ChangeMember(memberId))

        case "current-member" =>
          Future.successful(memberId)

        case "help" =>
          usage()
          Future.successful("---")

        case _ =>
          (cartRouter ? envelopeFn(DisplayContents)).mapTo[Response]
      }

      result
        .recover {
          case t: Throwable =>
            println(t.getMessage)
            "Try again..."
        }
        .flatMap {
          case ChangeMember(newMemberId) =>
            commandLoop(cartRouter, newMemberId)

          case CartContents(items, cartId) =>
            println(s"""
                |Cart:
                |${items.map(showItem).mkString}
              """.stripMargin)
            commandLoop(cartRouter, memberId)
          case response =>
            println(response)
            commandLoop(cartRouter, memberId)
        }
    }
  }
}
