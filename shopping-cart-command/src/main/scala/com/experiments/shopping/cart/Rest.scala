package com.experiments.shopping.cart

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.syntax._
import com.experiments.shopping.cart.actors.ShoppingCart._
import com.experiments.shopping.cart.actors.sharding.ShoppingCart.CartEnvelope
import com.experiments.shopping.cart.domain.{ Item, MemberId, ProductId, VendorId }
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe.Decoder.Result

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Rest {
  val cartRouter: ActorRef
  implicit val timeout: Timeout
  implicit val ec: ExecutionContext

  implicit val simpleItemDecoder: Decoder[Item] = new Decoder[Item] {
    override def apply(c: HCursor): Result[Item] =
      for {
        productId <- c.downField("productId").as[UUID]
        vendorId <- c.downField("vendorId").as[UUID]
        name <- c.downField("name").as[String]
        price <- c.downField("price").as[BigDecimal]
        quantity <- c.downField("quantity").as[Int]
      } yield Item(ProductId(productId), VendorId(vendorId), name, price, quantity)
  }

  val routes: Route =
    pathPrefix("cart" / JavaUUID) { (id: UUID) =>
      // /cart/<UUID>
      val memberId = MemberId(id)
      def envelope(cmd: Command) = CartEnvelope(memberId, cmd)

      get {
        // GET /cart/<UUID>
        val result = (cartRouter ? envelope(DisplayContents)).mapTo[CartContents]
        complete(result)
      } ~ put {
        // PUT /cart/<UUID>
        entity(as[Item]) { item =>
          val result = (cartRouter ? envelope(AddItem(item))).mapTo[Response]
          onComplete(result) {
            case Success(i: ItemAdded) => complete(i)
            case Success(v: ValidationErrors) => complete(v)
            case _ => complete(ServiceUnavailable)
          }
        }
      } ~ path("productId" / JavaUUID) { (id: UUID) =>
        // /cart/<UUID>/productId/<UUID>
        val productId = ProductId(id)

        delete {
          // DELETE /cart/<UUID>/productId/<UUID>
          val result = (cartRouter ? envelope(RemoveItem(productId))).mapTo[Response]
          onComplete(result) {
            case Success(i: ItemRemoved) => complete(i)
            case Success(v: ValidationErrors) => complete(v)
            case _ => complete(ServiceUnavailable)
          }
        }
      } ~ path("checkout") {
        // /cart/<UUID>/checkout
        post {
          // POST /cart/<UUID>/checkout
          val result = (cartRouter ? envelope(Checkout)).mapTo[Response]
          onComplete(result) {
            case Success(i: ItemsPurchased) => complete(i)
            case Success(v: ValidationErrors) => complete(v)
            case _ => complete(ServiceUnavailable)
          }
        }
      }
    }
}
