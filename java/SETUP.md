# CLBS Java Migration - Setup Guide

## Overview

This directory contains the Java target foundation for the COBOL Legacy Benchmark Suite (CLBS) investment portfolio migration.

- **Build tool**: Maven (wrapper included in `java/`)
- **Java version**: 17 (required for Spring Boot 3.2+)
- **Framework**: Spring Boot 3.2.x with Spring Batch and Spring Data JPA
- **Databases**: H2 for fast tests, PostgreSQL for development

## Quick start

```bash
cd /home/ubuntu/repos/COBOL-Legacy-Benchmark-Suite/java
./mvnw test
```

To compile the project:

```bash
./mvnw compile
```

To run the application with the H2 profile (default):

```bash
./mvnw spring-boot:run
```

## Profiles

The project uses Spring profiles for environment selection.

### `h2` (default)

- In-memory H2 database for unit tests and local exploration.
- DDL is generated automatically (`spring.jpa.hibernate.ddl-auto=create-drop`).
- H2 console is disabled by default for security. Enable it locally by setting `SPRING_H2_CONSOLE_ENABLED=true`.

### `dev`

- Connects to a local PostgreSQL instance.
- Default connection string: `jdbc:postgresql://localhost:5432/clbs`
- Default credentials: `clbs` / `clbs`
- Hibernate `ddl-auto` is set to `update` so the schema is created/updated from entities.

To run with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

or

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

## Testing

The golden-master test suite is in `src/test/java/com/cog/gtm/clbs/migration/golden/`.
Fixtures are in `src/test/resources/fixtures/portfolio-validation/`.

```bash
./mvnw test
```

## Project layout

```
java/
├── pom.xml                           # Maven build
├── mvnw / mvnw.cmd                   # Maven wrapper
├── SETUP.md                          # This file
├── CONVENTIONS.md                    # COBOL -> Java mapping conventions
└── src/
    ├── main/
    │   ├── java/com/cog/gtm/clbs/migration/
    │   │   ├── MigrationApplication.java
    │   │   ├── domain/               # JPA entities from copybook/DB2 models
    │   │   ├── repository/           # Spring Data JPA repositories
    │   │   └── service/validation/   # PORTVALD Java port
    │   └── resources/
    │       ├── application.yml
    │       ├── application-h2.yml
    │       ├── application-dev.yml
    │       └── application-test.yml
    └── test/
        ├── java/.../golden/          # Golden-master test harness
        └── resources/fixtures/       # CSV input / expected output fixtures
```

## Notes

- Spring Batch jobs are disabled on startup (`spring.batch.job.enabled=false`) so the harness application can be exercised without running batch jobs.
- Spring Batch metadata tables are created automatically by `spring.batch.jdbc.initialize-schema=always` for the H2 profile.
