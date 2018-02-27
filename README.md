# Shopping Cart #

A application that uses Event Sourcing (ES) and Command Query Responsibility segregation (CQRS) to implement a shopping 
cart and provides a way to perform analytics. The command side is designed to provide shopping cart functionality to 
members and the different query sides are designed to provide analytics on member's shopping carts. Please note that
the query nodes are not the views (UI) themselves but rather the components that populate the Query side's databases 
that the views would use to display data to the user.

## Running the application ##
- Start up dependencies (Cassandra and ZooKeeper) with Docker Compose using `docker-compose up`
- If you want to run any SQL based Query nodes, also start up PostgreSQL using 
`docker-compose -f pg-docker-compose.yaml up`

- Start application
  - __Command nodes__ require the following environment variables to be specified 
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)
    - `MANAGEMENT_PORT`: HTTP port that exposes cluster management (e.g. 19999)
    - `REST_HOST`: IP of the host machine (e.g. localhost, 127.0.0.1, etc.) to expose the REST API
    - `REST_PORT`: Port to expose the REST API (e.g. 9001)

  - The __Query node for Vendor Billing__ requires the following environment variables to be specified
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)
    
  - The __Query node for Popular Items__ requires the following environment variables to be specified
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)

__Note:__ Make sure that any ports do not conflict between any nodes in the cluster if you plan to run them on the same
machine

### Command nodes ###
The command nodes are responsible for providing the operational functionality of the shopping cart. This modules allows 
you to store items in a shopping cart and check out when you are done. If you go away and come back later, it will 
remember exactly what you have purchased thanks to Akka Persistence. This component is able to scale horizontally thanks
to Akka Cluster Sharding. You can interact with this component in two ways:

#### REST API ####
In order to place an item in the shopping cart (Shopping Cart: `9a475f59-8863-43cc-aebd-7da999c16bea`):

```POST http://localhost:9001/cart/9a475f59-8863-43cc-aebd-7da999c16bea```
```json
{
	"productId": "9054a277-9998-4bb4-be89-7d1ac45828d2",
	"vendorId": "fbea6379-b76c-478b-8f86-4f1626fb8acf",
	"name": "awesome-desktop-pc",
	"price": "3200",
	"quantity": 1
}
```

Removing an item (by Product ID: `9054a277-9998-4bb4-be89-7d1ac45828d2`) from the shopping cart:

```
DELETE http://localhost:9001/cart/9a475f59-8863-43cc-aebd-7da999c16bea/productId/9054a277-9998-4bb4-be89-7d1ac45828d2
```

Getting the contents of your shopping cart: 

```
GET http://localhost:9001/cart/9a475f59-8863-43cc-aebd-7da999c16bea
```

In order to checkout with the items you have in your shopping cart:

```
POST http://localhost:9001/cart/9a475f59-8863-43cc-aebd-7da999c16bea/checkout
```

This will clear your shopping cart.

#### Command line interface ####
Initially when this application was being created I wanted a quick way to try things out so I came up with a really 
simple way to communicate with the system to try things out. It will take care of generating UUIDs for the products 
based on the names that you use. You can type the following commands into the application:

Choose a shopping cart for a person:

```bash
change-member calvin
```

Add an item to the existing shopping cart you have selected:

```bash
add orange
```

Remove an item to the existing shopping cart you have selected:

```bash
remove orange
```

Adjust the quantity of an existing item (you can use negative numbers to decrease) in your shopping cart you have 
selected:

```bash
adjust orange 10
```

Checkout all the items in your existing cart:

```bash
checkout
```

Provide current information about the shopping cart (UUID):

```bash
current-member
```

Provide information about available commands:

```bash
help
```

### Query nodes ###
There are a variety of query components. The purpose of each query component is to demonstrate how to populate the 
database of a read-side view but not actually provide the UI functionality of the view. In a sense Query nodes are more
like hydrators as they provide data for the view side UI to consume and display in a way they see fit. There are three
queries that consume from the event journal directly and write to the read-side view in an exactly-once manner providing
transactional guarantees (`popular-items`, `vendor-billing`, `vendor-billing-jdbc`). `popular-items` and `vendor-billing`
consume from the journal and hydrate Cassandra tables whilst `vendor-billing-jdbc` hydrates a PostgreSQL table. Last 
but not least is the `item-purchased-events` hydrator which is responsible consuming purchased items from the journal 
and publishing those events to Kafka in an at-least-once fashion. The modules that consume data from the event journal 
and publish data to the read-side database directly make use of a offset tracking table where they record their progress
and update the data in a transactional manner. All query/hydrator components make use of this offset tracking table
but the `item-purchased-events` module cannot perform transactional writes since it updates two different systems 
(Cassandra for offset-tracking and Kafka for event publishing). Each query module is run as a Cluster Singleton and 
joins the same cluster as the command nodes in order to make use of some optimizations under the hood. You can run
multiple query nodes (of the same type) at the same time but they will operate in a active-passive manner and hand-off 
will occur when the active query node goes down.

We'll now examine each query/hydrator module:

#### `popular-items` ####
This module is responsible for tallying up the most popular items that were purchased for each day. It pulls events
from the event journal via Akka Persistence query and writes them to a Cassandra table.
```cql
CREATE TABLE item_quantity_by_day (
  vendorid uuid,
  productid uuid,
  year int,
  month int,
  day int,
  quantity int,
  name string
  PRIMARY KEY((vendorid, productid, year, month), day)
) WITH CLUSTERING ORDER BY (day ASC);
```

#### `vendor-billing` ####
This module is responsible for tallying up the most popular items that were purchased for each day. It pulls events
from the event journal via Akka Persistence query and writes them to a Cassandra table.
```cql
CREATE TABLE balance_by_vendor (
  vendorId uuid,
  year int,
  month int,
  balance decimal,
  PRIMARY KEY ((vendorId, year), month)
) WITH CLUSTERING ORDER BY (month DESC)
```

#### `vendor-billing-jdbc` ####
Performs the same function as `vendor-billing` except it writes to PostgreSQL instead of Cassandra.
```postgresql
CREATE TABLE vendor_billing
(
  vendor_id UUID           NOT NULL,
  year      INTEGER        NOT NULL,
  month     INTEGER        NOT NULL,
  balance   NUMERIC(21, 2) NOT NULL,
  CONSTRAINT "vendorId_year_month_pk"
  PRIMARY KEY (vendor_id, year, month)
);
```

#### `item-purchased-events` ####
This module is responsible for pulling all item-purchased events from the event journal and pushing them to a Kafka 
topic for consumption by further downstream services. The updates to Kafka have an at-least-once delivery guarantee so
duplicates can occur because we cannot guarantee transactions can happen as we use Cassandra to track journal offsets
and we publish data to Kafka separately.