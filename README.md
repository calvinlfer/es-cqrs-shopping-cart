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

Adjust the quantity of an existing item (you can use negative numbers to decrease) in your shopping cart you have selected:

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