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
Command: `./mvnw dependency:tree | head -40`

```
<<< PASTE THE OUTPUT HERE >>>
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

{"message":"Hello, Ann!","at":"<<< your timestamp >>>"}
```

Actuator health:
```
{"status":"UP"}
```

### Jar size
`target/shop-0.0.1-SNAPSHOT.jar` is about **<<< XX MB >>>** (from `ls -lh target/*.jar`).
It is much bigger than my own code because it contains embedded Tomcat, Spring MVC, Jackson, H2, JDBC and the other dependencies, so it can run on its own.

## Auto-configuration

I started the app with `--debug` and looked at the condition evaluation report. Under "Positive matches":

```
<<< PASTE THE DataSourceAutoConfiguration LINES HERE >>>
```

`DataSourceAutoConfiguration` matched because the JDBC and H2 classes are on the classpath, so Spring Boot configures a `DataSource` automatically without any manual configuration.

## Git
- Commit after each task.
- Tag for this task: `task-1`.
- `target/` is excluded by `.gitignore` and is not committed.