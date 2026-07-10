# Build layouts & files (Gradle / Maven × standalone / module)

The FDD app's *framework* stack is fixed (`io.github.progmodek:flow:1.1.0`, Spring Boot 4.1.0,
Java 25, Postgres, Flyway). Only the **build tooling and project layout** vary depending on where the
skill is invoked. This file gives the detection rule, then the exact build files + wiring + verify
command for each of the four layouts. Everything *outside* the build files (Java sources, resources,
migration, docker-compose, README) is identical across layouts — see the example references.

## Detecting the layout

From the target directory, walk up toward the filesystem root and take the **first** build root you
find:

1. A **Gradle** build — `settings.gradle`/`settings.gradle.kts`, or a `build.gradle`/`build.gradle.kts`
   next to a `gradlew` wrapper → **Layout B: Gradle subproject**.
2. A **Maven** build — a `pom.xml` with `<packaging>pom</packaging>` and/or a `<modules>` block →
   **Layout C: Maven submodule**.
3. **Nothing** → **Layout A: standalone**, defaulting to **Maven** (Layout A-Maven). If the user
   prefers Gradle, use **Layout D: standalone Gradle**.

Auto-detect, announce it ("this is a Gradle build — I'll add `<app>` as a subproject"), and let the
user override (force standalone, or pick Gradle over Maven for a fresh project). When adding a module,
match the sibling modules' conventions (group id, plugin style) rather than blindly copying the
templates below.

Naming: `<module>` is the module directory name (e.g. `checkout-poc`); `<group>` the Maven/Gradle
group (default `com.example.<domain>`); `<pkg>` the base Java package.

---

## Layout A — Standalone Maven (default for a fresh project)

Full project with its own `pom.xml`. Uses `spring-boot-starter-parent` as the parent POM (simplest
dependency/plugin management for a root project). Lombok has no build plugin in Maven, so it is a
`provided` dependency **and** an annotation processor on the compiler plugin — miss either and
`@Data`/`@Builder`/`@Log4j2` won't generate.

`<app-dir>/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
  </parent>

  <groupId>com.example.checkout</groupId>
  <artifactId>checkout-poc</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <java.version>25</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.github.progmodek</groupId>
      <artifactId>flow</artifactId>
      <version>1.1.0</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <!-- Spring Boot 4.0 split autoconfig into per-technology modules: Flyway migration only
         runs if spring-boot-flyway is on the classpath. flyway-core ALONE is silently ignored
         (no migration, no error) — the app boots but the schema/flow_task table never get created. -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-flyway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>   <!-- Flyway 10+ DB dialect module -->
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
- `spring-boot-starter-parent` manages the `lombok` version, so no `<version>` is needed on it.
- **Verify:** `cd <app-dir> && mvn -q compile`  (needs a system `mvn`). Note `-q` prints **nothing**
  on success — no "BUILD SUCCESS" banner — so treat a silent exit 0 as a pass, or drop `-q` /
  check `$?` to see it explicitly.
- **Run:** `mvn spring-boot:run`.
- Optional wrapper: if the user wants a `mvnw` (so the project runs without a system Maven), generate
  it once with `mvn -N wrapper:wrapper` — the skill ships **no** Maven-wrapper binary, unlike the
  bundled Gradle wrapper.

---

## Layout B — Gradle subproject (existing Gradle build)

Do **not** add a wrapper or a `settings.gradle` to the module — it inherits the root wrapper and
settings. Each FDD subproject applies its own Spring Boot / Lombok plugins, exactly like the sibling
modules (this mirrors the order-preparation POCs).

`<build-root>/<module>/build.gradle`:
```gradle
plugins {
    id "java"
    id "io.freefair.lombok" version "9.5.0"
    id "org.springframework.boot" version "4.1.0"
    id "io.spring.dependency-management" version "1.1.7"
}

