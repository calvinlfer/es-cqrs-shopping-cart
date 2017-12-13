package com.experiments.shopping.cart.domain

import cats.data.ValidatedNel
import cats.syntax.validated._

/**
  * Also known as a Command Processor
  */
trait CartValidator {
  def addItem(item: Item, existing: CartRepr): ValidatedNel[ValidationError, Unit] =
    if (existing.contains(item.productId)) ItemAlreadyInCart(item.productId).invalidNel
    else ().validNel

  def removeItem(productId: ProductId, existing: CartRepr): ValidatedNel[ValidationError, Unit] =
    if (!existing.contains(productId)) ItemNotInCart(productId).invalidNel
    else ().validNel

  def adjustQuantity(productId: ProductId, delta: Int, existing: CartRepr): ValidatedNel[ValidationError, Unit] =
    existing.get(productId) match {
      case None =>
        ItemNotInCart(productId).invalidNel

      case Some(Item(_, _, _, quantity)) if quantity + delta > 0 =>
        ().validNel

      case _ =>
        NegativeQuantity(productId).invalidNel
    }

  def checkout(existing: CartRepr): ValidatedNel[ValidationError, Unit] =
    if (existing.isEmpty) EmptyCartCheckout.invalidNel
    else ().validNel
}
