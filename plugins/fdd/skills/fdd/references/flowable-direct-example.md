# Worked example — Flowable-direct mode (complete, runnable)

This is the real order-preparation POC, file by file. It is the canonical template for a
Flowable-direct app: a rich aggregate that `implements Flowable<UUID>` with business invariants,
persisted whole as a jsonb column. **Adapt names/fields/states/actions to the user's domain** —
keep the structure, wiring, and idioms identical.

Base package in this example: `com.progmod.poc`. Generated apps should use the package the user
picks (default `com.example.<domain>`), and a matching root path for controllers.

## Project layout
```
<app>/
├── build.gradle
├── settings.gradle
├── gradlew, gradlew.bat, gradle/wrapper/…      (bundled — copy from the skill's assets)
├── docker-compose.yml
├── README.md
└── src/main/
    ├── java/<pkg>/
    │   ├── PocApplication.java
    │   ├── PocConfig.java
    │   ├── domain/OrderPreparation.java          (aggregate: implements Flowable)
    │   ├── domain/Item.java                       (entity/value object)
    │   ├── domain/flow/OrderPreparationAction.java
    │   ├── domain/flow/OrderPreparationState.java
    │   ├── domain/flow/OrderPreparationFlowType.java
    │   ├── domain/delegate/StartPreparationDelegate.java
    │   ├── domain/delegate/PickItemsDelegate.java
    │   ├── domain/delegate/PickupDelegate.java
    │   ├── domain/delegate/NotifyDelegate.java     (SYSTEM action)
    │   ├── dto/*.java                              (request records)
    │   ├── infra/primary/PocController.java
    │   ├── infra/primary/ErrorHandler.java
    │   └── infra/secondary/KafkaEventPublisher.java
    └── resources/
        ├── application.yaml
        ├── db/migration/V1__Initial_version.sql
        └── flow/in-store-workflow.json
```

## build.gradle
> This is the **standalone-Gradle** build file (Layout D). If you're generating into an existing
> Gradle/Maven build, or a fresh project that should default to Maven, use the matching layout in
> `references/build-files.md` instead — the Java sources, resources, and migration below are the same
> regardless of build tool.
```gradle
plugins {
    id "java"
    id "io.freefair.lombok" version "9.5.0"
    id "org.springframework.boot" version "4.1.0"
    id "io.spring.dependency-management" version "1.1.7"
}

group "com.example.orderprep"
version "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

repositories { mavenCentral() }

dependencies {
    implementation "io.github.progmodek:flow:1.1.0"        // Flow Driven Domain
    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.postgresql:postgresql"
    // Spring Boot 4.0 split Flyway autoconfig into spring-boot-flyway; flyway-core alone is
    // silently ignored (migrations never run). flyway-database-postgresql = Flyway 10+ dialect.
    runtimeOnly    "org.springframework.boot:spring-boot-flyway"
    runtimeOnly    "org.flywaydb:flyway-database-postgresql"
}
```
`settings.gradle`: `rootProject.name = "<app>"`

## PocApplication.java
```java
package com.example.orderprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PocApplication {
  public static void main(final String[] args) {
    SpringApplication.run(PocApplication.class, args);
  }
}
```

## PocConfig.java (the two beans the consumer must declare)
```java
package com.example.orderprep;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.postgres.BasePostgresJsonRepository;
import com.example.orderprep.domain.OrderPreparation;
import com.example.orderprep.domain.flow.OrderPreparationFlowType;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

  @Bean
  public FlowRepository<OrderPreparation, UUID> orderPostgresRepository() {
    return new BasePostgresJsonRepository<>(OrderPreparation.class)
        .setTableInfo("order_preparation", "data");
  }

  @Bean
  FlowEngine<OrderPreparation, UUID> flowEngine(final FlowRepository orderPostgresRepository) {
    return new FlowEngine<>(UUID.class, OrderPreparationFlowType.class, orderPostgresRepository);
  }
}
```

