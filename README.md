# Shop: Lab-02 Spring Framework

Small Spring Boot application built step by step. This README covers **Task 1: Bootstrapping with Maven and Spring Boot**.

## Project info

- Group / artifact: `edu.itc.se` / `shop`
- Package: `edu.itc.shop`
- Build tool: Maven (wrapper `./mvnw`)
- Dependencies: Spring Web MVC, Validation, JDBC, H2, Actuator, AOP, DevTools

## Task 1: Bootstrapping

### What does the `<parent>` give me?

The parent is `spring-boot-starter-parent`. It provides:

- **Dependency management**: a BOM that fixes compatible versions of Spring, Jackson, Tomcat, Hibernate and many other libraries.
- **Plugin defaults**: pre-configured Maven plugins (compiler, surefire, jar, spring-boot-maven-plugin).
- **Build settings**: Java version, UTF-8 encoding and resource filtering.

### Why do the starters have no `<version>`?

The parent's dependency management already defines the version of every starter, so Maven reads the version from there. This keeps all libraries compatible with each other, and I can upgrade everything by changing only the parent version.

### What does `spring-boot-maven-plugin` do at package time?

It repackages the normal jar into an executable "fat jar". The jar contains my compiled classes, all dependency jars (in `BOOT-INF/lib`) and a launcher class. This is why I can start the application with `java -jar` without installing Tomcat.

### Dependency tree (first level)

Command: `./mvnw dependency:tree`

Verified direct dependencies:

```text
org.springframework.boot:spring-boot-starter-web:3.4.3:compile
org.springframework.boot:spring-boot-starter-validation:3.4.3:compile
org.springframework.boot:spring-boot-starter-jdbc:3.4.3:compile
org.springframework.boot:spring-boot-starter-aop:3.4.3:compile
org.springframework.boot:spring-boot-starter-actuator:3.4.3:compile
org.springframework.boot:spring-boot-devtools:3.4.3:runtime (optional)
com.h2database:h2:2.3.232:runtime
org.springframework.boot:spring-boot-starter-test:3.4.3:test
```

## Running the application

### With Maven

```bash
./mvnw spring-boot:run
curl -i "http://localhost:8080/hello?name=Ann"
curl http://localhost:8080/actuator/health
```

While the app runs, changing the message text in `HelloController` and saving makes DevTools restart the app automatically.

### As a packaged jar on port 9090

```bash
./mvnw -q package
java -jar target/shop-0.0.1-SNAPSHOT.jar --server.port=9090
curl -i "http://localhost:9090/hello?name=Ann"
```

Result:

```
HTTP/1.1 200
Content-Type: application/json

{"message":"Hello, Ann!","at":"2026-10-28T22:56:00"}
```

Actuator health:

```
{"status":"UP"}
```

### Jar size

`target/shop-0.0.1-SNAPSHOT.jar` is about **29 MB** (from `ls -lh target/*.jar`).
It is much bigger than my own code because it contains embedded Tomcat, Spring MVC, Jackson, H2, JDBC and the other dependencies, so it can run on its own.

## Auto-configuration

I started the app with `--debug` and looked at the condition evaluation report. Under "Positive matches":

```
DataSourceAutoConfiguration matched:
  @ConditionalOnClass found required classes
  'javax.sql.DataSource',
  'org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType'
  @ConditionalOnMissingBean did not find any beans

DataSourceConfiguration.Hikari matched:
  HikariDataSource class found.
  spring.datasource.type matched.
```

`DataSourceAutoConfiguration` matched because the JDBC and H2 classes are on the classpath, so Spring Boot configures a `DataSource` automatically without any manual configuration.

## Git

- Commit after each task.
- Tag for this task: `task-1`.
- `target/` is excluded by `.gitignore` and is not committed.

## Task 2: Dependency Injection and Bean Scopes

### Notifier implementations

`Notifier` defines the `send(to, text)` operation. `EmailNotifier` is the default implementation, while `SmsNotifier` handles urgent messages. `ConsoleNotifier` is active only when the `dev` profile is enabled.

- `@Primary` selects the default notifier when multiple implementations are available.
- `@Qualifier("smsNotifier")` explicitly injects `SmsNotifier` for urgent messages.
- `@Profile("dev")` activates `ConsoleNotifier` in development. `EmailNotifier` uses `@Profile("!dev")`, so it is inactive in the `dev` profile.

### Lifecycle callbacks

`EmailNotifier` and `SmsNotifier` use `@PostConstruct` to log when initialized and `@PreDestroy` to log when Spring shuts them down.

### Prototype scope and provider injection

`ReportBuilder` uses `@Scope("prototype")`. `WelcomeService` injects `ObjectProvider<ReportBuilder>` and requests two instances to verify that they are different objects.

### Clock bean and constructor injection

`AppConfig` defines a `Clock` bean. `WelcomeService` receives its dependencies through constructor injection, making its dependencies explicit and allowing a fixed clock to be used in tests.

### Verification

- Default profile: `EmailNotifier` handles Ann's normal welcome message; `SmsNotifier` handles Bob's urgent message.
- Development profile: `ConsoleNotifier` handles normal messages; `SmsNotifier` handles urgent messages.
- Prototype check: `prototype instances differ: true`.
- Tests: `./mvnw test` — **BUILD SUCCESS**, 2 tests, 0 failures, 0 errors.

## Task 3 — JDBC, JdbcClient and Transactions

### 1. Product Model and Database

Created a `Product` record containing `id`, `name`, `price`, and `stock`. Configured an H2 in-memory database and created the `product` table using `schema.sql`. The table enforces unique product names and prevents negative stock values. Five sample products are inserted through `data.sql`.

### 2. Raw JDBC vs. JdbcClient

Implemented two repository approaches:

- **Raw JDBC:** `RawJdbcProductRepository` uses `DataSource`, `PreparedStatement`, and `ResultSet`, with try-with-resources to close database resources.
- **JdbcClient:** `ProductRepository` uses Spring's `JdbcClient` for product queries, insertion, stock updates, and deletion.

Line-count comparison:

- Raw JDBC repository: 56 lines.
- JdbcClient repository: 95 lines.

The JdbcClient implementation reduces manual JDBC resource handling and provides a more concise approach to database operations.

### 3. Transactions and Rollback

Implemented `ProductService.restock()` with `@Transactional`. The service subtracts stock from a source product and adds it to a destination product. If either update fails, an exception causes the transaction to roll back.

The rollback tests verify that:

- A transfer exceeding the source product's available stock leaves both products unchanged.
- A transfer to a nonexistent destination restores the source product's original stock.

### 4. Testing and Exception Translation

Repository tests use `@JdbcTest` and `@Import(ProductRepository.class)`. Service tests use `@SpringBootTest`.

The tests cover product retrieval, searching, insertion, deletion, stock updates, duplicate product names, and transaction rollback.

A duplicate product name causes a `DuplicateKeyException`. Spring's JDBC exception-translation infrastructure converts the underlying database constraint violation into this Spring data-access exception.

### 5. Test Results

Command: `.\mvnw.cmd clean test`

Result: **BUILD SUCCESS**

- Tests run: 10
- Failures: 0
- Errors: 0
- Skipped: 0
