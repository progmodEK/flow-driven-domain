# Worked example — BaseFlow mode (complete, runnable)

Use this mode when there is **no rich domain aggregate** — the "thing" being driven is essentially
a workflow instance with a bag of variables, commanded by an external process. You do **not** write
an aggregate class; you use the library's `BaseFlow` (id type is always `String`) directly. Create
instances with `flowEngine.buildBaseFlow(type, vars)` and advance them with `applyAction`.

Everything below is the same shape as the Flowable-direct example (`flowable-direct-example.md`) —
same `build.gradle`, same `@SpringBootApplication @EnableScheduling`, same auto-config — **except**
the aggregate class is gone and the pieces listed here change. Example domain: a **document
approval** workflow. Base package `com.example.approval`.

## What differs from Flowable-direct
| Concern | Flowable-direct | BaseFlow |
|---|---|---|
| Aggregate class | you write one `implements Flowable<UUID>` | none — use library `BaseFlow` |
| Id type | `UUID` (or Long/String) | always `String` |
| Create instance | `makeFlowable(agg, type, vars)` | `buildBaseFlow(type, vars)` |
| Delegate T param | `ActionDelegate<YourAgg, I, R>` | `ActionDelegate<BaseFlow, I, R>` |
| Business data | fields on the aggregate | entries in the `variables` map |
| Persisted JSON | flow nested under `flow` | flow at top level (BaseFlow *is* a Flow) |
| jsonb table id column | `uuid` | `varchar` |

## Flow model — states/actions/type
```java
// FlowAction enum
@Getter @RequiredArgsConstructor
public enum ApprovalAction implements FlowAction {
  SUBMIT_FOR_REVIEW(USER),
  APPROVE(USER),
  REJECT(USER),
  ESCALATE(SYSTEM);                 // auto-fires on a timer if review stalls
  private final ActionType type;
}

// FlowState enum
@Getter
public enum ApprovalState implements FlowState {
  DRAFT, UNDER_REVIEW, ESCALATED, APPROVED, REJECTED
}

// FlowType enum
@AllArgsConstructor
public enum ApprovalFlowType implements FlowType {
  DEFAULT("flow/approval-workflow.json", ApprovalAction.class, ApprovalState.class);
  @Getter private final String template;
  @Getter private final Class<? extends FlowAction> flowActionType;
  @Getter private final Class<? extends FlowState> flowStateType;
}
```

## Config — beans typed to BaseFlow / String
```java
@Configuration
public class ApprovalConfig {

  @Bean
  public FlowRepository<BaseFlow, String> approvalRepository() {
    return new BasePostgresJsonRepository<>(BaseFlow.class)
        .setTableInfo("approval_flow", "flow_data");
  }

  @Bean
  FlowEngine<BaseFlow, String> flowEngine(final FlowRepository approvalRepository) {
    return new FlowEngine<>(String.class, ApprovalFlowType.class, approvalRepository);
  }
}
```

## Delegates — operate on the variables bag (no aggregate)
Business state lives in `BaseFlow`'s inherited `variables` map. Read globals via
`flow.getGlobalVariable(key, Class)`; write per-action results into `actionContext`.
```java
@Component @RequiredArgsConstructor @Log4j2
public class SubmitForReviewDelegate
    implements ActionDelegate<BaseFlow, Map<String, Object>, BaseFlow> {
  @Override
  public BaseFlow execute(BaseFlow flow, Map<String,Object> ctx, Map<String,Object> in) {
    // e.g. stamp who submitted; store in the flow's global variables
    flow.getVariables().put("submittedBy", in.get("submittedBy"));
    return flow;
  }
}

@Component @RequiredArgsConstructor @Log4j2
public class ApproveDelegate
    implements ActionDelegate<BaseFlow, Map<String, Object>, BaseFlow> {
  @Override
  public BaseFlow execute(BaseFlow flow, Map<String,Object> ctx, Map<String,Object> in) {
    flow.getVariables().put("decisionBy", in.get("reviewer"));
    // single-outcome transition -> engine defaults transition var to "success"
    return flow;
  }
}

@Component @RequiredArgsConstructor @Log4j2
public class RejectDelegate
    implements ActionDelegate<BaseFlow, Map<String, Object>, BaseFlow> {
  @Override
  public BaseFlow execute(BaseFlow flow, Map<String,Object> ctx, Map<String,Object> in) {
    flow.getVariables().put("rejectionReason", in.get("reason"));
    return flow;
  }
}

// SYSTEM action fired by the task consumer on a timer
@Component @RequiredArgsConstructor @Log4j2
public class EscalateDelegate extends SystemActionDelegate<BaseFlow> {
  @Override
  public BaseFlow execute(BaseFlow flow, Map<String,Object> ctx) {
    log.info("review stalled -> escalating flow {}", flow.getId());
    return flow;
  }
}
```

