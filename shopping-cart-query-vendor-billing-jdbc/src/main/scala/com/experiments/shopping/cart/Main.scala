package com.experiments.shopping.cart

import java.util.UUID

import com.experiments.shopping.cart.repositories.internal.{
  OffsetTrackingTable,
  VendorBillingInformationRow,
  VendorBillingTable
}
import slick.jdbc.PostgresProfile.api._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  val config = ConfigFactory.load()
  val database = Database.forConfig("db")
  println(s"Connecting to ${config.getString("db.url")}")
  val vendorBillingQuery: TableQuery[VendorBillingTable] = TableQuery(
    VendorBillingTable(tableName = "vendor_billing", tableSchema = "shopping_cart")
  )
  val offsetTrackingQuery: TableQuery[OffsetTrackingTable] = TableQuery(
    OffsetTrackingTable(tableName = "offset_tracking", tableSchema = "shopping_cart")
  )

//  println(vendorBillingQuery.schema.create.statements.mkString)
//  println {
//    Await.result(database.run(vendorBillingQuery.schema.create), 30.seconds)
//  }

  val query = vendorBillingQuery.insertOrUpdate(
    VendorBillingInformationRow(UUID.fromString("79fcc2f3-b8cc-4b24-8428-63aa7bbed1d0"), 2017, 12, 199.18)
  ) andThen
    (vendorBillingQuery += VendorBillingInformationRow(UUID.randomUUID(), 2017, 12, 199.18))

  println {
    Await.result(database.run(query.transactionally), 10.seconds)
  }
}
