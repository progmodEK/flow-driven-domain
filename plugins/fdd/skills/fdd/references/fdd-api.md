# FDD Library API Reference (non-reactive, `io.github.progmodek:flow:1.1.0`)

Everything a generated consumer app needs to know about the library's public surface.
Package root throughout: `com.progmod.flow`. Stack: Spring Boot 4.1.0, Java 25 (bytecode 17
compatible), Postgres. The library brings `flow-common` transitively (`api` dependency), so
depending on `io.github.progmodek:flow:1.1.0` is enough.

## Exact imports (copy these — do NOT guess the packages)

The single most common way a generated FDD app fails to compile is guessing wrong package paths for
the framework types. The interfaces are spread across several packages that are **not** where you'd
intuitively expect (e.g. `FlowType` is *not* in `domain.model`, and `ActionDelegate` is *not* in
`domain.delegate`). Use exactly these fully-qualified names:

```java
// --- enum contracts: parser.definition, NOT domain.model ---
import com.progmod.flow.domain.service.parser.definition.FlowAction;
import com.progmod.flow.domain.service.parser.definition.FlowState;
import com.progmod.flow.domain.service.parser.definition.FlowType;

// --- action type + static USER/SYSTEM: domain.model ---
import com.progmod.flow.domain.model.ActionType;
import static com.progmod.flow.domain.model.ActionType.USER;
import static com.progmod.flow.domain.model.ActionType.SYSTEM;

// --- aggregate model: domain.model ---
import com.progmod.flow.domain.model.Flowable;
import com.progmod.flow.domain.model.Flow;
import com.progmod.flow.domain.model.BaseFlow;          // BaseFlow mode only
import com.progmod.flow.domain.model.ActionExecuted;     // built-in domain event (read in an EventsPublisher)

// --- delegates: domain.service.delegate, NOT domain.delegate ---
import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import com.progmod.flow.domain.service.delegate.EmptyParams;   // only if referenced directly

// --- engine, ports, exception, persistence, utils ---
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.port.EventsPublisher;
import com.progmod.flow.domain.service.DelegateException;
import com.progmod.flow.infra.postgres.BasePostgresJsonRepository;
import static com.progmod.flow.utils.FlowUtils.ACTION_TRANSITION_VARIABLE;   // == "transition"
```

These are verified against the published `flow:1.1.0` jar and the reference POC. After generating,
if `compileJava` reports `package ... does not exist`, it is almost always one of these — fix the
import to the FQN above rather than adding a dependency.

## Table of contents
1. The two usage models (Flowable-direct vs BaseFlow)
2. `Flowable<ID>` interface
3. `Flow` / `Action` history model
4. Action / State / FlowType enum contracts
5. `ActionDelegate` + `SystemActionDelegate` + `DelegateException`
6. `FlowEngine` — the application entry point
7. `FlowRepository` + `BasePostgresJsonRepository`
8. Auto-configuration, required annotations, `EventsPublisher`
9. Required database tables
10. Gotchas / rules that are easy to get wrong

---

## 1. The two usage models

**(a) Flowable-direct** — your rich domain aggregate class `implements Flowable<ID>` and carries
`id`, `state`, `flow` **plus** its own business fields and invariants. You build instances yourself
and call `flowEngine.makeFlowable(aggregate, flowType, vars)` to initialise the process on an
already-built object. Delegates are typed `ActionDelegate<YourAggregate, I, R>`. This is what the
order-preparation POC and the Hello-World tutorial use. Use it when there is real domain data +
invariants to protect. See `flowable-direct-example.md`.

**(b) BaseFlow** — no aggregate class at all. You use the library's `BaseFlow` (id type is always
`String`, a random UUID) as a pure workflow instance whose only data is a `variables` map. You
create instances with `flowEngine.buildBaseFlow(flowType, vars)`. Delegates are typed
`ActionDelegate<BaseFlow, I, R>`. Use it when the "aggregate" is essentially just a workflow
instance driven by an external process, with no rich domain model. See `baseflow-example.md`.

