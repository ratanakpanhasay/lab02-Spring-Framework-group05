# Shop JPA

Spring Boot backend project for managing an online shop using Spring Data JPA and Hibernate.

## Technology Stack

- Java 25
- Spring Boot 4.1.1
- Maven
- Spring Data JPA
- Hibernate ORM
- Spring Web MVC
- Jakarta Validation
- H2 Database
- Flyway Database Migration

## Project Setup

The project uses Maven Wrapper.

Verify Java:

```bash
java -version
```

Run tests:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

## Database Configuration

- Database: H2 in-memory database
- Database URL: jdbc:h2:mem:shop
- Schema management: Flyway
- Hibernate DDL mode: validate
- Open Session in View: disabled

H2 Console:

http://localhost:8080/h2-console

## Database Migrations

Flyway migration files are stored in:

`src/main/resources/db/migration/`

- V1__init.sql: Initial database schema
- V1_1__product_sku_stock.sql: Task 3 product SKU and stock support
- V2__indexes.sql: Reserved for Task 5 database indexes (not present on this branch)

## Project Tasks

- Setup (S0)
- Task 1: Entities and mappings
- Task 2: Associations, cascading and entity identity
- Task 3: Repositories, queries and REST APIs
- Task 4: Transactions, dirty checking and optimistic locking
- Task 5: Performance optimization and database migrations

# LAB 03 â€” SHOP JPA

## Project Information

- **Project:** Shop JPA
- **Repository:** lab02-Spring-Framework-group05
- **Integration Branch:** lab03
- **Group ID:** edu.itc.se
- **Artifact ID:** shop-jpa
- **Base Package:** edu.itc.shop
- **Java:** 25
- **Spring Boot:** 4.1.1

## Task 1 â€” Entities and Mappings

**Responsible Member:** Member 1

### 1. Implementation

Implemented the following embeddable records:

- Money
- Address

Implemented the following enums:

- Category
- OrderStatus

Implemented the following JPA entities:

- Customer
- Product
- Tag
- Order
- OrderLine

### 2. JPA Mappings

- Used sequence-based identifiers for all five entities.
- Configured allocationSize = 50.
- Used @Embedded for Money and Address.
- Used @Enumerated(EnumType.STRING) for enum fields.
- Configured entity-to-table mappings using @Entity and @Table.

### 3. Database Migration

Created Flyway migration:

`V1__init.sql`

The migration creates:

- Five database sequences.
- Six database tables.
- Primary key constraints.
- Required foreign key constraints.
- Required column constraints.

### 4. Hibernate and Flyway Configuration

- Database: H2
- Schema migration: Flyway
- Hibernate DDL mode: validate

Flyway creates the database schema, and Hibernate validates the entity mappings against the database.

### 5. ProductMappingTest

The following tests were implemented:

- `embeddedMoneyIsPersisted()` â€” Verifies that embedded Money values are persisted and retrieved correctly.
- `enumCategoryIsSavedAsString()` â€” Verifies that the Category enum is stored as a string in the database.
- `embeddedAddressIsPersisted()` â€” Verifies that embedded Address values are persisted and retrieved correctly.

Additionally, `ShopJpaApplicationTests.contextLoads()` verifies successful application context initialization. With Flyway enabled and Hibernate configured with `ddl-auto=validate`, application startup also exercises schema validation.

All four tests passed after restoring the original mappings. The two schema mismatch experiments separately confirmed that Hibernate detects missing columns and incompatible column types.

### 6. Schema Mismatch Experiment 1 â€” Column Name

**Objective:** Demonstrate that Hibernate detects a mismatch between the entity mapping and database column name.

- **Original configuration:** The `Product` entity defines a Java field `private String name;`, which maps to the `name` column in the `product` table created by `V1__init.sql`.
- **Experimental change:** Temporarily added `@Column(name = "product_name")` above the `name` field in `Product.java`, without modifying `V1__init.sql`.
- **Observed error:** Running `mvn clean verify` produced `BUILD FAILURE` (4 tests run, 0 assertion failures, 4 errors). Hibernate reported:

  ```text
  org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing column [product_name] in table [product]
  ```

