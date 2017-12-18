# Shopping Cart #

A application that uses Event Sourcing (ES) and Command Query Responsibility segregration (CQRS) to implement a shopping cart and provides a way to perform analytics. 
The command side is designed to provide shopping cart functionality to members and the different query sides are designed to provide analytics on member's shopping carts.

## Running the application ##
- Start up dependencies (Cassandra and ZooKeeper) with Docker Compose using `docker-compose up`

- Start application
  - __Command nodes__ require the following environment variables to be specified 
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)
    - `MANAGEMENT_PORT`: HTTP port that exposes cluster management (e.g. 19999)
  - `sbt command-side`

  - The __Query node__ requires the following environment variables to be specified
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)
  - `sbt query-side` 