`BaseFlow` itself:
```java
public class BaseFlow extends Flow implements Flowable<String> {
  private String id = UUID.randomUUID().toString();
  private Instant createdAt = Instant.now();
  private String state;
  @Override public Flow getFlow() { return this; }   // it IS its own Flow
}
```
Because `BaseFlow extends Flow`, in the persisted JSON its `state`, `flowType`, `actions`,
`eligibleActions`, and `variables` all live at the **top level** — there is no nested `flow` node.
In the Flowable-direct model they are nested under a `flow` property alongside business fields.

---

## 2. `Flowable<ID>` interface (in `flow-common`)

The **entire** contract an aggregate must satisfy:
```java
public interface Flowable<ID> {
  ID getId();
  String getState();
  void setState(String state);
  Flow getFlow();
  void setFlow(Flow flow);
}
```
No default methods. Persist all three pieces of data: the id, the `state` string, and the whole
`Flow` object (as JSON/jsonb). With Lombok `@Data` the getters/setters are generated for you.

---

## 3. `Flow` / `Action` history model

`Flow` is the process container serialized under the `flow` JSON property (or at top level for
BaseFlow). Persisted fields: `expiresAt`, `List<Action> actions` (per-action execution history +
variables + count), `flowType` (String), `List<String> eligibleActions`, `Map<String,Object>
variables` (global vars). Two runtime-only `@JsonIgnore` buffers exist (`asyncTasks`, `events`).
Helpers you may call: `getGlobalVariable(key, Class)`, `getActionVariable(...)`,
`getMatchingAction(FlowAction)`, `addEvent(...)`.

Per-action history (`Action`): `name`, `type` (`USER`/`SYSTEM`), `count`, `Map<String,Object>
variables`, and a capped `LinkedList<Execution> executions` (last 5). Each `Execution` records
`executedAt`, `result`, `error`, `fromState`, `toState` — this is the built-in audit trail.

---

## 4. Action / State / FlowType enum contracts (in `flow-common`)

```java
public interface FlowAction { String name(); ActionType getType(); }  // name() free from enum
public interface FlowState  { String name(); }                        // name() free from enum
public interface FlowType {
  String name();
  String getTemplate();                          // classpath path to the workflow JSON
  Class<? extends FlowAction> getFlowActionType();
  Class<? extends FlowState>  getFlowStateType();
}
public enum ActionType { USER, SYSTEM }
```
- **USER** actions are invoked via the controller / `applyAction`.
- **SYSTEM** actions are only runnable by the async task consumer (timers/retries); they are
  filtered out of `getEligibleActions`. A SYSTEM action needs a `timer` on the transition that
  targets it so the engine schedules it.

Canonical enum implementations:
```java
@Getter @RequiredArgsConstructor
public enum OrderPreparationAction implements FlowAction {
  START_PREPARATION(USER), PICK_ITEMS(USER), PICKUP(USER), NOTIFY_OM(SYSTEM);
  private final ActionType type;   // @Getter supplies getType()
}

@Getter
public enum OrderPreparationState implements FlowState {
  TO_PREPARE, IN_PREPARATION, PENDING_PICKUP, DELIVERED, RETRY_NOTIFICATION, COMPLETED, ERROR
}

@AllArgsConstructor
public enum OrderPreparationFlowType implements FlowType {
  DEFAULT("flow/in-store-workflow.json", OrderPreparationAction.class, OrderPreparationState.class);
  @Getter private final String template;
  @Getter private final Class<? extends FlowAction> flowActionType;
  @Getter private final Class<? extends FlowState> flowStateType;
}
```
`getTemplate()` is a classpath resource path (loaded by the default `ClassLoaderResourceParser`).
One `FlowType` enum can declare several workflows (e.g. `DEFAULT`, `EXPRESS`), each pointing at a
different JSON; the engine builds one flow definition per constant at startup.

---

## 5. `ActionDelegate`, `SystemActionDelegate`, `DelegateException`

One `@Component` bean per action. The engine resolves the delegate by the **Spring bean name**
declared as `delegate` in the JSON.

```java
public interface ActionDelegate<T extends Flowable, I, R> {
  R execute(T flowable, Map<String, Object> actionContext, I inputParams);
}
```
- `T` = your aggregate (or `BaseFlow`), `I` = input params type passed to `applyAction`, `R` = the
  value surfaced back through `applyAction` (often the aggregate itself, or an external response).
- `actionContext` is the **per-action variables map**. To drive a multi-branch transition, write
  the reserved key into it; the string is matched against the transition's `result` map keys:
