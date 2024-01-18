
# Order Preparation POC !

Here is a complete example of an Order Preparation domain.
> The same application exist in reactive mode (spring webflux and project reactor) under
> **order-preparation-poc-reactive**

## Use Case Explanation

Our application is designed to manage the preparation of orders in a click-and-collect retail environment.<br>
When a customer places an order online and chooses the click-and-collect option, the orderPreparation must be created in our system in a 'TO_PREPARE' state. 
The goal is to efficiently move the order through various steps until it's ready for the customer to pick up.<br>
The following states and actions define this journey:

- States:
    - TO_PREPARE: initial state
    - IN_PREPARATION: The order is actively being prepared, items are being picked
    - PENDING_PICKUP: Preparation is complete, and the order is waiting for the customer to collect it
    - DELIVERED: The customer has picked up the order, completing the process
- Actions:
    - START_PREPARATION  : start the preparation,transitioning to IN_PREPARATION state
    - PICK_ITEMS         : Select items for the order. This can occur multiple times. Once all items are picked, the order moves to 'PENDING_PICKUP'.
    - PICKUP             : The customer collects their order, transitioning it to the 'DELIVERED' state.
    - NOTIFY_OM (notify Order Manager): Automatically triggered when the order reaches the 'DELIVERED' state, informing the Order Manager that the order has been picked up.

### Process rules:
- Sequential Flow: The order must follow the sequence of 'START_PREPARATION' → 'PICK_ITEMS' → 'PICKUP'.
- Multiple Item Picking: 'PICK_ITEMS' can be executed several times as needed. Only when all items are ready does the order shift to 'PENDING_PICKUP'.
- Automatic Notification: Upon reaching 'DELIVERED', an automatic notification is sent to the Order Manager.
- Error Handling: In case of notification failure, the order enters 'RETRY_NOTIFICATION', where it attempts to notify the Order Manager again every 10 seconds, up to 3 retries. Success moves the order to a 'COMPLETE' state, while failure leads to an 'ERROR' state.

> check the **[in-store-workflow.json](src/main/resources/flow/in-store-workflow.json)** to understand how all these rules are configured

### Aggregate rules:

- Item Existence Check: The system checks that the items being picked are part of the original order.
- Quantity Verification: The quantity of each item picked cannot exceed the quantity specified in the order.

> check  **[OrderPreparation.java](src/main/java/com/progmod/poc/domain/OrderPreparation.java)** and **[Item.java](src/main/java/com/progmod/poc/domain/Item.java)**  to understand how these business invariants are coded

## Note on Repository:

The poc repository uses a built-in framework helper for using a postgres jsonb, where all the aggregate is saved in one 'data' column.<br>
check **[PocConfig.java](src/main/java/com/progmod/poc/PocConfig.java)** where we define the repository using the framework helper class
```java
@Bean
public FlowRepository<OrderPreparation, UUID> orderPostgresRepository() {
  return new BasePostgresJsonRepository<>(OrderPreparation.class)
        .setTableInfo("order_preparation", "data");
}
```
> It is up to you to define your aggregate repository, the only prerequisite is to implements the **FlowRepository**

## DataBase tables

we create 2 tables for this POC
- The Aggregate Table
```roomsql
CREATE TABLE IF NOT EXISTS poc.order_preparation (
	id uuid NOT NULL,
	data jsonb NOT NULL,
	CONSTRAINT order_preparation_pk PRIMARY KEY (id)
);
```

- Task Table used by the framework to handle async system tasks

```roomsql
CREATE TABLE IF NOT EXISTS poc.flow_task (
	id varchar NOT NULL,
	score int8 NOT NULL,
	status varchar NOT NULL,
	ver int4 NOT NULL,
	CONSTRAINT flow_task_pk PRIMARY KEY (id)
);
```

## Install and launch

- clone the repo 
```shell
git clone https://github.com/progmodEK/flow-driven-domain.git
```

- launch a postgres container
```shell
cd flow-driven-domain
mkdir -p ./tools/data/postgres-data
docker-compose up &
```

- launch the application
```shell
./gradlew :order-preparation-poc:bootRun
```


##  APIs collection

After starting the application, you can interact with the API running on port 8081.<br>

### Postman collection
> if you are using Postman, import this collection **./tools/POC.postman_collection**<br>
> and just use it

### Curl commands
> Replace 'ID' in the URL with the actual aggregate ID obtained after creating an order.

- create an order to prepare 
```shell
curl 'http://localhost:8081/pocs' \
--header 'Content-Type: application/json' \
--data '{
   "orderRef" : "orderXYZ",
   "items": [
        {
            "skuId": "123",
            "name": "sku123",
            "qty": 2
        },
        {
            "skuId": "456",
            "name": "sku456",
            "qty": 3
        }
   ]
}'
```

- start the preparation
```shell
curl --request POST 'http://localhost:8081/pocs/ID/start-preparation'
```

- pick items
```shell
curl 'http://localhost:8081/pocs/ID/pick-items' \
--header 'Content-Type: application/json' \
--data '{
    "pickItems" : [
        {
            "skuId" : "123",
            "qty": 2
        },
         {
            "skuId" : "456",
            "qty": 3
        }

    ]
}'
```