## domain/OrderPreparation.java (aggregate — note the three flow fields + invariants)
```java
package com.example.orderprep.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.progmod.flow.domain.model.Flow;
import com.progmod.flow.domain.model.Flowable;
import com.example.orderprep.dto.PickItemRequest;
import com.example.orderprep.dto.PickItemsRequest;
import java.beans.Transient;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "orderRef", "items", "state", "flow"})
public class OrderPreparation implements Flowable<UUID> {
  @Builder.Default
  private UUID id = UUID.randomUUID();
  private String orderRef;
  private List<Item> items;

  // ---- flow fields required by Flowable; persisted WITH the aggregate ----
  private String state;
  private Flow flow;

  /** Business behaviour + invariant checks live on the aggregate, NOT in delegates. */
  public void updatePreparation(final PickItemsRequest pickItemsRequest) {
    final List<String> existingSkus = items.stream().map(Item::getSkuId).toList();
    final boolean allExist = pickItemsRequest.pickItems().stream()
        .map(PickItemRequest::skuId).allMatch(existingSkus::contains);
    if (!allExist) {
      throw new IllegalArgumentException("prepared items do not exist");
    }
    pickItemsRequest.pickItems().forEach(req ->
        items.stream().filter(i -> i.getSkuId().equals(req.skuId())).findFirst()
            .ifPresent(i -> i.updatePreparedQty(req.qty())));
  }

  @Transient
  public boolean isFullyPrepared() {
    return items.stream().allMatch(i -> i.getQty() == i.getQtyPrepared());
  }
}
```

## domain/Item.java
```java
package com.example.orderprep.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder @Data @NoArgsConstructor @AllArgsConstructor
public class Item {
  private String skuId;
  private String name;
  private int qty;
  private int qtyPrepared;
  private boolean pickedUp;

  public Item updatePreparedQty(final int qtyPrepared) {
    if (qtyPrepared > this.qty) {
      throw new IllegalArgumentException("prepared qty is greater than order qty");
    }
    this.qtyPrepared = qtyPrepared;
    if (qty == qtyPrepared) { pickedUp = true; }
    return this;
  }
}
```

## domain/flow enums
See `fdd-api.md` §4 — `OrderPreparationAction` (USER/SYSTEM constants), `OrderPreparationState`
(all states), `OrderPreparationFlowType` (`DEFAULT("flow/in-store-workflow.json", Action.class,
State.class)`).

## domain/delegate — one @Component per action
USER delegate that just runs (thin):
```java
@Component @RequiredArgsConstructor @Log4j2
public class StartPreparationDelegate
    implements ActionDelegate<OrderPreparation, Map<String, String>, OrderPreparation> {
  @Override
  public OrderPreparation execute(OrderPreparation agg, Map<String,Object> ctx, Map<String,String> in) {
    log.info("startPreparation invoked");
    return agg;
  }
}
```
USER delegate that branches (writes the transition var, calls aggregate behaviour):
```java
@Component @RequiredArgsConstructor @Log4j2
public class PickItemsDelegate
    implements ActionDelegate<OrderPreparation, PickItemsRequest, OrderPreparation> {
  @Override
  public OrderPreparation execute(OrderPreparation agg, Map<String,Object> ctx, PickItemsRequest req) {
    agg.updatePreparation(req);
    ctx.put(ACTION_TRANSITION_VARIABLE, agg.isFullyPrepared() ? "full" : "partial");
    return agg;
  }
}
```
SYSTEM delegate (extends `SystemActionDelegate`, can throw `DelegateException` to drive retries):
```java
@Component @RequiredArgsConstructor @Log4j2
public class NotifyDelegate extends SystemActionDelegate<OrderPreparation> {
  @Override
  public OrderPreparation execute(OrderPreparation agg, Map<String,Object> ctx) {
    log.info("notification invoked");
    if (new Random().nextBoolean()) {           // simulate a transient failure
      throw new DelegateException(1, "notification error");
    }
    return agg;
  }
}
```
(Bean names become `startPreparationDelegate`, `pickItemsDelegate`, `pickupDelegate`,
`notifyDelegate` — reference exactly these in the workflow JSON.)

## dto records (Jackson 2 annotation namespace)
```java
public record CreateOrderPreparationRequest(@JsonProperty("orderRef") String orderRef,
                                            @JsonProperty("items") List<ItemRequest> items) {}
public record ItemRequest(@JsonProperty("skuId") String skuId,
                          @JsonProperty("name") String name,
                          @JsonProperty("qty") int qty) {}
public record PickItemRequest(@JsonProperty("skuId") String skuId, @JsonProperty("qty") int qty) {}
public record PickItemsRequest(@JsonProperty("pickItems") List<PickItemRequest> pickItems) {}
```

