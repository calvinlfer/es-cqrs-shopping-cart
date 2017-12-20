package com.experiments.shopping.cart.repositories.internal

import java.util.UUID

import scala.concurrent.Future

case class ItemInformation(vendorId: UUID,
                           productId: UUID,
                           year: Int,
                           month: Int,
                           day: Int,
                           quantity: Int,
                           name: String)

trait PopularItemsRepository {
  def find(vendorId: UUID, productId: UUID, year: Int, month: Int, day: Int): Future[Option[ItemInformation]]
}
