# Database Migrations

The PostgreSQL schema for the Java migration is managed with [Flyway](https://flywaydb.org/).

- **Executable migrations** live at `java/common/src/main/resources/db/migration/` (Flyway's conventional classpath location). `V1__baseline_schema.sql` is the relational baseline translated from the VSAM copybooks.
- **Design documentation** stays in this directory: `ERD.md`, `FIELD_MAPPINGS.md`, and the reviewed DDL in `ddl/` (kept as the design reference for ticket 0.2).

## Running migrations locally

1. Start PostgreSQL:

   ```bash
   docker run --name portfolio-db -d \
     -e POSTGRES_DB=portfolio \
     -e POSTGRES_USER=portfolio \
     -e POSTGRES_PASSWORD=portfolio \
     -p 5432:5432 postgres:16-alpine
   ```

2. Run migrations, either:

   - **Via Maven** (uses the `flyway-maven-plugin` managed in the root `java/pom.xml`):

     ```bash
     export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portfolio
     export SPRING_DATASOURCE_USERNAME=portfolio
     export SPRING_DATASOURCE_PASSWORD=portfolio
     cd java/common && mvn org.flywaydb:flyway-maven-plugin:migrate
     ```

   - **Via app startup**: any runnable Spring Boot module picks up the shared `application.yml` from the `common` module and runs Flyway automatically on startup against the configured datasource. The same `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` environment variables apply (defaults point at the local Docker database above).

## Tests

`FlywayMigrationTest` in the `common` module spins up a Testcontainers PostgreSQL instance, applies the migrations, and asserts every expected table exists and is empty. It requires Docker (available locally and on the GitHub Actions `ubuntu-latest` runners used by `java-ci.yml`).