- **Explanation:** Flyway created the table with a `name` column, but Hibernate expected a `product_name` column because of the temporary JPA annotation. With Hibernate DDL mode set to `validate`, schema validation failed while initializing the JPA `EntityManagerFactory`, preventing the test application contexts from loading.
- **Fix:** Removed the temporary `@Column(name = "product_name")` annotation and restored the original `name` field mapping. Ran `mvn clean verify` again and confirmed `BUILD SUCCESS`.

### 7. Schema Mismatch Experiment 2 â€” Column Type

**Objective:** Demonstrate that Hibernate detects a mismatch between the entity field type and database column type.

- **Original configuration:** `Product.java` defines `name` as a Java `String`; `V1__init.sql` defines the corresponding column as `name VARCHAR(150) NOT NULL`.
- **Experimental change:** Temporarily changed the column in `V1__init.sql` to `name INTEGER NOT NULL`, without changing `Product.java`.
- **Observed error:** Running `mvn clean verify` produced `BUILD FAILURE` (4 tests run, 0 assertion failures, 4 errors). Hibernate reported:

  ```text
  org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: wrong column type encountered in column [name] in table [product]; found [integer (Types#INTEGER)], but expecting [varchar(255) (Types#VARCHAR)]
  ```

- **Explanation:** Flyway created the column with SQL type `INTEGER`, whereas Hibernate expected a `VARCHAR` type for the Java `String` field. The type mismatch was detected during Hibernate schema validation, so the JPA `EntityManagerFactory` could not initialize and the application context tests errored.
- **Fix:** Restored the migration column to `name VARCHAR(150) NOT NULL`. Ran `mvn clean verify` again and confirmed `BUILD SUCCESS`.

**Important:** Both experimental changes were reverted after testing. The final project retains the original entity mapping and Flyway migration.

### 8. How to Run

Build the project:

```bash
./mvnw clean verify
```

Run tests:

```bash
./mvnw test
```

Run the application:

```bash
./mvnw spring-boot:run
```

### 9. Task 1 Completion

Task 1 implementation and testing are completed after all acceptance criteria pass.


## Task 2 â€” Associations, Cascading and Entity Identity

### 1. Implementation

Implemented the following relationships:

- Orderâ€“OrderLine: bidirectional one-to-many / many-to-one.
- Productâ€“Tag: bidirectional many-to-many.

### 2. Order Aggregate Root

The Order entity manages its order lines through:

- `addLine(Product product, int quantity)` â€” Adds a line, sets the owning side, and copies the product's current price.
- `removeLine(OrderLine line)` â€” Removes a line and updates the relationship.
- `getLines()` â€” Returns a read-only view.
- `total()` â€” Calculates the total order amount.

Configured:

- `cascade = CascadeType.ALL`
- `orphanRemoval = true`

### 3. Productâ€“Tag Relationship

- Product owns the relationship through `@JoinTable`.
- Tag uses `mappedBy = "tags"` as the inverse side.
- `Product.tag(Tag tag)` maintains both sides.
- Join table: `product_tag`.

### 4. Entity Identity

Implemented `equals()`, `hashCode()`, and `toString()` for all five entities:

- Customer
- Product
- Tag
- Order
- OrderLine

Entity equality is based on the same entity class and the same non-null database identifier.

The hash code remains stable before and after persistence.

The `toString()` methods use scalar fields only and exclude entity relationships to prevent recursive calls and unexpected lazy loading.

### 5. Tests

Created:

- `OrderCascadeTest`
- `EntityIdentityTest`

Existing tests:

- `ShopJpaApplicationTests`
- `ProductMappingTest`

All tests passed.

### 6. Order Cascade SQL Evidence

The `OrderCascadeTest` confirmed the following SQL operations.

**Initial persistence:**

- One INSERT into `orders`.
- Two INSERTs into `order_line`.
- No UPDATE statements during initial order persistence.

**Orphan removal:**

When one order line is removed, Hibernate executes:

```sql
delete from order_line where id=?
```

