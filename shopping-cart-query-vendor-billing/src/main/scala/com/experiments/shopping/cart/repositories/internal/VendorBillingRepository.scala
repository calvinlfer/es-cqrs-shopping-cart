package com.experiments.shopping.cart.repositories.internal

import java.util.UUID

import scala.concurrent.Future

case class VendorBillingInformation(vendorId: UUID, year: Int, month: Int, balance: BigDecimal)

trait VendorBillingRepository {
  def find(vendorId: UUID, year: Int, month: Int): Future[Option[VendorBillingInformation]]
}
