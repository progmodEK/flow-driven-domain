# fdd

Scaffolds a complete, runnable Spring Boot application built on the
**flow-driven-domain (FDD)** library (`io.github.progmodek:flow`).

Describe a business process in natural language ("orders that go from placed to
shipped", "a leave-approval workflow", "a document review pipeline") and the skill
designs the states / actions / transitions, writes the workflow JSON, and generates
the full project: build files (Gradle or Maven), the `Flowable`/`BaseFlow` domain,
action delegates, `FlowEngine` wiring, Flyway migrations, `application.yaml`, a REST
controller, `docker-compose`, and a README.

## Skill

| Skill | Trigger |
|-------|---------|
| `fdd` | "make an FDD app", "flow-driven-domain", "process-centric aggregate", "generate a flow app", "scaffold a workflow service with flow-common", or just describing a business process to turn into an app. |

The skill activates automatically when your request matches, or invoke it explicitly
with `/fdd`.
