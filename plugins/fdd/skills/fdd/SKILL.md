---
name: fdd
description: >-
  Scaffolds a complete, runnable Spring Boot application built on the flow-driven-domain (FDD)
  library (io.github.progmodek:flow). Use this whenever the user wants to create, generate, bootstrap,
  or scaffold a new FDD app, an "FDD-based" service, a flow-driven / process-centric domain, or a
  Spring Boot state-machine/workflow app using the `flow` / `flow-common` library — even if they just
  describe a business process ("orders that go from placed to shipped", "a leave-approval workflow",
  "a document review pipeline") and ask to turn it into an app. Takes a natural-language description
  of the process, designs the states/actions/transitions + workflow JSON, and generates the full
  project (Gradle or Maven build, Flowable/BaseFlow domain, delegates, FlowEngine wiring, Flyway
  migrations, application.yaml, REST controller, docker-compose, README) — adapting to the build tool
  of the repo it is invoked in (subproject/submodule) or standalone. Trigger on "make an FDD app",
  "flow-driven-domain", "process-centric aggregate", "generate a flow app", "scaffold a workflow
  service with flow-common", and similar.
---

# FDD App Generator

Generate a complete, runnable Spring Boot application on top of the **flow-driven-domain (FDD)**
library. FDD turns a domain into a *process-centric* one: actions/states/transitions are declared in
a workflow JSON, a `FlowEngine` drives the process, and flow history is persisted with the aggregate.

Your job: take the user's process description, **design the state machine**, confirm it, then emit a
full project that compiles and runs. The *framework* stack is fixed: `io.github.progmodek:flow:1.1.0`,
Spring Boot 4.1.0, Java 25, Postgres (jsonb), Flyway. The *build tooling*, however, **adapts to where
the skill is invoked** — a new module inside the caller's existing Gradle/Maven build, or a standalone
project — so read `references/build-files.md` and pick the layout in Step 4 before writing any build
files. Reactive is out of scope — always generate the imperative (blocking) stack.

Read the reference files as you go — do not generate from memory. **In particular, copy the exact
import statements from the "Exact imports" section at the top of `references/fdd-api.md`** — the
framework packages are counter-intuitive (`FlowAction`/`FlowState`/`FlowType` live in
`com.progmod.flow.domain.service.parser.definition`, and `ActionDelegate`/`SystemActionDelegate` in
`com.progmod.flow.domain.service.delegate`, **not** in `domain.model` / `domain.delegate`). Guessing
these is the #1 cause of a generated app failing to compile.

- `references/fdd-api.md` — the library's public API (interfaces, engine, enums, persistence, rules).
- `references/workflow-json.md` — the workflow JSON schema and runtime semantics.
- `references/flowable-direct-example.md` — a complete Flowable-direct app, file by file.
- `references/baseflow-example.md` — a complete BaseFlow app, file by file (the deltas).
- `references/build-files.md` — the four build layouts (Gradle/Maven × standalone/module), their
  build files, wiring, and verify commands. Read before writing `build.gradle`/`pom.xml`.

## Workflow

### Step 1 — Understand the process (design, don't interrogate)

The user gives a natural-language description. **Design the flow yourself**, then confirm — don't
walk them through a long questionnaire. From the description infer:

- The **aggregate / process name** and its meaningful business data (fields).
- The **states**: one initial state, the intermediate states, and the terminal state(s). Name them
  as clear status constants (`TO_PREPARE`, `UNDER_REVIEW`, `APPROVED`, `EXPIRED`, ...).
- The **actions** and whether each is `USER` (invoked via REST) or `SYSTEM` (fired automatically by
  a timer/retry — timeouts, auto-notifications, escalations).
- The **transitions**: which action moves which state to which, including branching outcomes
  (a delegate that decides "partial" vs "full"), retries, timeouts, and error/exception targets.
- Any **business invariants** worth enforcing on the aggregate (quantity checks, existence checks).

