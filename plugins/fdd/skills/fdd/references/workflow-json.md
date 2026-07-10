# Workflow JSON schema (the state machine)

The workflow JSON at `src/main/resources/<template-path>` is the heart of an FDD app — the Java
delegates are intentionally thin, so this file is where the runtime behaviour of the process
actually lives. Field names use lowerCamelCase.

## Top-level shape
```jsonc
{
  "basedOn": "flow/parent-workflow.json",   // optional: inherit + override another JSON
  "actions": [ /* Action[] */ ],
  "states":  [ /* State[]  */ ]
}
```

## Action object
```jsonc
{
  "name": "PICK_ITEMS",            // MUST equal a constant of the Action enum
  "delegate": "pickItemsDelegate", // Spring bean name of the ActionDelegate (lowerCamel of class)
  "expiration": false,             // optional: marks THE action used to compute state expiry (expiresAt)
  "context": "prep",               // optional grouping key
  "initContext": "prep",           // optional: when fired, clears variables of actions sharing this context
  "system": "NOTIFY_OM",           // optional: links a user action to a system action (dedupe guard)
  "combination": "APPEND"          // REPLACE|APPEND|REMOVE — only relevant with `basedOn` merging
}
```
List every action (USER and SYSTEM) here with its `delegate`. Only `name` and `delegate` are
required in the common case.

## State object
```jsonc
{
  "name": "IN_PREPARATION",        // MUST equal a constant of the State enum
  "initial": true,                 // exactly ONE state should set this; it's the starting state
  "combination": "APPEND",         // merge strategy when using basedOn
  "transitions": [ /* Transition[] */ ]
}
```
Terminal/final states simply have `"transitions": []`.

## Transition object
```jsonc
{
  "action": "PICK_ITEMS",          // the action that triggers this transition (from this state)
  "internal": false,               // if true: stay in the same state (self-loop)
  "dependsOn": "START_PREPARATION",// eligibility gate: a prior action must have run first
  "result": {                      // Map<String,State>: delegate's "transition" var -> next state
    "partial": "IN_PREPARATION",
    "full":    "PENDING_PICKUP"
  },
  "exceptions": {                  // Map<Integer,State>: DelegateException code -> next state
    "001": "RETRY_NOTIFICATION"
  },
  "retry": {                       // optional
    "number": 3,                   // max attempts, then transition to `exceeded`
    "exceeded": "ERROR"
  },
  "timer": {                       // optional: schedule the action asynchronously
    "inherit": false,              // true = keep the parent state's expiry instant; ignores `sec`
    "sec": 10                      // delay in seconds; 0 = run (near-)instantly
  },
  "combination": "APPEND"
}
```

### Runtime semantics
- **On success:** next state = `result[<transition var>]`. The delegate sets the branch via
  `actionContext.put("transition", "<key>")`; if it sets nothing, the engine uses `"success"`, so
  a single-outcome transition just needs `"result": { "success": "NEXT_STATE" }`.
- **On `DelegateException(code)`:** if the attempt count `>= retry.number` → go to
  `retry.exceeded`; otherwise → `exceptions[code]`. JSON exception keys are strings like `"001"`
  but bind to `Integer`, so `new DelegateException(1, ...)` matches `"001"`.
- **`timer.sec > 0`** schedules a delayed `flow_task`; `sec == 0` schedules an instant one. This is
  how SYSTEM actions and timeouts fire. On leaving the state, a stale scheduled task is removed.
- **`internal: true`** keeps the same state (useful for repeatable actions like picking items in
  several calls before a branch finally moves the state forward).
- **`dependsOn`** enforces ordering: the named action must have executed before this transition is
  eligible (typically paired with internal transitions).

## Two canonical patterns

**Repeatable action that eventually branches** (pick items until complete):
```jsonc
{ "name": "IN_PREPARATION",
  "transitions": [
    { "action": "PICK_ITEMS",
      "result": { "partial": "IN_PREPARATION", "full": "PENDING_PICKUP" } } ] }
```

**System notification with retry + timer** (auto-notify, retry every 10s up to 3x, else ERROR):
```jsonc
{ "name": "DELIVERED",
  "transitions": [
    { "action": "NOTIFY_OM",
      "result":     { "success": "COMPLETED" },
      "exceptions": { "001": "RETRY_NOTIFICATION" },
      "timer":      { "sec": 0 } } ] },
{ "name": "RETRY_NOTIFICATION",
  "transitions": [
    { "action": "NOTIFY_OM",
      "result":     { "success": "COMPLETED" },
      "exceptions": { "001": "RETRY_NOTIFICATION" },
      "retry":      { "number": 3, "exceeded": "ERROR" },
      "timer":      { "sec": 10 } } ] }
```

**Timeout that expires an idle process** (Hello-World `TIMEOUT`):
```jsonc
// action declared with "expiration": true
{ "name": "PENDING_COMPLETION",
  "transitions": [
    { "action": "WORLD",   "result": { "success": "COMPLETED" } },
    { "action": "TIMEOUT", "result": { "success": "EXPIRED" }, "timer": { "sec": 30 } } ] }
```
`"expiration": true` marks the **single** action whose timer defines the aggregate's `expiresAt`. If a
process has several timeout hops (e.g. escalate-then-auto-reject), set it on only **one** — normally
the first timeout out of the initial waiting state; every other SYSTEM timeout just carries its own
`timer` and needs no `expiration` flag. It's a metadata hint for `expiresAt`, not a per-transition
switch, so don't put it on more than one action.

## Design checklist before writing the JSON
1. One `initial: true` state; all reachable states listed; terminal states have empty transitions.
2. Every action referenced in a transition is declared in `actions` with a `delegate`.
3. Each SYSTEM action's transition carries a `timer` (else it never fires).
4. Branching transitions have a `result` map whose keys the delegate actually sets.
5. Retry/exception targets are real states.
