# CLBS Portfolio — Java Target

Java target project for migrating the COBOL Investment Portfolio Management System
(this repository) to Java. This is the **Phase 0 scaffold** (Jira MBA-1928, story
MBA-1920, epic MBA-1919): a buildable Spring Boot skeleton with Spring Batch and
Spring Data JPA wired. Business logic is migrated in later phases.

## Stack

- Java 17
- Spring Boot 3.3.x (`spring-boot-starter-parent`)
- Spring Web (REST/services), Spring Batch (batch jobs), Spring Data JPA (persistence)
- Build: Maven (wrapper included — `./mvnw`)
- Default dev/test database: in-memory H2 (final RDBMS selection tracked in MBA-1929)

## Package structure

```
com.cognition.clbs
├── ClbsApplication          # Spring Boot entry point
├── config                   # Spring configuration (BatchConfig, ...)
├── web                      # REST controllers (migrated CICS online layer)
├── service                  # Business logic (migrated PROCEDURE DIVISION paragraphs)
├── batch                    # Spring Batch jobs/steps (migrated batch programs)
├── persistence
│   ├── entity               # JPA entities (migrated VSAM/DB2 record layouts)
│   └── repository           # Spring Data repositories
└── common                   # Shared utilities, error handling, return codes
```

## Build & test

```bash
cd java
./mvnw verify
```

## Run

```bash
cd java
./mvnw spring-boot:run
```

Then:

- `GET http://localhost:8080/` — application info
- `GET http://localhost:8080/actuator/health` — health check

Spring Batch jobs do not run on startup (`spring.batch.job.enabled=false`); they
are launched explicitly by later phases.