```java
import static com.progmod.flow.utils.FlowUtils.ACTION_TRANSITION_VARIABLE; // == "transition"
actionContext.put(ACTION_TRANSITION_VARIABLE, fullyPrepared ? "full" : "partial");
```
  If a delegate writes nothing, the engine defaults the branch to `"success"`.

SYSTEM actions extend a convenience base (no input params, aggregate as return type):
```java
public abstract class SystemActionDelegate<T extends Flowable>
    implements ActionDelegate<T, EmptyParams, T> {
  public abstract T execute(T flowable, Map<String,Object> actionContext);
}
```

Signal a business failure / trigger a retry or exception-transition by throwing:
```java
throw new DelegateException(1, "notification error");
```
The `int` code is matched against the transition's `exceptions` map (JSON key `"001"` deserializes
to Integer `1`).

**Bean-name rule (critical):** the JSON `delegate` string must equal the delegate bean name =
class name with the first letter lowercased. `PickItemsDelegate` → bean `pickItemsDelegate`.
Give delegate classes normal PascalCase names and reference the lowerCamel name in the JSON.

---

## 6. `FlowEngine` — application entry point

Generic over `T extends Flowable` and id type `ID`. Must be a Spring `@Bean` (it field-injects
framework collaborators). Constructor:
```java
public FlowEngine(Class<ID> idType,
                  Class<? extends FlowType> flowTypeEnum,
                  FlowRepository<T, ID> flowRepository)
```
Only `String`, `Long`, `UUID` are supported id types.

Key methods:
```java
// (Flowable-direct) initialise an existing aggregate into a flow, then save it
T makeFlowable(T flowable, FlowType flowType, Map<String,Object> variables)

// (BaseFlow) create a bare BaseFlow instance, then save it
BaseFlow buildBaseFlow(FlowType flowType, Map<String,Object> variables)

// apply an action — main runtime call; returns the delegate's R
<I,R> R applyAction(ID flowId, FlowAction action, I delegateParams)
<I,R> R applyAction(T flowable, FlowAction action, I delegateParams)
<I,R> R applyActionWithStringId(String flowId, FlowAction action, I delegateParams)

List<FlowAction> getEligibleActions(Flowable<ID> flowable)
FlowDefinition   getFlowDefinitionByType(FlowType flowType)
```
`applyAction(id, action, params)` loads via `findById` (throws `FlowableNotFoundException` if
absent), runs the transition + delegate, saves, schedules/removes async tasks, and publishes
events. It is `@Transactional(noRollbackFor = FlowException.class)` — a business/flow error is
**persisted** (recorded on the aggregate's history) rather than rolled back.

Typical controller usage:
```java
flowEngine.makeFlowable(orderPreparation, OrderPreparationFlowType.DEFAULT, Map.of());
flowEngine.<PickItemsRequest, OrderPreparation>applyAction(
    UUID.fromString(id), OrderPreparationAction.PICK_ITEMS, pickItemsRequest);
```

---

## 7. `FlowRepository` + `BasePostgresJsonRepository`

Port:
```java
public interface FlowRepository<T extends Flowable, ID> {
  Optional<T> findById(ID flowId);
  T save(T flowable);
}
```
Default Postgres jsonb implementation (stores the **whole aggregate as one jsonb column**,
upsert semantics, id column hard-coded to `id`):
```java
@Bean
public FlowRepository<OrderPreparation, UUID> orderPostgresRepository() {
  return new BasePostgresJsonRepository<>(OrderPreparation.class)
      .setTableInfo("order_preparation", "data");   // table name, jsonb column name
}
```
It field-injects Spring's `JdbcTemplate` and a Jackson `ObjectMapper`, so it must be a bean.
Any custom `FlowRepository<T,ID>` bean works too (JPA, Spring Data, etc.) — jsonb is just the
built-in convenience. The default persistence for generated apps is this Postgres-jsonb helper.

---

## 8. Auto-configuration, annotations, events

- **No `@Enable...` annotation is needed** — having the `flow` jar on the classpath triggers
  `FlowAutoConfiguration`, which registers `FlowConfiguration`, the `FlowDefinitionParser`
  (`ClassLoaderResourceParser`), the `TaskRepository`/`DatabaseTaskScheduler`/`LockService`, the
  `EventPublisherRegistry`, and a default action logger.
- The main class needs `@SpringBootApplication` + `@EnableScheduling` (the task consumer polls
  `flow_task` on a schedule). `@EnableTransactionManagement` is **not** required — Spring Boot's
  auto transaction management plus the engine's own `@Transactional` suffice (the POC leaves it
  commented out).
