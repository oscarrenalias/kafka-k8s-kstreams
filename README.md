# Pre-requisites

- Docker Desktop: https://www.docker.com/products/docker-desktop - Install and enable Kubernetes on it
- Kubectl: https://kubernetes.io/docs/tasks/tools/install-kubectl/
- Skaffold: https://skaffold.dev/docs/install/
- Java 11 or newer: https://jdk.java.net

# Stand up the environment

## Start dependencies

Start the component dependencies:

```
kubectl apply -f k8s/zookeeper.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/scylladb.yaml
kubectl apply -f k8s/schema-registry.yaml
```

## Create Kafka topics

```
kubectl exec kafka-0 -it -- \
    /usr/bin/kafka-topics --create --bootstrap-server kafka-0:9092 --topic data \
    --partitions 1 --replication-factor 1
```

```
kubectl exec kafka-0 -it -- \
    /usr/bin/kafka-topics --create --bootstrap-server kafka-0:9092 --topic mapped-data \
    --partitions 1 --replication-factor 1
```

Kafka is not running using a PV so this must be repeated every time the broker is restarted.

## Create Scylla keyspace

```
kubectl exec <scylladb-pod> -it -- cqlsh \
    -e "create keyspace topology with replication = {'class':'SimpleStrategy', 'replication_factor' : 1};" localhost
```

# Build and run the application

The application can be built and run using Gradle locally but it's designed to be run by Skaffold _in_ the local Kubernetes cluster as a container. The app's application.properties is pre-configured to find its dependencies in Kubernetes and will have to be updated accordingly to support other modes of execution.

Use skaffold to run the appication:

```
skaffold dev --port-forward=true
```

Skaffold will monitor the source code for changes and run the build process as needed. The application container is built using Jib connected to the local Docker daemon. The overall process to build the application, container image and publish the container should take about 20-30 seconds.

# HOWTOs

## Publish messages in the console

```
kubectl exec kafka-0 -it -- \
    /usr/bin/kafka-console-producer --broker-list kafka-0:9092 --topic data
```

## Consume messages in the console

```
kubectl exec kafka-0 -it -- \
    /usr/bin/kafka-console-consumer --bootstrap-server kafka-0:9092 --topic mapped-data
```

## Connect to ScyllaDB

```
kubectl exec <scylladb-pod> -it -- cqlsh localhost
```

# KSQL

## Topic set up

```
kubectl exec kafka-0 -it -- \
    /usr/bin/kafka-topics --create --bootstrap-server kafka-0:9092 --topic transactions-feed \
    --partitions 1 --replication-factor 1
```

## Connect to KSQL CLI

```
kubectl run --image confluentinc/cp-ksql-cli:5.4.0 ksql-cli -it -- http://ksql:8088
```

## Create topic
Run command to create a table based on the transaction-feed topic:

```
CREATE STREAM transactions
  (id VARCHAR, 
  customer_id VARCHAR, 
  account_from VARCHAR, 
  account_to VARCHAR, 
  amount BIGINT)
  WITH (KAFKA_TOPIC='transactions-feed', VALUE_FORMAT='JSON', key='id');
```

Query data as it is fed into the underlying stream:

```
SELECT id, customer_name FROM transactions EMIT CHANGES;
```

And feed some test data:

```
kubectl exec kafka-0 -i -- \
    /usr/bin/kafka-console-producer --broker-list kafka-0:9092 --topic transactions-feed \
    < kstreams/test_transactions.json
```

Create a table that aggregates transactions and amounts per user

```
CREATE TABLE transactions_per_user AS
    SELECT customer_id, COUNT(*)
    FROM transactions
    GROUP BY (customer_id)
    EMIT CHANGES;
```

And then query the table:

```
SELECT FROM transactions_per_user WHERE ROWKEY='customer-1';
```

## Transactions per minute