If the description is genuinely ambiguous on something that changes the machine (e.g. "should an
unreviewed request auto-expire, and after how long?"), ask a **short, targeted** question — don't
ask about things you can reasonably default.

### Step 2 — Choose the mode (ask this explicitly)

There are two usage models; the user must pick (see `fdd-api.md` §1). Ask which they want, briefly
explaining the trade-off:

- **Flowable-direct** — a rich aggregate class `implements Flowable<ID>` carrying business fields +
  invariants, persisted whole. Pick this when there's real domain data/rules (the common case; it's
  what the order-preparation POC uses). → follow `references/flowable-direct-example.md`.
- **BaseFlow** — no aggregate class; the library's `BaseFlow` (id = String) is the workflow instance
  and business data lives in a `variables` map. Pick this when the process is essentially a
  standalone workflow with no rich domain. → follow `references/baseflow-example.md`.

Recommend Flowable-direct when the description clearly has domain data + invariants; recommend
BaseFlow when it's a thin orchestration/approval-style flow. Let the user decide.

### Step 3 — Confirm the design

Present a compact spec before writing any files: aggregate + fields, the state list (mark initial &
terminal), the action list (mark USER/SYSTEM), and the transition table (action, from → to,
branches/retries/timers). Keep it skimmable. Get a yes (or adjust), then generate.

Also confirm (or state your defaults and proceed): base **package** (default
`com.example.<domain>`), **app/root name**, DB **schema** name, and the HTTP **base path**.

### Step 4 — Pick the build layout (detect the context)

Before writing any build files, work out **where** this app is being generated, because it decides
whether you emit a standalone project or wire a module into the caller's existing build. Look at the
target directory and its parents:

- **Existing Gradle build** (`settings.gradle`/`settings.gradle.kts` — or a `build.gradle`(`.kts`) +
  `gradlew` — at the target dir or an ancestor): generate the app as a **Gradle subproject** of that
  build. No own wrapper, no own `settings.gradle`; add `include '<module>'` to the root settings.
- **Existing Maven build** (`pom.xml` with `<modules>` / `<packaging>pom</packaging>`): generate the
  app as a **Maven submodule** — a module `pom.xml` plus a `<module>` entry in the reactor POM.
- **No existing build** (a bare/new directory): generate a **standalone** project. Default to
  **Maven** (most consumers use it); offer Gradle if the user prefers it.

