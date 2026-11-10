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

- Verify Money embedded values are persisted correctly.
- Verify enum values are persisted as strings.
- Verify Flyway migration and Hibernate schema validation.

### 6. Schema Mismatch Experiment 1 — Column Name

**Objective:** Demonstrate that Hibernate detects a mismatch between the entity mapping and database column name.

- Original configuration:
- Experimental change:
- Observed error:
- Explanation:
- Fix:

### 7. Schema Mismatch Experiment 2 — Column Type

**Objective:** Demonstrate that Hibernate detects a mismatch between the entity field type and database column type.

- Original configuration:
- Experimental change:
- Observed error:
- Explanation:
- Fix:

**Important:** Both experimental changes were reverted after testing.

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