**Cascade remove:**

When the order is deleted, Hibernate deletes its remaining line first, followed by the order:

```sql
delete from order_line where id=?
delete from orders where id=? and version=?
```

The test verifies that no order lines remain after deleting the order.

### 7. Entity Identity Test

The `EntityIdentityTest` verifies that:

- A Product remains findable in a HashSet after persistence.
- Reloading the Product creates a different Java object.
- The original and reloaded objects are equal.
- The hash code remains stable after persistence.

### 8. toString() Recursion Experiment

**Objective:** Demonstrate the problem caused by including bidirectional relationships in entity `toString()` methods.

**Experimental changes:**

1. Temporarily included `lines` in `Order.toString()`.
2. Temporarily included `order` in `OrderLine.toString()`.
3. Called `toString()` on an order.

**Observed result:**

The two methods recursively called each other until Java threw a `StackOverflowError`. This behavior was verified using a JUnit test with:

```java
assertThrows(StackOverflowError.class, order::toString);
```

**Solution:**

Restored both methods to use scalar fields only, excluding entity relationships.

**Conclusion:**

Entity relationships should generally be excluded from `toString()` to avoid infinite recursion and unexpected lazy loading.

### 9. Task 2 Verification

Run the complete test suite:

```bash
./mvnw clean test
```

**Result:** BUILD SUCCESS.

Task 2 implementation and verification are completed.

## Task 3 â€” Repositories, Queries, Projections and REST APIs

Implemented and verified on `task-3` on 2026-11-11 using Java 25, Maven 3.10.0,
Spring Boot 4.1.1, Spring Data JPA 4.1.1, Hibernate 7.4.5.Final and H2 2.4.240.
No commits, pushes, merges or branch switches were performed.

### Repositories

All repositories extend `JpaRepository` and use the existing entities and sequence mappings.

| Repository / method | Behavior |
| --- | --- |
| `CustomerRepository.findByEmail(email)` | Derived exact email lookup returning `Optional<Customer>`. |
| `OrderRepository.findByCustomerEmailAndStatusOrderByPlacedAtDesc(email, status)` | Traverses `customer.email`, filters by status and returns newest orders first. |
| `OrderRepository.countByStatus(status)` | Counts orders with the given status. |
| `OrderRepository.findFirstByCustomerIdOrderByPlacedAtDesc(customerId)` | Returns the latest order for a customer, or an empty Optional. |
| `OrderRepository.findByStatus(status, pageable)` | Returns a page of interface projections, including nested customer name. |
| `OrderRepository.findAllProjectedBy(pageable)` | Same projection for the endpoint when no status is supplied. |
| `OrderRepository.findWithLinesByCustomerEmail(email)` | JPQL fetches customer, lines and each line's product in one query. Left fetch joins preserve orders with no lines; `distinct` removes duplicate roots. Results are newest first, then descending ID for ties. |
| `OrderRepository.countLinesByStatus(status)` | JPQL aggregate counts line rows, rather than orders or the sum of quantities. |
| `ProductRepository.existsBySku(sku)` | Derived SKU existence check. |
| `ProductRepository.findBySku(sku)` | Optional lookup used to reuse seeded products. |
| `ProductRepository.findTop3ByCategoryOrderByPriceAmountDesc(category)` | Traverses embedded `price.amount`, returning up to three most expensive products in a category. |
| `ProductRepository.findByTagsName(name)` | Traverses the many-to-many tags association and filters by tag name. |
| `ProductRepository.findLowStock(min)` | Native SQL selects products with `stock < min`, ordered by stock then ID. |
| `ProductRepository.findByCategory(category, pageable)` | JPQL constructor projection selects the five `ProductView` fields, including `price.amount`; an explicit count query supports paging. |
| `ProductRepository.findAll(specification, pageable)` | Inherited from `JpaSpecificationExecutor<Product>`; used for the product search endpoint. |
| `TagRepository.findByName(name)` | Reuses existing tags during development seeding. |

Every explicitly declared JPQL/native parameter uses `@Param`. Global association
fetching remains lazy; fetch joins are limited to the customer-orders query.

