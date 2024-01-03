# Flow Driven Domain

Transform your Domain into a **process-centric domain**.<br>
The library enables you to send **actions** to your domain, put **rules** over these action's **transitions**,
and code logic over each action in a **seperate delegate** Class.<br>
Your domain will have a **State** property and will have a **Flow** property containing the actions history (the process view).

Two main purposes (2 points of vue):
-  **<em>make your domain flowable</em>**
-  **<em>develop your domain as a process</em>**

> Available in
> - **reactive** functional programming (spring reactor) mode
> - **normal** mode (imperative programming)

<br>

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Introduction](#introduction)
- [How to use <em>(Hello World ex)</em>](#how-to-use-emhello-world-exem)
  - [Step1 - library dependency](#step1---library-dependency)
  - [Step2 - Action, States and flow.json](#step2---action-states-and-flowjson)
  - [Step3 - Implement Flowable interface](#step3---implement-flowable-interface)
  - [Step4 - Implement delegates defined in flow.json](#step4---implement-delegates-defined-in-flowjson)
  - [Step5 - Your domain Repository and your DataBase](#step5---your-domain-repository-and-your-database)
    - [Implements the FlowRepository interface](#implements-the-flowrepository-interface)
    - [Create the Tasks Table](#create-the-tasks-table)
  - [Step6 - Create the FlowEngine](#step6---create-the-flowengine)
  - [Step7 - Make your domain flowable](#step7---make-your-domain-flowable)
    - [Make your Domain Flowabe](#make-your-domain-flowabe)
    - [Invoke Actions](#invoke-actions)
  - [Step8 - Listen and publish flow events](#step8---listen-and-publish-flow-events)
- [Library DDD Design](#library-ddd-design)
- [Flow Functionalities](#flow-functionalities)
  - [action](#action)
  - [state](#state)
  - [transition](#transition)
- [Advantages](#advantages)
- [Use Cases](#use-cases)
- [POC: Order Preparation](#poc-order-preparation)
- [POC: Reactive version](#poc-reactive-version)
- [Genesis and Achievement: Decathlon success story](#genesis-and-achievement-decathlon-success-story)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Introduction
Beyond basic business logic, domains often incorporate **state-based rules**.<br>
When these state-dependent rules come into the picture, the domain's overall structure
becomes **more complex over time**, making both evolutionary changes and maintenance more difficult

Managing the state complexity often leads us to use **state machines** or **business process management (BPM)** tools.<br>
While valuable, these tools operates as distinct units from the domain, leading to
  - potential inconsistencies
  - integration challenges
  - increased overhead
  - scattered business logic

When using flow-driven-domain, your domain becomes more process-centric:
 - **Flow rules becomes first-call citizen in your your domain**
 - **Flow history is kept inside your domain model**


## How to use <em>(Hello World ex)</em>

Let's consider a HELLO WORLD example, where your domain is Greeting Aggregate with a simple Value Object message as String
```java
  public class Greeting {
    private UUIID id;
    private String message;

    public void updateMessage(String message) {
        this.message = message;
    }

    public void reset() {
        this.message = "";
    }

    public String getMessage() {
        return message;
    }
}
```

Now lets imagine we want to add some process/flow over our Greeting domain to be able to produce "Hello World" greeting message using 2 steps/actions
 - **HELLO**: an Action that set the message to "Hello"
 - **WORLD**: an Action that set the message to "Hello World"

And imagine also we ave these rules:<br>
1- WORLD action cant be executed without a previous HELLO action<br>
2- after HELLO action, if WORLD action is not executed during 30 seconds, the Greeting Domain will reset<br>
3- after HELLO action, when WORLD is executed, the Greeting cant be changed anymore (reach a final State)

Lets explore how we can use the library to reach our goal.

### Step1 - library dependency
Add the library dependency, here is a gradle example:
```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/progmodEK/flow-friven-domain")
        credentials {
            username = "fdd-user"
            password = "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"
        }
    }
}

dependencies {
    implementation "com.progmod:flow-reactive:1.0.0"
}
```
> the **maven** part part we add in **repositories** is for security reasons, to be able to download the library.


### Step2 - Action, States and flow.json

Define ACTIONS, STATES and a process/flow based on these ENUMs.

- **ACTION ENUM**
```java
public enum GreetingAction implements FlowAction {
    HELLO(USER),
    WORLD(USER),
    TIMEOUT(SYSTEM);
    private final ActionType type;    
}
```

> USER and SYSTEM indicates if its a user action or system action (TIMEOUT is invoked by the system ).

- **STATE ENUM**
```java
public enum GreetingState implements FlowState {
    INITIAL,
    PENDING_COMPLETION,
    EXPIRED,
    COMPLETED
}
```
> INITIAL : initial state when we create our Greeting domain<br>
PENDING_COMPLETION : intermediate state<br>
EXPIRED, COMPLETED: final states where no more actions are acceptable

- **FlowType ENUM**
```java
public enum GreetinglowType implements FlowType {
    DEFAULT("default-flow.json", GreetingAction.class, GreetingState.class);

    @Getter
    private final String template;
    @Getter
    private final Class<? extends FlowAction> flowActionType;
    @Getter
    private final Class<? extends FlowState> flowStateType;

}
```
> This means that we define a DEFAULT flowType based on GreetingAction and GreetingState enums, and a default-flow.json file.

-  **Flow JSON file**, default-flow.json in our case
```json
{
    "actions": [
        {
            "name": "HELLO",
            "delegate": "helloDelegate"
        },
        {
            "name": "WORLD",
            "delegate": "worldDelegate"
        },
        {
            "name": "TIMEOUT",
            "delegate": "timeoutDelegate",
            "expiration": true
        }
    ],
    "states": [
        {
            "name": "INITIAL",
            "initial": true,
            "transitions": [
                {
                    "action": "HELLO",
                    "internal": "PENDING_COMPLETION"
                }
            ]
        },
        {
            "name": "PENDING_COMPLETION",
            "transitions": [
                {
                    "action": "WORLD",
                    "result": {
                        "success": "COMPLETED"
                    }
                },
                {
                    "action": "TIMEOUT",
                    "result": {
                        "success": "EXPIRED"
                    },
                    "timer": {
                        "sec": 30
                    }
                }
            ]
        },
        {
            "name": "EXPIRED",
            "transitions": []
        },
        {
            "name": "COMPLETED",
            "transitions": []
        }
    ]
} 
```
> Explanations:<br>
--We list the ACTIONS with their associated delegates (these delegates will be invoked when the action is invoked)<br>
--**TIMEOUT** action have also "expiration=true", this tells that this action is used for expiration and the system will be able to compute "expiresAt" automatically based on the state you currently are.<br>
--**INITIAL** state has "initial=true", means when we create our Greeting Aggregate it will be in an **INITIAL** state.<br>
--**INITIAL** state have only 1 transition with the action **HELL**O and transit to **PENDING_COMPLETION** state.<br>
--**PENDING_COMPLETION** has 2 transitions, one with action **WORLD** that transit to **COMPLETED** state, and one with **TIMEOUT** (this one is automatic with a **timer** of 30sec),
means that while in **PENDING_COMPLETION** state, if no **WORLD** action is called, it will automatically transit to **EXPIRED** after 30 sec

### Step3 - Implement Flowable interface

Let your Domain Aggregate Implement the Flowable Interface
```java
@Data
public class Greeting implements Flowable<UUID> {
    private UUID id;
    private String message;
    
    // -----------------------------------------------------  
    // here are the flow elements to add to your domain
    // remember that your domain becomes process-centric, 
    // means it holds the flow history
    private String state;
    private Flow flow;
    // -----------------------------------------------------

    public void updateMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
```
> **NB**: you have to persist these property in your repository<br>
  if you are using NOSQL it will be straight forward,<br>
  if not consider persisting Flow property as a String or Json

### Step4 - Implement delegates defined in flow.json

Define an ActionDelegate for each **Action**, these delegate will have the process logic when the action is executed
```java
@Component
public class HelloDelegate implements ActionDelegate<Greeting, DelegateParams, Greeting> {

    @Override
    public Mono<UserStory> execute(final Greeting greeting, final Map<String, Object> variables, final DelegateParams delegateParams) {
        // here you can implement process related logic, ex call a API, etc..
        // and we invoke logic on our aggregate
        greeting.updateMessage("Hello");
        return Mono.just(greeting);
    }

}

@Component
public class WorldDelegate implements ActionDelegate<Greeting, DelegateParams, Greeting> {

    @Override
    public Greeting execute(final Greeting greeting, final Map<String, Object> variables, final DelegateParams delegateParams) {
        // here you can implement process related logic, ex call a API, etc..
        // and we invoke logic on our aggregate
        greeting.updateMessage("Hello World");
        return greeting;
    }

}

@Component
public class TimeoutDelegate implements SystemActionDelegate<Greeting> {

    @Override
    public Greeting execute(final Greeting greeting, final Map<String, Object> variables) {
        // here you can implement process related logic, ex call a API, etc..
        // and we invoke logic on our aggregate
        greeting.reset();
        return greeting;
    }

}
```
> Each delegate must implement the generic **ActionDelegate<T extends Flowable, I, R>**<br>
-**T** type is your aggregate<br>
-**I** type is the input that you can pass to the delegate (a request object containing params)<br>
-**R** type is the return type (in our ex its the aggregate itself, but you can return a response from calling an external api for ex)<br>

> **Note**: for the system action **TIMEOUT" we implements **SystemActionDelegate<T extends Flowable>** cause here we cannot pass input params or return something else cause its invoked by the system


### Step5 - Your domain Repository and your DataBase

####  Implements the FlowRepository interface
Let your **DomainRepository**  extends this **FlowRepository** interface<br>

```java
public interface FlowRepository<T extends Flowable, ID> {
  Optional<T> findById(ID flowId);
  T save(T flowable);
}
```
> This interface contains only the already used method in Spring Repositories so it should be straight froward.<br>
Means if you are using Spring Data or Spring JPA these methods are already handled

ex for reactive library :
```java
public interface DomainRepository extends ReactiveCrudRepository<Greeting, UUID>, FlowRepository<Greeting, UUID> {
}
```
or JPA
```java
public interface DomainRepository extends JpaRepository<Greeting, UUID>, FlowRepository<Greeting, UUID> {
}
```

> the Library includes a Base FlowRepository implementation in case you are using postgres jsonb
> so you can use it to define your domain repository as follow:
```java
@Bean
public FlowRepository<Greeting, UUID> greetingPostGresRepository() {
        return new BaseR2dbcPostgresJsonRepository<>(Greeting.class)
        .setTableInfo(table_name, jsonb_column);
        }
```

#### Create Your Domain DB table

> the **Flow** property should always be in JSON format
> Best Practice: use JSONB if you are using postgres

Following are some example using postgres JSONB<br>
If you are using JSONB for all your aggregate, your table would be
```roomsql
CREATE TABLE IF NOT EXISTS greeting (
  id uuid NOT NULL,
  greeting_data jsonb NOT NULL,
  CONSTRAINT flow_pk PRIMARY KEY (id)
);
```

If you are using JSONB only for the **Flow** property of your aggregate, your table would be
```roomsql
CREATE TABLE IF NOT EXISTS greeting (
  id uuid NOT NULL,
  message varchar NOT NULL,
  flow_data jsonb NOT NULL,
  CONSTRAINT flow_pk PRIMARY KEY (id)
);
```

> you can off course use a DB other than postgresql

#### Create the Tasks Table
create a table where scheduled tasks will be saved (flow capabilities like timeouts or other scheduled automatic tasks)
ex for postgres
```roomsql
CREATE TABLE IF NOT EXISTS flow_task (
    id varchar NOT NULL,
    score int8 NOT NULL,
    status VARCHAR(255) NOT NULL,
    ver INTEGER NOT NULL,
    PRIMARY KEY (id)
);
```
> Replace the types by your specific database types (ex VARCHAR(255), BIGINT, INTEGER)

### Step6 - Create the FlowEngine

Instantiate a FlowEngine (the library flow engine) by passing it:<br>
-**ID class type** of your Aggregate<br>
-**Aggragte  class type**<br>
-**Repository** of youre domain
```java
@Bean
FlowEngine<Greeting, UUID> flowEngine(DomainRepository repo) {
    return new FlowEngine<>(UUID.class, Greeting.class, repo);
}
```

### Step7 - Make your domain flowable
Make your domain flowable and execute actions on your domain

####  Make your Domain Flowabe
```java
Greeting greeting = 'create your Aggragte'
flowEngine.makeFlowable(greeting, ProductflowType.DEFAULT, Map.of());
```
> we can pass a MAP that will be saved to the variables property of the Flow if we want for later use
####  Invoke Actions
```java
flowEngine.applyAction(id, HELLO, inputParams) 
```
and here you go, your domain will be process-centric (check the state and the flow properties)
```json
{
    "id": "e30d2da8-f18e-4c43-bd84-bcffb726cb37",
    "message" : "Hello",
    "flow": {
        "flowType": "DEFAULT",
        "eligibleActions": ["WORLD"],
        "actions": [
            {
                "name": "HELLO",
                "type": "USER",
                "count": 1,
                "executions": [
                    {
                    "executedAt": "2023-08-23T15:48:11.091511Z",
                    "result": "success",
                    "previousState": "INITIAL",
                    "nextState": "PENDING_COMPLETE"
                    }
                ]
            }
        ]
    },
    "state": "PENDING_COMPLETE"
}
```
> we can see when the **HELLO** action was executed and how the state arrived to **PENDING_COMPLETE** and we see too that the Greeting will expire in 30sec with the **expiresAt** property

### Step8 - Listen and publish flow events

Each executed **Action** is considered as a **flow Event**.<br> 
All flow events are pushed to **EventsPublishers**, you can define your own Event Publishers.<br>
ex:
```java
@Component
@Slf4j
public class KafkaEventPublisher implements EventsPublisher {
  @Override
  public void publishEvents(final Flowable aggregate) {
    log.info("PUBLISH EVENTS TO KAFKA IF YOU WANT");
  }
}
```

> The Library includes an ActionLogger as an EventsPublisher which logs all the executedAction.<br>
> You can define your own Logger as EventsPublisher and disable the library one


## Library DDD Design

The library follows DDD principles.<br>
- Your Aggregate (your principal entity), is at the heart of the domain
- The library flowEngine acts as the Application layer of your domain
- The flow related mechanism (actions eligibility, transitions, etc..) is coded as a Domain Service<br>

> <span style="color: blue;">BLUE</span>  : Library Components<br>
> <span style="color: green;">GREEN</span>  : Your components


![Local Image](./fdd-design.png)

> the **Port/Interface** part that links the domain to the infra layer is not shown to simplify the diagram<br>

## Flow Functionalities

We saw in HelloWorld example some features of the **flow.json** when defining actions, states and transitions.<br>
Here is a List of most of the features and how to use them

### action

we can use these properties on each **action** defined in the **JSON** file
 - **delegate**: the spring bean (implementation of an ActionDelegate) that will be invoked 
 - **expiration**: ture/false(default) used to compute **expiredAt** functionality (inside the **flow** property of the aggregate) 

```json
{
  "name": "TIMEOUT",
  "delegate": "timeoutDelegate",
  "expiration": true
}
```

### state

we can use these properties on each **state** defined in the **JSON** file
- **initial**: ture/false(default) to indicate that it will be the INITIAL state of the aggregate<br>
Remember how we make the aggregate flowable
```java
Greeting greeting = 'create your Aggragte'
flowEngine.makeFlowable(greeting, ProductflowType.DEFAULT, Map.of());
```

```json
{
  "name": "INITIAL",
  "initial": true,
}
```

### transition

we can use these properties on each **transition** defined in the **JSON** file
- **internal**: indicates that the transition is internal, means the state will remains the same
- **dependsOn**: indicates that the transition depends on a preceding action. normally used with **internal** transitions
              ex: if we have 3 internal transition with A1,A2 and A3, we can say that the 2nd transition using A2  dependsOn A1
- **result**: this String based map indicates where to transit based on functional results
- **timer**: indicates that the action will be automatically executed after x sec
- **exceptions**: indicates where to transit based on exceptions (exceptions can be launched from action delegates using the **DelegateException**)
- **retry**: number of retries (in case of exceptions) before transiting to another state

```json
"transitions": [
  {
    "action": "APPROVE",
    "result": {
      "success": "APPROVED",
      "error": "NOT_APPROVED"
    },
    "exceptions": {
      "001": "PENDING_APPROVAL"
    },
    "retry": {
      "number": 3,
      "exceeded": "NOT_APPROVED"
    },
    "timer": {
      "sec": 30
    }
  }
]
```

> **APPROVE** action will be launched in **30sec** (**timer**)<br>
> if the **functional** result (fom the actionDelegate) is **success** then transit to **APPROVED**<br>
> If the **functional** result (fom the actionDelegate) is **error** then transit to **NOT_APPROVED**<br>
> if an **exception** is thrown (**technical** exception from the delegate) then transit to **PENDING_APPROVAL**<br>
> in case of other *$exceptions** (not handled by the exceptions part) occurred then stay in same state, but after **3 retries**  transit to **NOT_APPROVED**<br>


## Advantages

logging, monitoring, objects in your Domain (vs variables in BPMs), etc..

## Use Cases

checkout, order preparation, etc..

## POC: Order Preparation

## POC: Reactive version

## Genesis and Achievement: Decathlon success story

The foundational **concept of the library** was originally crafted for **Decathlon**.<br>
It was specifically tailored to address their **checkout process** needs and challenges.<br>

This concept, a testament to our innovative approach, has been successfully implemented within Decathlon, demonstrating its effectiveness and reliability in a real-world, enterprise environment.<br>

Building on this initial success, the concept was further evolved into a full-fledged library,
guided by principles of Domain-Driven Design (DDD), ensuring a robust and scalable architecture.