- pickup the order
```shell
curl --request POST 'http://localhost:8081/pocs/ID/pickup'
```

- view the Aggregate
```shell
curl 'http://localhost:8081/pocs/ID'
```

> Testing Scenarios:
>- Ensure you cant "pick items" before "start preparation" 
>- Verify you cant pick items more than the order's quantity
>- Verify that you can call multiple times "pick items" (all the api with less quantities and you will stay in the state IN_PREPARATION
>- check that after PICKUP invoked, there we will an async action invoked by the system to notify the Order manager (view the aggregate to check how it evolves)


## Example of a COMPLETE OrderPreparation

```json
{
  "id": "17e0deab-bcc3-43e0-a175-d6c0a1eb11fa",
  "orderRef": "orderXYZ",
  "items": [
    {
      "skuId": "123",
      "name": "sku123",
      "qty": 2,
      "qtyPrepared": 2,
      "pickedUp": true
    },
    {
      "skuId": "456",
      "name": "sku456",
      "qty": 3,
      "qtyPrepared": 3,
      "pickedUp": true
    }
  ],
  "state": "COMPLETED",
  "flow": {
    "expiresAt": null,
    "actions": [
      {
        "name": "START_PREPARATION",
        "type": "USER",
        "count": 2,
        "variables": {
          "transition": "success"
        },
        "executions": [
          {
            "executedAt": "2024-01-07T08:51:47.754002Z",
            "result": "success",
            "error": null,
            "fromState": "TO_PREPARE",
            "toState": "IN_PREPARATION"
          },
          {
            "executedAt": "2024-01-07T08:51:49.766903Z",
            "result": "error",
            "error": "InvalidActionException: Desired action 'START_PREPARATION' does not match current flow rules",
            "fromState": "IN_PREPARATION",
            "toState": "IN_PREPARATION"
          }
        ]
      },
      {
        "name": "PICK_ITEMS",
        "type": "USER",
        "count": 1,
        "variables": {
          "transition": "full"
        },
        "executions": [
          {
            "executedAt": "2024-01-07T08:51:52.109008Z",
            "result": "success",
            "error": null,
            "fromState": "IN_PREPARATION",
            "toState": "PENDING_PICKUP"
          }
        ]
      },
      {
        "name": "PICKUP",
        "type": "USER",
        "count": 1,
        "variables": {
          "transition": "success"
        },
        "executions": [
          {
            "executedAt": "2024-01-07T08:51:54.463242Z",
            "result": "success",
            "error": null,
            "fromState": "PENDING_PICKUP",
            "toState": "DELIVERED"
          }
        ]
      },
      {
        "name": "NOTIFY_OM",
        "type": "SYSTEM",
        "count": 2,
        "variables": {
          "transition": "success"
        },
        "executions": [
          {
            "executedAt": "2024-01-07T08:51:54.572989Z",
            "result": "error",
            "error": "DelegateException: notification error",
            "fromState": "DELIVERED",
            "toState": "RETRY_NOTIFICATION"
          },
          {
            "executedAt": "2024-01-07T08:52:04.697400Z",
            "result": "success",
            "error": null,
            "fromState": "RETRY_NOTIFICATION",
            "toState": "COMPLETED"
          }
        ]
      }
    ],
    "flowType": "DEFAULT",
    "eligibleActions": [],
    "variables": {}
  }
}
```

> Note that action that generates error are also traced, <br>
> for ex: START_PREPARATION was called twice, the second time it generates an error cause the order was already in IN_PREPARATION state
```json
      {
  "name": "START_PREPARATION",
  "type": "USER",
  "count": 2,
  "variables": {
    "transition": "success"
  },
  "executions": [
    {
      "executedAt": "2024-01-07T08:51:47.754002Z",
      "result": "success",
      "error": null,
      "fromState": "TO_PREPARE",
      "toState": "IN_PREPARATION"
    },
    {
      "executedAt": "2024-01-07T08:51:49.766903Z",
      "result": "error",
      "error": "InvalidActionException: Desired action 'START_PREPARATION' does not match current flow rules",
      "fromState": "IN_PREPARATION",
      "toState": "IN_PREPARATION"
    }
  ]
}
```

> Note also that NOTIFY_OM (the async automatic system action) was executed twice, first time was in error, the second time it transit to COMPLETED
```json
{
  "name": "NOTIFY_OM",
  "type": "SYSTEM",
  "count": 2,
  "variables": {
    "transition": "success"
  },
  "executions": [
    {
      "executedAt": "2024-01-07T08:51:54.572989Z",
      "result": "error",
      "error": "DelegateException: notification error",
      "fromState": "DELIVERED",
      "toState": "RETRY_NOTIFICATION"
    },
    {
      "executedAt": "2024-01-07T08:52:04.697400Z",
      "result": "success",
      "error": null,
      "fromState": "RETRY_NOTIFICATION",
      "toState": "COMPLETED"
    }
  ]
}
```