### Product specifications and filter semantics

`ProductSpecs.filter()` combines optional predicates with `Specification.allOf()`:

- `nameContains(q)`: case-insensitive substring match; blank input is unrestricted.
  SQL wildcard characters `%`, `_` and the escape character are treated literally.
- `inCategory(category)`: exact enum equality.
- `cheaperThan(maxPrice)`: **strictly less than** the supplied amount; the boundary
  is excluded, matching the method name.
- `inStock()`: stock greater than zero.
- `inStock=true` applies `inStock()`; `false` selects stock equal to zero; absent
  stock, category, price and name filters impose no restriction.

### DTOs, services and controllers

`OrderSummary` is a closed interface projection with ID, status, placed-at time,
and a nested `CustomerName` interface exposing only name. `OrderService.list()`
materializes it into `OrderSummaryResponse` (which implements the interface)
inside the read-only transaction, so JSON serialization never needs a live
projection target or a lazy customer association.

`ProductView` is the required five-field record: ID, SKU, name, amount and stock.
`ProductService.search()` applies the specifications and maps each product to
this DTO within its read-only transaction. The category repository method
separately demonstrates a constructor-based record projection over embedded Money.

`OrderDetail` contains order metadata, customer name/email, order lines and total.
`OrderLineView` contains line ID, product ID/SKU/name, quantity and a price snapshot.
`MoneyView` contains amount and currency. `OrderService.customerOrders()` explicitly
uses `@Transactional(readOnly = true)` and the required fetch-join query to build
all DTOs before the persistence context closes.

All services and controllers use constructor injection. Controllers only bind
request parameters and delegate; no controller returns a JPA entity. The existing
`spring.jpa.open-in-view=false` setting is unchanged.

| Controller / endpoint | Parameters and response |
| --- | --- |
| `OrderController`: `GET /api/orders` | Optional `status`, `page`, `size`, `sort`; returns `Page<OrderSummary>`. Defaults: page 0, size 20, `placedAt,desc`. No status returns all orders. |
| `ProductController`: `GET /api/products` | Optional `q`, `category`, `maxPrice`, `inStock`, `page`, `size`; returns `Page<ProductView>`. Defaults: page 0, size 20, ID ascending. |
| `CustomerController`: `GET /api/customers/{email}/orders` | Returns a list of complete `OrderDetail` DTOs. Unknown email returns an empty list. |

Page numbers are zero-based. Invalid enum values return HTTP 400 using Spring MVC's
standard parameter conversion. Full real requests and response bodies, including
pagination and filter examples, are recorded in [api-session.md](api-session.md).

### Compatible domain and schema changes

The inspected Task 2 `Product` and V1 schema had neither SKU nor stock. The new
`V1_1__product_sku_stock.sql` adds:

- `sku VARCHAR(64) NOT NULL`, with a unique constraint. Existing rows receive
  `LEGACY-<id>` values, so the migration works on populated V1 databases.
- `stock INT DEFAULT 0 NOT NULL`, with a database `CHECK (stock >= 0)` constraint.

`V1__init.sql` is unchanged. Flyway orders the migrations as **V1 â†’ V1.1 â†’ V2**.
Version 2 remains reserved for Task 5; no V2 migration exists on this branch.
Integrate V1.1 before applying the future V2 migration. The normal Flyway ordering
and validation settings remain enabled; out-of-order migration is not enabled.

`Product` adds only the corresponding fields, getters and a constructor accepting
explicit SKU and stock. The existing three-argument public constructor remains
available and assigns an `AUTO-<UUID>` SKU and zero stock. The unique database
constraint is the final uniqueness guarantee. Explicit blank/oversized SKUs and
negative stock are rejected by the constructor; the database also rejects negative
stock, duplicate SKUs and null SKUs. No association, identity or cascade mappings changed.

`Order` keeps its existing constructor and default `PENDING` status. An overload
accepts initial status and placement time for deterministic seed/test fixtures;
it rejects null status/time. No status-transition API is introduced.

