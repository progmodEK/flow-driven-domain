
# Flow Driven Domain Library
The library transform your Domain into a process-centric domain.
It means that it allows you to send ACTION to your domain, put rules over these action's transitions,
and code logic over each Action in a seperate delegate Class.
Your domain will have a STATE property and will have a Flow property containing the actions history (the process view)

## Why and when to Use 
Beyond basic business logic, domains often incorporate state-based rules.
When these state-dependent rules come into the picture, the domain's overall structure
becomes more complex over time, making both evolutionary changes and maintenance more difficult

Managing the state complexity often leads us to use state machines or business process management (BPM) tools.
While valuable, these tools operates as distinct units from the domain, leading to
  - potential inconsistencies
  - integration challenges
  - increased overhead
  - scattered business logic


## How to Use

Let's consider a HELLO WORLD example, where our domain is Greeting with a simple Value Object message as String

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

Now we want to add some process/flow over our Greeting domain to be able to produce "Hello World" greeting with 2 steps
 - HELLO: an Action that set the message to "Hello"
 - WORLD: an Action that set the message to "Hello World"
with a rule that WORLD action cant be executed without a previous HELLO action, plus if WORLD action is not executed during 30 seconds, the Greeting Domain will reset
and when WORLD is executed, the Greeting cant be changed anymore (reach a final State)

Lets explore how we can use the library to reach our goal

### Step1

Define ACTIONS, STATES and a process/flow based on these ENUMs. 
ex:

- ACTION ENUM


    public enum GreetingAction implements FlowAction {
        HELLO(USER),
        WORLD(USER),
        TIMEOUT(SYSTEM),
        private final ActionType type;    
    }

FlowAction is a Library interface, and "USER/SYSTEM" tells us if a system action (like TIMEOUT) or a user one.

- STATE ENUM


    public enum GreetingState implements FlowState {
        INITIAL,
        PENDING_COMPLETION,
        EXPIRED,
        COMPLETED
    }

INITIAL : initial state when we create our Greeting domain
PENDING_COMPLETION : intermediate state
EXPIRED, COMPLETED: final states where no more actions are acceptable

- FlowType ENUM


    public enum GreetinglowType implements FlowType {
        DEFAULT("default-flow.json", GreetingAction.class, GreetingState.class);
    
        @Getter
        private final String template;
        @Getter
        private final Class<? extends FlowAction> flowActionType;
        @Getter
        private final Class<? extends FlowState> flowStateType;
    
    }

This means that we define a DEFAULT flowType based on the enum we defined and on the default-flow.json file.


-  default-flow.json


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
                "delegate": "timeoutDelegate"
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

Explanation:<br>
When we create our Greeting Aggregate it will be in an INITIAL state.
Initial state have only 1 transition with the ACTION HELLO and transit to PENDING_COMPLETION.
PENDING_COMPLETION has 2 transitions, one with action WORLD and one with TIMEOUT (this one is automatice with a timer of 30sec),
means that while in PENDING_COMPLETION state, if no WORLD action is called, it will automatically transit to EXPIRED after 30 sec

### Step2 

Let your Domain Aggregate Implement the Flowable Interface

    @Data
    public class Greeting implements Flowable<UUID> {
        private UUID id;
        private String message;
        
        // ---------------  
        // here are the process elements to add to your domain
        // 
        private String state;
        private Flow flow;
        // --------------
    
        public void updateMessage(String message) {
            this.message = message;
        }
    
        public String getMessage() {
            return message;
        }
    }

Here we added 2 properties, State and Flow, these field will be used by the library
NB: you have also to take care about how to persist these property, if you are using NOSQL it will be straight forward,
    if not consider persisting Flow property as a String or Json

### Step3

Define an ActionDelegate for each Action, these delegate will have the process logic when the action is executed

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
    public class TimeoutDelegate implements ActionDelegate<Greeting, DelegateParams, Greeting> {
    
        @Override
        public Mono<UserStory> execute(final Greeting greeting, final Map<String, Object> variables, final DelegateParams delegateParams) {
            // here you can implement process related logic, ex call a API, etc..
            // and we invoke logic on our aggregate
            greeting.reset();
            return Mono.just(userStory);
        }
    
    }

### Step3

Instantiate a FlowEngine (the library flow engine) by passing it your Domain repository 

    @Bean
    FlowEngine<Greeting, UUID> flowEngine(DomainRepository repo) {
        return new FlowEngine<>(Long.class, Aggregate.class, repo);
    }

NB: your DomainRepository must extends FlowRepository interface (the library interface)
    this repository only contains the already used method in Sroing Repositories imlmentation

    public interface FlowRepository<T extends Flowable, ID> {
        Optional<T> findById(ID flowId);
        T save(T flowable);
    }

so if you are using Spring Data or Spring JPA these methods are already handled and all you have todo is for ex:

    public interface DomainRepository extends ReactiveCrudRepository<Greeting, UUID>, FlowRepository<Greeting, UUID> {
    }

or

    public interface DomainRepository extends JpaRepository<Greeting, UUID>, FlowRepository<Greeting, UUID> {
    
    }

### Step4

Watch your domain becomes flowable:

- invoke an ACTION


    flowEngine.applyAction(id, HELLO, inputParams) 

- check your domain aggregate


    {
        "id": "e30d2da8-f18e-4c43-bd84-bcffb726cb37",
        "message" : "Hello",
        "flow": {
            "flowType": "DEFAULT",
            "eligibleActions": [WORLD],
            "actions": [
                {
                    "name": “HELLO”,
                    "type": "USER",
                    "count": 1,
                    "executions": [
                        {
                        "executedAt": "2023-08-23T15:48:11.091511Z",
                        "result": "success",
                        "previousState": "INITIAL”,
                        "nextState": "PENDING_COMPLETE”
                        }
                    ]
                }
            ]
        }
        "state": "PENDING_COMPLETE”,
    }

we see here that we gain the process-centric view with the state and flow property.
we can understand when the HELLO action was executed and how the state arrived to PENDING_COMPLETE and we see too that the Greeting will expire in 30sec.

