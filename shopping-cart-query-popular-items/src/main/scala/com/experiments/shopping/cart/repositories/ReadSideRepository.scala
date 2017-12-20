package com.experiments.shopping.cart.repositories

import com.experiments.shopping.cart.repositories.internal.{
  ItemInformation,
  OffsetTrackingInformation,
  OffsetTrackingRepository,
  PopularItemsRepository
}

import scala.concurrent.Future

trait ReadSideRepository extends PopularItemsRepository with OffsetTrackingRepository {
  def update(info: ItemInformation,
             offset: OffsetTrackingInformation): Future[(ItemInformation, OffsetTrackingInformation)]
}