The inspected enums did not contain the sample API values `HARDWARE` and `PAID`.
These were added without removing existing values. The existing string enum
mappings and VARCHAR schema already support them, so no enum-column migration is needed.

### Development seed data

`DataSeeder` is a `@Component`, `@Profile("dev")`, transactional `ApplicationRunner`.
On a fresh database it inserts 3 customers, 12 products (each with two shared tags),
30 orders and 90 lines (2â€“4 per order), with all six order statuses represented.
The fixture names are Dara, Sophea and Sokha; their emails are `<name>@example.com`.
Three products have zero stock. Times are fixed from `2026-01-01T00:00:00Z` so the
pagination examples are reproducible.

Repeated execution reuses customers by email, products by SKU and tags by name.
It skips creating an order batch for a fixture customer who already has orders.
The entire run is one transaction, so a failed run rolls back all of its writes.
This is a development fixture loader; it does not overwrite existing data or
replenish partially deleted order batches. Seeding is disabled by default and in
the original Task 1/2 tests. The HTTP integration tests explicitly enable `dev`
and use a separate in-memory database to verify seeding and repeatability.

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Required derived-query naming experiment

Temporarily added this method to `OrderRepository`:

```java
List<Order> findByCustomerEmali(String email);
```

Executed:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=0"
```

The actual startup attempt on 2026-11-11 at 18:17 (Asia/Bangkok) exited with code 1
and `BUILD FAILURE`. These are exact exception lines from that run:

```text
Caused by: org.springframework.data.repository.query.QueryCreationException: Cannot create query for method [OrderRepository.findByCustomerEmali(java.lang.String)]; No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
Caused by: org.springframework.data.core.PropertyReferenceException: No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
```

Spring Data successfully traversed `Order.customer`, then failed to resolve the
misspelled `emali` property during repository creation, preventing context startup.
The experimental method was removed immediately afterward. The restored source
passed the full build and the standalone application started successfully.

### Verification and actual results

Baseline, before edits: `mvn clean verify` passed **6 tests**, zero failures,
errors or skips, in 18.380 seconds.

Final verification, after removing the broken query:

```powershell
mvn clean verify
mvn test "-Dtest=OrderRepositoryTest"
```

```text
$ mvn clean verify
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.616 s -- in edu.itc.shop.domain.EntityIdentityTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.165 s -- in edu.itc.shop.domain.OrderCascadeTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.179 s -- in edu.itc.shop.domain.ProductMappingTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.713 s -- in edu.itc.shop.repository.OrderRepositoryTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.121 s -- in edu.itc.shop.repository.ProductSchemaMigrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.585 s -- in edu.itc.shop.ShopJpaApplicationTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.928 s -- in edu.itc.shop.web.ApiIntegrationTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  20.204 s
[INFO] Finished at: 2026-11-11T18:18:12+07:00