## infra/primary/PocController.java (one endpoint per USER action + create + GET)
```java
@RestController @Log4j2 @RequestMapping("/orders") @RequiredArgsConstructor
public class PocController {
  protected final FlowRepository<OrderPreparation, UUID> repository;
  protected final FlowEngine<OrderPreparation, UUID> flowEngine;

  @PostMapping
  public ResponseEntity<OrderPreparation> create(@RequestBody CreateOrderPreparationRequest req) {
    OrderPreparation agg = OrderPreparation.builder()
        .orderRef(req.orderRef())
        .items(req.items().stream().map(i ->
            Item.builder().skuId(i.skuId()).name(i.name()).qty(i.qty()).build()).toList())
        .build();
    return ResponseEntity.ok(flowEngine.makeFlowable(agg, OrderPreparationFlowType.DEFAULT, Map.of()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrderPreparation> get(@PathVariable String id) {
    return ResponseEntity.of(repository.findById(UUID.fromString(id)));
  }

  @PostMapping("/{id}/start-preparation")
  public ResponseEntity<OrderPreparation> start(@PathVariable String id) {
    return ResponseEntity.ok(flowEngine.<Map, OrderPreparation>applyAction(
        UUID.fromString(id), OrderPreparationAction.START_PREPARATION, Map.of()));
  }

  @PostMapping("/{id}/pick-items")
  public ResponseEntity<OrderPreparation> pick(@PathVariable String id,
                                               @RequestBody PickItemsRequest req) {
    return ResponseEntity.ok(flowEngine.<PickItemsRequest, OrderPreparation>applyAction(
        UUID.fromString(id), OrderPreparationAction.PICK_ITEMS, req));
  }

  @PostMapping("/{id}/pickup")
  public ResponseEntity<OrderPreparation> pickup(@PathVariable String id) {
    return ResponseEntity.ok(flowEngine.<Map, OrderPreparation>applyAction(
        UUID.fromString(id), OrderPreparationAction.PICKUP, Map.of()));
  }
}
```
Note: **only USER actions get an endpoint.** SYSTEM actions (NOTIFY_OM) fire automatically via the
task consumer.

## infra/primary/ErrorHandler.java
```java
@Log4j2 @ControllerAdvice
public class ErrorHandler {
  @ExceptionHandler
  ResponseEntity<String> handle(final RuntimeException ex) {
    return ResponseEntity.status(HttpStatusCode.valueOf(406)).body(ex.getMessage());
  }
}
```

## infra/secondary/KafkaEventPublisher.java (optional sample EventsPublisher)
```java
@Component @Slf4j
public class KafkaEventPublisher implements EventsPublisher {
  @Override public void publishEvents(final Flowable flow) {
    log.info("PUBLISH EVENTS TO KAFKA IF YOU WANT");
  }
}
```

## resources/application.yaml
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres?currentSchema=orderprep
    username: admin
    password: admin
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0.0
    locations: [ 'classpath:db/migration' ]
    url: jdbc:postgresql://localhost:5432/postgres
    schemas: [ 'orderprep' ]
    user: "admin"
    password: "admin"
  output:
    ansi:
      enabled: ALWAYS

flow:
  actionLogger:
    enabled: true
  taskConsumer:
    scheduleMilli: 1000
  lockService:
    removeBlockedLockScheduleMilli: 10000
```

## resources/db/migration/V1__Initial_version.sql
```sql
CREATE SCHEMA IF NOT EXISTS orderprep AUTHORIZATION "admin";

CREATE TABLE IF NOT EXISTS orderprep.order_preparation (
  id uuid NOT NULL,
  data jsonb NOT NULL,
  CONSTRAINT order_preparation_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS orderprep.flow_task (
  id varchar NOT NULL,
  score int8 NOT NULL,
  status varchar NOT NULL,
  ver int4 NOT NULL,
  CONSTRAINT flow_task_pk PRIMARY KEY (id)
);
```

## resources/flow/in-store-workflow.json
See `workflow-json.md` for the full annotated version — it's the exact state machine for this
domain (TO_PREPARE → IN_PREPARATION → PENDING_PICKUP → DELIVERED → (NOTIFY_OM) → COMPLETED, with
RETRY_NOTIFICATION/ERROR).
