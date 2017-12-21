# Shopping Cart #

A application that uses Event Sourcing (ES) and Command Query Responsibility segregration (CQRS) to implement a shopping cart and provides a way to perform analytics. 
The command side is designed to provide shopping cart functionality to members and the different query sides are designed to provide analytics on member's shopping carts.

## Running the application ##
- Start up dependencies (Cassandra and ZooKeeper) with Docker Compose using `docker-compose up`
- If you want to run any SQL based Query nodes, also start up PostgreSQL using 
`docker-compose -f pg-docker-compose.yaml up`

- Start application
  - __Command nodes__ require the following environment variables to be specified 
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)
    - `MANAGEMENT_PORT`: HTTP port that exposes cluster management (e.g. 19999)

  - The __Query node for Vendor Billing__ requires the following environment variables to be specified
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)
    
  - The __Query node for Popular Items__ requires the following environment variables to be specified
    - `HOST_IP`: IP of the host machine (e.g. 192.168.1.144 or 127.0.0.1)
    - `HOST_PORT`: Remoting port (e.g. 2552)

__Note:__ Make sure `HOST_PORT`s do not conflict between any nodes in the cluster