## Controller — create via buildBaseFlow, advance via applyAction
```java
@RestController @Log4j2 @RequestMapping("/approvals") @RequiredArgsConstructor
public class ApprovalController {
  protected final FlowRepository<BaseFlow, String> repository;
  protected final FlowEngine<BaseFlow, String> flowEngine;

  @PostMapping
  public ResponseEntity<BaseFlow> create(@RequestBody Map<String,Object> body) {
    // seed the flow's variables with whatever the caller sends
    return ResponseEntity.ok(flowEngine.buildBaseFlow(ApprovalFlowType.DEFAULT, body));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseFlow> get(@PathVariable String id) {
    return ResponseEntity.of(repository.findById(id));
  }

  @PostMapping("/{id}/submit")
  public ResponseEntity<BaseFlow> submit(@PathVariable String id,
                                         @RequestBody Map<String,Object> in) {
    return ResponseEntity.ok(flowEngine.<Map, BaseFlow>applyAction(
        id, ApprovalAction.SUBMIT_FOR_REVIEW, in));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<BaseFlow> approve(@PathVariable String id,
                                          @RequestBody Map<String,Object> in) {
    return ResponseEntity.ok(flowEngine.<Map, BaseFlow>applyAction(id, ApprovalAction.APPROVE, in));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<BaseFlow> reject(@PathVariable String id,
                                         @RequestBody Map<String,Object> in) {
    return ResponseEntity.ok(flowEngine.<Map, BaseFlow>applyAction(id, ApprovalAction.REJECT, in));
  }
}
```
Note the `applyAction(String id, ...)` overload is used directly since ids are already Strings.

## Migration — id column is varchar
```sql
CREATE SCHEMA IF NOT EXISTS approval AUTHORIZATION "admin";

CREATE TABLE IF NOT EXISTS approval.approval_flow (
  id varchar NOT NULL,
  flow_data jsonb NOT NULL,
  CONSTRAINT approval_flow_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS approval.flow_task (
  id varchar NOT NULL,
  score int8 NOT NULL,
  status varchar NOT NULL,
  ver int4 NOT NULL,
  CONSTRAINT flow_task_pk PRIMARY KEY (id)
);
```

## Workflow JSON (resources/flow/approval-workflow.json)
```json
{
  "actions": [
    { "name": "SUBMIT_FOR_REVIEW", "delegate": "submitForReviewDelegate" },
    { "name": "APPROVE",           "delegate": "approveDelegate" },
    { "name": "REJECT",            "delegate": "rejectDelegate" },
    { "name": "ESCALATE",          "delegate": "escalateDelegate", "expiration": true }
  ],
  "states": [
    { "name": "DRAFT", "initial": true,
      "transitions": [
        { "action": "SUBMIT_FOR_REVIEW", "result": { "success": "UNDER_REVIEW" } } ] },
    { "name": "UNDER_REVIEW",
      "transitions": [
        { "action": "APPROVE",  "result": { "success": "APPROVED" } },
        { "action": "REJECT",   "result": { "success": "REJECTED" } },
        { "action": "ESCALATE", "result": { "success": "ESCALATED" }, "timer": { "sec": 3600 } } ] },
    { "name": "ESCALATED",
      "transitions": [
        { "action": "APPROVE", "result": { "success": "APPROVED" } },
        { "action": "REJECT",  "result": { "success": "REJECTED" } } ] },
    { "name": "APPROVED", "transitions": [] },
    { "name": "REJECTED", "transitions": [] }
  ]
}
```
The persisted row's `flow_data` for a BaseFlow holds `id`, `createdAt`, `state`, `flowType`,
`actions`, `eligibleActions`, `variables` **at the top level** (no nested `flow` node), because
`BaseFlow` *is* its own `Flow`.

## infra/secondary/LoggingEventPublisher.java (same as Flowable-direct)
Generate the one sample `EventsPublisher` here too — it is **identical** to the flowable-direct
version (see `flowable-direct-example.md`), because `BaseFlow implements Flowable<String>` and the SPI
is typed on `Flowable<ID>`. It reads `flowable.getFlow().getEvents()`, logs each `ActionExecuted`, and
is the single `@Component` you swap to fan events out to any real sink. Nothing BaseFlow-specific
changes.
