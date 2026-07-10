# {{APP_TITLE}}

A process-centric Spring Boot application built with the **flow-driven-domain (FDD)** library
(`io.github.progmodek:flow:1.1.0`). {{ONE_LINE_DESCRIPTION}}

Mode: **{{MODE}}** (Flowable-direct / BaseFlow) · Java 25 · Spring Boot 4.1.0 · Postgres (jsonb).

## The process

States: {{STATES}}

Actions: {{ACTIONS}}

The full state machine lives in [`src/main/resources/flow/{{WORKFLOW_JSON}}`](src/main/resources/flow/{{WORKFLOW_JSON}}).
The Java delegates in `domain/delegate/` are intentionally thin — read the JSON first to understand
the runtime behaviour.

## Run it

```bash
# 1. start Postgres
{{DOCKER_COMPOSE_CMD}}

# 2. run the app (listens on :8081, Flyway creates the schema/tables on startup)
{{RUN_COMMAND}}
```

> Fill `{{RUN_COMMAND}}` for the build layout: standalone Gradle `./gradlew bootRun` · Gradle
> subproject `./gradlew :<module>:bootRun` (from the build root) · standalone Maven `mvn spring-boot:run`
> · Maven submodule `mvn -pl <module> spring-boot:run`. And `{{DOCKER_COMPOSE_CMD}}` is
> `docker compose up -d`, or a note to reuse the root build's compose if the module has none.

## API

Drive the process end to end with curl — create the aggregate, invoke each USER action, and `GET` it
between calls to watch the state advance (SYSTEM actions fire on their own via timers, no endpoint).

{{CURL_EXAMPLES}}

## How it's wired

- `domain/` — {{DOMAIN_NOTE}}
- `domain/flow/` — the `*Action` / `*State` / `*FlowType` enums that declare the process.
- `domain/delegate/` — one `ActionDelegate` bean per action (bean name matches the JSON `delegate`).
- `*Config.java` — the `FlowRepository` (Postgres jsonb) and `FlowEngine` beans.
- `resources/flow/{{WORKFLOW_JSON}}` — the workflow / state machine.
- `resources/db/migration/` — Flyway migration (aggregate table + mandatory `flow_task` table).

USER actions are exposed as REST endpoints; SYSTEM actions fire automatically via the framework's
scheduled task consumer (timers / retries).