Auto-detect, then **state what you found and are about to do** ("this is a Gradle build — I'll add
`<app>` as a subproject") so the user can override (e.g. force standalone, or pick Gradle over Maven).
The counterpart to `settings.gradle`/`pom.xml` wiring, the exact build-file templates for all four
layouts, and their per-layout verify commands live in `references/build-files.md` — read it now and
follow the matching layout in the next step. Everything below the build files (Java, resources,
migration, docker-compose, README) is identical across layouts.

### Step 5 — Generate the project

Create the app under the chosen location (a module directory inside the build, or a standalone
`./<app-name>/`). Mirror the chosen example's structure, wiring, and idioms exactly — only the domain
specifics change. Generate every file:

1. **Build file(s)** — per the layout picked in Step 4, following `references/build-files.md`:
   the module `build.gradle` or `pom.xml`, plus the root-build wiring (`include`/`<module>`) for a
   subproject/submodule, or the full standalone build (with Gradle wrapper only for standalone
   Gradle). Keep the exact framework/plugin/dependency versions.
2. The `*Application.java` (`@SpringBootApplication @EnableScheduling`) and `*Config.java` (the
   `FlowRepository` + `FlowEngine` beans).
3. `domain/flow/` — the `*Action`, `*State`, `*FlowType` enums.
4. `domain/` — the aggregate + entities (Flowable-direct) **or** nothing extra (BaseFlow).
5. `domain/delegate/` — one `@Component ActionDelegate` per action. Bean name = class name with
   first letter lowercased, and it **must** equal the `delegate` string in the JSON.
6. `dto/` — request records (Jackson `com.fasterxml.jackson.annotation.*` namespace).
7. `infra/primary/` — the REST controller (one endpoint per **USER** action + create + GET) and an
   `ErrorHandler`. `infra/secondary/` — an optional sample `EventsPublisher`.
8. `resources/flow/<name>.json` — the workflow JSON (validate against the `workflow-json.md`
   checklist).
9. `resources/application.yaml` — datasource + Flyway (own schema) + `flow.*` props.
10. `resources/db/migration/V1__Initial_version.sql` — schema + aggregate table (id column type
    matches the id type: `uuid` for UUID, `varchar` for BaseFlow) + the mandatory `flow_task` table.
11. `docker-compose.yml` — copy from `assets/docker-compose.yml` (skip if the caller's build already
    ships one at the repo root that exposes Postgres on `5432`; reuse it instead).
12. `README.md` — fill in `assets/README.template.md` (states, actions, curl examples for each USER
    endpoint, run instructions). For a module, note how to run it from the root build
    (`./gradlew :<module>:bootRun` or `mvn -pl <module> spring-boot:run`).

### Step 6 — Verify it builds

Confirm the app is actually runnable before handing off. The command depends on the layout (see the
"Verify" line for each layout in `references/build-files.md`):
```bash
# standalone Gradle:   cd <app-dir> && ./gradlew compileJava --console=plain -q
# Gradle subproject:   ./gradlew :<module>:compileJava --console=plain -q   # from the build root
# standalone Maven:    cd <app-dir> && mvn -q compile
# Maven submodule:     mvn -q -pl <module> -am compile                      # from the build root
```
Fix any compilation errors. The usual suspects, in order of frequency:
1. **Wrong import package** (`package com.progmod.flow... does not exist`) — re-check against the
   "Exact imports" section of `references/fdd-api.md`. `FlowAction/FlowState/FlowType` →
   `...domain.service.parser.definition`; `ActionDelegate/SystemActionDelegate` →
   `...domain.service.delegate`. Do not add a dependency to "fix" a missing framework package.
2. A delegate bean name not matching the JSON `delegate` string.
3. A state/action referenced in the JSON but missing from the enum. The full build
(`./gradlew build` / `mvn package`) also works but needs no DB; `bootRun` needs Postgres up.

**Compile ≠ runs.** A clean compile does NOT exercise Flyway, the datasource, or delegate wiring —
e.g. a missing `spring-boot-flyway` module lets the app *start* but silently skips all migrations, so
`flow_task` never gets created and the task consumer errors every second. When you can (Postgres
available), do a real smoke test: `docker compose up -d`, boot the app, and confirm the startup log
shows `Migrating schema ... to version` and no `relation "flow_task" does not exist`. If you can't
boot it, tell the user the app is compile-verified but not run-verified, and give them the smoke-test
steps.

Then tell the user how to run it (docker compose up → the layout's `bootRun`/`spring-boot:run`
command from `references/build-files.md`) and give a copy-pasteable curl walkthrough that drives the
process end to end (create → each USER action → GET to watch state evolve, noting where a SYSTEM
action fires automatically).

## Correctness rules (the things that break FDD apps)

These come from `fdd-api.md` §10 — check them in the generated output:

- **Delegate bean name == JSON `delegate`** (lowerCamel of the class name). This is the #1 failure.
- **Exactly one** state has `"initial": true`; every state/action named in the JSON exists in the
  enums; terminal states have `"transitions": []`.
- **Every SYSTEM action's transition carries a `timer`** — that's how the engine fires it. No
  endpoint is generated for SYSTEM actions.
- **`flow_task` table is always created** in the migration (any timer/retry needs it).
- **Branching transitions**: the delegate calls `actionContext.put(ACTION_TRANSITION_VARIABLE, key)`
  with a key present in the transition's `result` map; single-outcome transitions use
  `"result": {"success": "NEXT"}` and the delegate sets nothing.
- **SYSTEM delegates** extend `SystemActionDelegate` and may `throw new DelegateException(code,msg)`
  to drive `exceptions` / `retry` transitions.
- App class uses `@SpringBootApplication @EnableScheduling` (no `@EnableTransactionManagement`
  needed); the `flow` jar auto-configures the rest — there is **no** `@Enable` annotation for FDD.

## Notes

- Keep delegates thin: process wiring + a call into aggregate behaviour. Business invariants belong
  on the aggregate (Flowable-direct) or as explicit checks in the delegate (BaseFlow).
- The published README documents version `1.0.1`; the current library is **1.1.0** — always generate
  `1.1.0`.
- If the user wants a persistence approach other than Postgres jsonb (JPA/Spring Data), any
  `FlowRepository<T,ID>` bean works — but the default and recommended path is
  `BasePostgresJsonRepository`.
