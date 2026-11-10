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