$ mvn test "-Dtest=OrderRepositoryTest"
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.266 s -- in edu.itc.shop.repository.OrderRepositoryTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  14.333 s
[INFO] Finished at: 2026-11-11T18:18:44+07:00
```

`OrderRepositoryTest` covers derived queries, aggregate counts, native low-stock
SQL, interface/record projections, optional specifications, literal wildcard
handling, pagination and sorting, and compatible SKU/stock initialization.
Its fetch-join test asserts `Hibernate.isInitialized(order.getLines())`, verifies
the mapped collection through `PersistenceUnitUtil.isLoaded(order, "lines")`, and
checks that products/customer are initialized. This extra check matters because
the existing getter returns an unmodifiable wrapper. A separate lookup in a
cleared persistence context verifies that ordinary associations remain lazy and
that detached line access throws `LazyInitializationException`.

`ProductSchemaMigrationTest` first applies V1, inserts legacy products, upgrades
to V1.1, checks backfilled SKUs/stock, tests database constraints and confirms a
second migration run applies no migrations. `ApiIntegrationTest` exercises a real
HTTP server with Open in View disabled, verifies page defaults and explicit sort,
combined product filters, line snapshots/totals, invalid enums, unknown customers,
and seed repeatability. All original tests were retained unchanged and passed.

The packaged app was started with Java and the `dev` profile on port 18083. Actual
startup log excerpts confirm Flyway order, Hibernate initialization with
`ddl-auto=validate`, and successful context startup:

```text
2026-11-11T18:18:32.145+07:00  INFO 14424 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "1 - init"
2026-11-11T18:18:32.183+07:00  INFO 14424 --- [           main] o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "1.1 - product sku stock"
2026-11-11T18:18:32.209+07:00  INFO 14424 --- [           main] o.f.core.internal.command.DbMigrate      : Successfully applied 2 migrations to schema "PUBLIC", now at version v1.1 (execution time 00:00.044s)
2026-11-11T18:18:34.109+07:00  INFO 14424 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-11-11T18:18:35.664+07:00  INFO 14424 --- [           main] edu.itc.shop.ShopJpaApplication          : Started ShopJpaApplication in 6.669 seconds (process running for 7.21)
```

Actual HTTP results (all HTTP 200, complete bodies in `api-session.md`):

| Request | Observed result |
| --- | --- |
| `/api/orders` | 20 DTOs from 30 orders; 2 pages, size 20, newest first. |
| `/api/orders?status=PAID&page=0&size=5&sort=placedAt,desc` | 5 paid orders, newest first. |
| `/api/orders?status=PAID&page=1&size=2&sort=placedAt,desc` | 2 DTOs from page 1 of 3; total 5. |
| `/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true` | Mechanical Keyboard (`DEV-001`, 55.00, stock 5) and USB Keyboard (`DEV-002`, 25.00, stock 6). |
| Same product filter with `page=1&size=1` | USB Keyboard; total 2 across 2 pages. |
| `/api/products?inStock=false` | 3 zero-stock product DTOs. |
| `/api/customers/dara@example.com/orders` | 10 complete orders with 2â€“4 lines each and calculated totals. |
| `/api/customers/missing@example.com/orders` | Empty list. |

Actual SQL excerpts from the build/application logs follow. The fetch join loads
the customer, lines and products together; product filtering uses a parameterized
predicate and database pagination; the native query and constructor projection
select the expected columns. These are captured statements, not reconstructed SQL:

```text
2026-11-11T18:18:06.489+07:00 DEBUG 24756 --- [           main] org.hibernate.SQL                        : SELECT * FROM product WHERE stock < ? ORDER BY stock, id
2026-11-11T18:18:06.700+07:00 DEBUG 24756 --- [           main] org.hibernate.SQL                        : select p1_0.id,p1_0.sku,p1_0.name,p1_0.price_amount,p1_0.stock from product p1_0 where p1_0.category=? order by p1_0.price_amount fetch first ? rows only
2026-11-11T18:18:06.857+07:00 DEBUG 24756 --- [           main] org.hibernate.SQL                        : select count(l1_0.id) from orders o1_0 join order_line l1_0 on o1_0.id=l1_0.order_id where o1_0.status=?
2026-11-11T18:19:09.354+07:00 DEBUG 14424 --- [io-18083-exec-2] org.hibernate.SQL                        : select distinct o1_0.id,o1_0.customer_id,c1_0.id,c1_0.street,c1_0.city,c1_0.country,c1_0.postal_code,c1_0.email,c1_0.name,l1_0.order_id,l1_0.id,l1_0.product_id,p1_0.id,p1_0.category,p1_0.name,p1_0.price_amount,p1_0.price_currency,p1_0.sku,p1_0.stock,l1_0.quantity,l1_0.unit_price_amount,l1_0.unit_price_currency,o1_0.placed_at,o1_0.status,o1_0.version from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id left join order_line l1_0 on o1_0.id=l1_0.order_id left join product p1_0 on p1_0.id=l1_0.product_id where c1_0.email=? order by o1_0.placed_at desc,o1_0.id desc
2026-11-11T18:19:09.298+07:00 DEBUG 14424 --- [o-18083-exec-10] org.hibernate.SQL                        : select p1_0.id,p1_0.category,p1_0.name,p1_0.price_amount,p1_0.price_currency,p1_0.sku,p1_0.stock from product p1_0 where lower(p1_0.name) like ? escape '\' and p1_0.category=? and p1_0.price_amount<? and p1_0.stock>? order by p1_0.id offset ? rows fetch first ? rows only
```

No functional failures remain. Non-blocking environment/framework notices observed:
Maven reports HTTP 401 metadata warnings for configured optional Flyway feeds;
Flyway reports that H2 2.4.240 is newer than its verified H2 version; Spring Data
warns that the default `PageImpl` JSON shape is not guaranteed across framework
upgrades. The requested `Page<...>` responses work and their current shape is
recorded and tested. No dependency versions or global page serialization settings
were changed for Task 3.

### File manifest and review handoff

Modified files:

```text
README.md
src/main/java/edu/itc/shop/domain/Product.java
src/main/java/edu/itc/shop/domain/Order.java
src/main/java/edu/itc/shop/domain/enums/Category.java
src/main/java/edu/itc/shop/domain/enums/OrderStatus.java
```

Created files:

```text
api-session.md
src/main/java/edu/itc/shop/repository/CustomerRepository.java
src/main/java/edu/itc/shop/repository/ProductRepository.java
src/main/java/edu/itc/shop/repository/OrderRepository.java
src/main/java/edu/itc/shop/repository/TagRepository.java
src/main/java/edu/itc/shop/repository/ProductSpecs.java
src/main/java/edu/itc/shop/service/ProductService.java
src/main/java/edu/itc/shop/service/OrderService.java
src/main/java/edu/itc/shop/service/DataSeeder.java
src/main/java/edu/itc/shop/web/ProductController.java
src/main/java/edu/itc/shop/web/OrderController.java
src/main/java/edu/itc/shop/web/CustomerController.java
src/main/java/edu/itc/shop/web/dto/ProductView.java
src/main/java/edu/itc/shop/web/dto/OrderSummary.java
src/main/java/edu/itc/shop/web/dto/OrderSummaryResponse.java
src/main/java/edu/itc/shop/web/dto/OrderDetail.java
src/main/java/edu/itc/shop/web/dto/OrderLineView.java
src/main/java/edu/itc/shop/web/dto/MoneyView.java
src/main/resources/db/migration/V1_1__product_sku_stock.sql
src/test/java/edu/itc/shop/repository/OrderRepositoryTest.java
src/test/java/edu/itc/shop/repository/ProductSchemaMigrationTest.java
src/test/java/edu/itc/shop/web/ApiIntegrationTest.java
```

Final Git status: `task-3`, 5 modified tracked files and 22 untracked new files;
no staged changes. V1, the Maven dependencies, application settings and the existing
tests are unchanged. Nothing has been committed or pushed.

Suggested commit message: `feat: implement lab03 task 3 repositories and REST APIs`

---

## Task 4 — Transactions, Dirty Checking and Optimistic Locking

**Responsible Member:** Member 5

### 1. Managed Entities and Dirty Checking

Implemented transactional order status updates in `OrderService`.

The `markPaid()` and `changeStatus()` methods retrieve an Order through `OrderRepository` and modify its status using `changeStatus()`.

No explicit `save()` call is required after modifying a managed entity. Hibernate detects the change through dirty checking and generates an UPDATE statement when the transaction is flushed or committed.

**Verification:** `OrderServiceTest` verifies that changing an order status through the service persists the new status.

### 2. Detached Entities and Merge

Implemented a detached entity experiment in `ProductDetachedEntityTest`.

The test:
- Creates and saves a Product.
- Loads the Product outside a surrounding transaction.
- Changes its stock from 10 to 99.
- Reloads the Product and confirms that the database still contains 10.
- Calls `products.save(p)` to merge the detached entity.
- Reloads the Product and confirms that the stock is now 99.

Spring Data JPA uses the merge operation for an existing detached entity when saving it.

**Verification:** `ProductDetachedEntityTest` passed.

### 3. Read-Only Transactions

Implemented `OrderService.view()` with `@Transactional(readOnly = true)`.

The method retrieves a managed Order, deliberately attempts to change its status to CANCELLED, and returns an `OrderDto`.

`ReadOnlyTransactionTest` verifies two different results:
- The returned DTO contains CANCELLED.
- A fresh database read still contains PENDING.

This demonstrates the read-only transaction behavior with the current Hibernate configuration. Read-only transactions are an optimization and should not be treated as a universal write-protection mechanism.

**Verification:** `ReadOnlyTransactionTest` passed.

### 4. Flush Ordering and Constraint Violations

Implemented `FlushOrderingTest` using the unique constraint on `Tag.name`.

Two scenarios are tested:

**Without an explicit flush:**
- Delete an existing Tag.
- Schedule insertion of another Tag with the same name.
- Flush the persistence context.
- Verify that the unique constraint violation is detected.

**With an explicit flush:**
- Delete the existing Tag.
- Call `tags.flush()` to execute the DELETE.
- Insert a replacement Tag with the same name.
- Verify that the replacement is successfully persisted.

The experiment demonstrates that Hibernate's SQL execution order is not necessarily the same as the order of Java method calls. Explicit flushing can be necessary when database constraints depend on statement ordering.

**Verification:** Both `FlushOrderingTest` scenarios passed.

### 5. Optimistic Locking with Concurrent Transactions

The Order entity uses:

`@Version private long version;`

This version field allows Hibernate to detect concurrent modifications.

`OptimisticLockingTest` uses two real threads and a `CountDownLatch` to ensure both transactions load the same Order version before attempting updates.

The test verifies:
- Both transactions load the same version.
- Both attempt to change the Order status.
- One transaction successfully updates the record.
- The competing transaction receives an `ObjectOptimisticLockingFailureException`.
- The version increases exactly once.

The test also verifies that the final status is either PAID or CANCELLED, depending on which transaction succeeds.

**Verification:** `OptimisticLockingTest` passed.

### 6. Retry Recovery with OrderFacade

Implemented `OrderFacade` separately from `OrderService`.

The facade calls `OrderService.changeStatus()` and catches `ObjectOptimisticLockingFailureException`.

It retries the complete service transaction, with a maximum of three attempts and a short increasing back-off delay.

The separation is important because the exception may occur during transaction commit, after the service method has executed.

`OrderFacadeTest` verifies two scenarios:

- Two failures followed by a successful third attempt.
- Three consecutive failures followed by propagation of the exception.

**Verification:** Both `OrderFacadeTest` scenarios passed.

### 7. SQL and Transaction Logging

Enabled Hibernate SQL and parameter logging in `application.yml`:

- `org.hibernate.SQL: DEBUG`
- `org.hibernate.orm.jdbc.bind: TRACE`
- `hibernate.generate_statistics: true`

Also enabled Spring transaction logging:

- `org.springframework.transaction: TRACE`
- `org.springframework.orm.jpa.JpaTransactionManager: DEBUG`

The captured evidence is saved in:

`task4-optimistic-locking.log`

The actual concurrency log shows two UPDATE statements containing the version condition:

```sql
update orders
set customer_id=?, placed_at=?, status=?, version=?
where id=? and version=?
```

The log also confirms that one transaction rolled back after the optimistic locking exception.

The successful concurrency test result was:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 8. Task 4 Test Summary

| Test class | Verification |
| --- | --- |
| OrderServiceTest | Dirty checking |
| ProductDetachedEntityTest | Detached entity and merge |
| ReadOnlyTransactionTest | Read-only transaction |
| FlushOrderingTest | Flush ordering and unique constraints |
| OptimisticLockingTest | Concurrent updates and optimistic locking |
| OrderFacadeTest | Retry and failure handling |

All six Task 4 test classes were executed individually and reported successful results during implementation.

### 9. Conclusion

Task 4 demonstrates how Spring transactions and Hibernate persistence-context behavior affect database updates.

The implementation covers managed and detached entities, dirty checking, read-only transactions, explicit flushing, optimistic locking and retry recovery.

The concurrency experiment confirms that version checking prevents lost updates when multiple transactions attempt to modify the same Order.


