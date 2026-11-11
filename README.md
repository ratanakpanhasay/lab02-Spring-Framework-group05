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
- V2__indexes.sql: Database indexes

## Project Tasks

- Setup (S0)
- Task 1: Entities and mappings
- Task 2: Associations, cascading and entity identity
- Task 3: Repositories, queries and REST APIs
- Task 4: Transactions, dirty checking and optimistic locking
- Task 5: Performance optimization and database migrations

# LAB 03 — SHOP JPA

## Project Information

- **Project:** Shop JPA
- **Repository:** lab02-Spring-Framework-group05
- **Integration Branch:** lab03
- **Group ID:** edu.itc.se
- **Artifact ID:** shop-jpa
- **Base Package:** edu.itc.shop
- **Java:** 25
- **Spring Boot:** 4.1.1

## Task 1 — Entities and Mappings

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

- `embeddedMoneyIsPersisted()` — Verifies that embedded Money values are persisted and retrieved correctly.
- `enumCategoryIsSavedAsString()` — Verifies that the Category enum is stored as a string in the database.
- `embeddedAddressIsPersisted()` — Verifies that embedded Address values are persisted and retrieved correctly.

Additionally, `ShopJpaApplicationTests.contextLoads()` verifies successful application context initialization. With Flyway enabled and Hibernate configured with `ddl-auto=validate`, application startup also exercises schema validation.

All four tests passed after restoring the original mappings. The two schema mismatch experiments separately confirmed that Hibernate detects missing columns and incompatible column types.

### 6. Schema Mismatch Experiment 1 — Column Name

**Objective:** Demonstrate that Hibernate detects a mismatch between the entity mapping and database column name.

- **Original configuration:** The `Product` entity defines a Java field `private String name;`, which maps to the `name` column in the `product` table created by `V1__init.sql`.
- **Experimental change:** Temporarily added `@Column(name = "product_name")` above the `name` field in `Product.java`, without modifying `V1__init.sql`.
- **Observed error:** Running `mvn clean verify` produced `BUILD FAILURE` (4 tests run, 0 assertion failures, 4 errors). Hibernate reported:

  ```text
  org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing column [product_name] in table [product]
  ```

- **Explanation:** Flyway created the table with a `name` column, but Hibernate expected a `product_name` column because of the temporary JPA annotation. With Hibernate DDL mode set to `validate`, schema validation failed while initializing the JPA `EntityManagerFactory`, preventing the test application contexts from loading.
- **Fix:** Removed the temporary `@Column(name = "product_name")` annotation and restored the original `name` field mapping. Ran `mvn clean verify` again and confirmed `BUILD SUCCESS`.

### 7. Schema Mismatch Experiment 2 — Column Type

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

