
> The library is available in 
> - **reactive** functional programming (spring reactor) mode
> - **normal** mode (imperative programming)

# Flow Driven Domain: <em>make your domain flowable</em>
The library transform your Domain into a process-centric domain.<br>
It means that it allows you to send **ACTION** to your domain, put **rules** over these action's **transitions**,
and code logic over each Action in a **seperate delegate Class**<br>
Your domain will have a **State** property and will have a **Flow** property containing the actions history (the process view)

## Why and when to Use 
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


## How to Use <em>(Hello World ex)</em>

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

### Step1
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


### Step2

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

### Step3

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

### Step4

Define an ActionDelegate for each **Action**, these delegate will have the process logic when the action is executed
```java
@Component
public class HelloDelegate implements ActionDelegate<Greeting, DelegateParams, Greeting> {

    @Override
    public Mono<UserStory> execute(final Greeting greeting, final Map<String, Object> variables, final DelegateParams delegateParams) {
        // here you can implement process related logic, ex call a API, etc..
        // and we invoke logic on our aggregate
        greeting.updateMessage("Hello");
        return Mono.just(userStory);
    }

}

@Component
public class WorldDelegate implements ActionDelegate<Greeting, DelegateParams, Greeting> {

    @Override
    public Mono<UserStory> execute(final Greeting greeting, final Map<String, Object> variables, final DelegateParams delegateParams) {
        // here you can implement process related logic, ex call a API, etc..
        // and we invoke logic on our aggregate
        greeting.updateMessage("Hello World");
        return Mono.just(userStory);
    }

}

@Component
public class TimeoutDelegate implements SystemActionDelegate<Greeting> {

    @Override
    public Mono<Greeting> execute(final Greeting greeting, final Map<String, Object> variables) {
        // here you can implement process related logic, ex call a API, etc..
        // and we invoke logic on our aggregate
        greeting.reset();
        return Mono.just(userStory);
    }

}
```
> Each delegate must implement the generic **ActionDelegate<T extends Flowable, I, R>**<br>
-**T** type is your aggregate<br>
-**I** type is the input that you can pass to the delegate (a request object containing params)<br>
-**R** type is the return type (in our ex its the aggregate itself, but you can return a response from calling an external api for ex)<br>

> **Note**: for the system action **TIMEOUT" we implements **SystemActionDelegate<T extends Flowable>** cause here we cannot pass input params or return something else cause its invoked by the system


### Step5

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

### Step6

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

### Step7
Make your domain flowable and execute actions on your domain

- Make your Domain Flowabe
```java
Greeting greeting = 'create your Aggragte'
flowEngine.makeFlowable(greeting, ProductflowType.DEFAULT, Map.of());
```
> we can pass a MAP that will be saved to the variables property of the Flow if we want for later use
- invoke an ACTION
```java
flowEngine.applyAction(id, HELLO, inputParams) 
```
- and here you go, your domain will be process-centric (check the state and the flow properties)
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