group "com.example.checkout"
version "0.0.1-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
    implementation "io.github.progmodek:flow:1.1.0"        // Flow Driven Domain
    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.postgresql:postgresql"
    // Spring Boot 4.0 moved Flyway autoconfig into spring-boot-flyway; flyway-core alone
    // is silently ignored (migrations never run). flyway-database-postgresql = Flyway 10+ dialect.
    runtimeOnly    "org.springframework.boot:spring-boot-flyway"
    runtimeOnly    "org.flywaydb:flyway-database-postgresql"
}
```
- **Omit the `java { toolchain … }` block** if the root build already configures the Java toolchain
  for all subprojects; add it only if each module sets its own (check a sibling module first).
- **Wire it into the build** — append to the root `settings.gradle` (or `.kts`):
  ```gradle
  include 'checkout-poc'
  ```
  (`settings.gradle.kts`: `include("checkout-poc")`.) Add the line; don't rewrite the file.
- **Verify:** from the build root, `./gradlew :<module>:compileJava --console=plain -q`.
- **Run:** `./gradlew :<module>:bootRun`.

---

## Layout C — Maven submodule (existing Maven build)

The module inherits the **reactor root** as its parent (not `spring-boot-starter-parent`). If the
reactor root already imports the Spring Boot BOM / manages these versions, drop the redundant
`<version>`s and plugin config below and mirror a sibling module instead. When the reactor does *not*
manage Boot, import the BOM in the module's `dependencyManagement` as shown.

`<build-root>/<module>/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.example</groupId>           <!-- the reactor root's coordinates -->
    <artifactId>flow-driven-domain</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>

  <artifactId>checkout-poc</artifactId>

  <properties>
    <java.version>25</java.version>
  </properties>

  <!-- Only needed if the reactor root does NOT already manage Spring Boot: -->
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>4.1.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>io.github.progmodek</groupId>
      <artifactId>flow</artifactId>
      <version>1.1.0</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <!-- Spring Boot 4.0 split autoconfig into per-technology modules: Flyway migration only
         runs if spring-boot-flyway is on the classpath. flyway-core ALONE is silently ignored
         (no migration, no error) — the app boots but the schema/flow_task table never get created. -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-flyway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>   <!-- Flyway 10+ DB dialect module -->
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <version>${lombok.version}</version>   <!-- omit if the parent manages lombok -->
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
- **Wire it into the reactor** — add to the root `pom.xml`'s `<modules>` block:
  ```xml
  <module>checkout-poc</module>
  ```
- **Verify:** from the build root, `mvn -q -pl <module> -am compile` (`-am` also builds needed
  siblings).
- **Run:** `mvn -pl <module> spring-boot:run`.

---

## Layout D — Standalone Gradle (user override of the Maven default)

The original single-project layout: the `build.gradle` from `references/flowable-direct-example.md`
(with a `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }` block), its own
`settings.gradle` (`rootProject.name = "<app>"`), **plus the bundled Gradle wrapper**:
```bash
cp -R <skill-dir>/assets/gradle-wrapper/. <app-dir>/
chmod +x <app-dir>/gradlew
```
(brings `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` + `.properties` — Gradle 9.5.0.)
- **Verify:** `cd <app-dir> && ./gradlew compileJava --console=plain -q`.
- **Run:** `./gradlew bootRun`.

---

## Quick reference

| Layout | Build file(s) | Wrapper | Root wiring | Verify (from build root unless noted) |
|---|---|---|---|---|
| A — standalone Maven | `pom.xml` (parent = starter-parent) | none (optional `mvnw`) | — | `cd <app> && mvn -q compile` |
| B — Gradle subproject | module `build.gradle` | inherited | `include '<module>'` in settings | `./gradlew :<module>:compileJava` |
| C — Maven submodule | module `pom.xml` (parent = reactor) | inherited | `<module>` in root `<modules>` | `mvn -q -pl <module> -am compile` |
| D — standalone Gradle | `build.gradle` + `settings.gradle` | bundled (copy asset) | — | `cd <app> && ./gradlew compileJava` |