- What the consumer must still declare: the `FlowRepository` bean, the `FlowEngine` bean, and each
  `@Component ActionDelegate`.
- **Listening to domain events = implement `EventsPublisher`.** `publishEvents(Flowable)` is the SPI
  the engine calls after *every* action (right after it saves the aggregate).
  `EventPublisherRegistry` injects `List<EventsPublisher>` — Spring collects **all** beans
  implementing the interface and hands each the flow — so registering a new sink is just adding a
  `@Component`, with no config and no `@Enable`. Multiple sinks coexist and run independently; the
  library's own `FlowActionLogger` is exactly one such bean. Read the events off
  `flowable.getFlow().getEvents()` and filter to the `FlowEvent` types you care about — the engine
  emits an `ActionExecuted` per action (`action` / `fromState` / `toState` / `isErrorOccurs` /
  `errorMessage`), and delegates may add custom `FlowEvent`s via `flowable.addEvent(...)`. It runs
  **synchronously inside the action's transaction**, so keep a listener quick and safe: a throw rolls
  the action back (use Spring's `@TransactionalEventListener` if you need after-commit fan-out).
  Generated apps always include one working sample sink — the `LoggingEventPublisher` in the worked
  example — whose body is the single swap-point for a real destination (Kafka, HTTP, an outbox, ...).
- Config props (all optional, sensible defaults) under `flow.*`:
  `flow.actionLogger.enabled`, `flow.taskConsumer.scheduleMilli`, `flow.taskConsumer.batch`,
  `flow.taskConsumer.concurrency`, `flow.lockService.lockTimeoutSec`,
  `flow.lockService.removeBlockedLockScheduleMilli`.

---

## 9. Required database tables (Postgres, via Flyway)

Two tables minimum, created by the consumer's Flyway migration
(`src/main/resources/db/migration/V1__*.sql`):

```sql
-- aggregate table: id column is named `id`, jsonb column name matches setTableInfo(...)
CREATE TABLE IF NOT EXISTS <schema>.<aggregate_table> (
  id <uuid|varchar|bigint> NOT NULL,
  <jsonb_column> jsonb NOT NULL,
  CONSTRAINT <aggregate_table>_pk PRIMARY KEY (id)
);

-- framework task table (MANDATORY whenever any timer/system/async action exists)
CREATE TABLE IF NOT EXISTS <schema>.flow_task (
  id varchar NOT NULL,
  score int8 NOT NULL,
  status varchar NOT NULL,
  ver int4 NOT NULL,
  CONSTRAINT flow_task_pk PRIMARY KEY (id)
);
```
The `id` column type matches the aggregate's id type: `uuid` for Flowable-direct with UUID ids,
`varchar` for BaseFlow (String ids). Flyway runs automatically on startup.

---

## 10. Gotchas / rules easy to get wrong

- **Delegate bean name == JSON `delegate`** (lowerCamel of the class name). Mismatch → the flow
  fails to parse at startup with a missing-bean error.
- **Exactly one state** should be `"initial": true`. It becomes the aggregate's state right after
  `makeFlowable` / `buildBaseFlow`.
- **SYSTEM action needs a `timer`** on the transition targeting it — that's how the engine
  schedules it. `"timer": { "sec": 0 }` runs it (near-)instantly; `"sec": N` delays N seconds.
- **`flow_task` table is mandatory** if any timer/system/retry exists, or the app errors when
  scheduling tasks.
- **Jackson annotation namespace:** the POC domain/DTO classes use `com.fasterxml.jackson.annotation.*`
  (`@JsonProperty`, `@JsonPropertyOrder`). Mirror that; do not invent a different namespace.
- **Multi-branch transitions:** the delegate must `actionContext.put("transition", "<key>")` where
  `<key>` matches a `result` map key; otherwise the engine assumes `"success"`.
- **`expiration` action + `expiresAt`:** mark the timeout action with `"expiration": true` so the
  engine computes an `expiresAt` on states that can time out.
