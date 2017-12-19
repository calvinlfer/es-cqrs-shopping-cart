package com.experiments.shopping.cart.repositories.internal

import java.util.UUID

import scala.concurrent.Future

case class OffsetTrackingInformation(hydratorId: String, tag: String, offset: UUID)

trait OffsetTrackingRepository {
  def find(hydratorId: String, tag: String): Future[Option[OffsetTrackingInformation]]
}
