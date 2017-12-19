package com.experiments.shopping.cart.repositories

import com.experiments.shopping.cart.repositories.internal._

import scala.concurrent.Future

trait ReadSideRepository extends VendorBillingRepository with OffsetTrackingRepository {
  def update(info: VendorBillingInformation,
             offset: OffsetTrackingInformation): Future[(VendorBillingInformation, OffsetTrackingInformation)]
}
