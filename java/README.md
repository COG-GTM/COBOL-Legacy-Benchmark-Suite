# CLBS Java Migration

Target implementation of the COBOL Legacy Benchmark Suite on **Java 21 / Spring Boot**. This
directory is a self-contained Maven multi-module build; the COBOL sources it replaces stay in
[`../src`](../src) and remain the behavioural reference for every migrated program.

## Prerequisites

| Tool  | Version | Notes                                                              |
| ----- | ------- | ------------------------------------------------------------------ |
| JDK   | 21+     | Any distribution (Temurin, Corretto, OpenJDK). `java -version`.    |
| Maven | 3.9+    | Not required: use the bundled wrapper `./mvnw` (`mvnw.cmd` on Windows). |

No database, container runtime, or other service is needed to build and test.

## Quick start

```bash
cd java
./mvnw verify            # build + format check + lint + unit tests + coverage gate
./mvnw spotless:apply    # auto-format before committing
```

`./mvnw verify` is exactly what CI runs, so a green local build means a green PR.

### Common commands

| Task                                     | Command                                                  |
| ---------------------------------------- | -------------------------------------------------------- |
| Full quality gate (what CI runs)         | `./mvnw verify`                                          |
| Compile + unit tests only                | `./mvnw test`                                            |
| Fix formatting                           | `./mvnw spotless:apply`                                  |
| Check formatting only                    | `./mvnw spotless:check`                                  |
| Lint only                                | `./mvnw -DskipTests checkstyle:check`                    |
| Build one module and its dependencies    | `./mvnw -pl clbs-portfolio -am verify`                   |
| Run a single test class                  | `./mvnw -pl clbs-test-harness test -Dtest=GoldenComparatorTest` |
| Regenerate golden files (see below)      | `./mvnw test -Dclbs.golden.update=true`                  |
| Run the application                      | `./mvnw -pl clbs-app spring-boot:run`                    |
| Build an executable jar                  | `./mvnw -DskipTests package` then `java -jar clbs-app/target/clbs-app-*.jar` |

Coverage reports land in `<module>/target/site/jacoco/index.html`, with a cross-module aggregate in
`clbs-coverage/target/site/jacoco-aggregate/index.html`.

If your network blocks `repo.maven.apache.org`, point the wrapper at a mirror:
`MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2 ./mvnw verify` and add
the same URL as a `<mirror>` in `~/.m2/settings.xml`.

## Module layout

Modules mirror the COBOL domain folders under `src/programs` so each migration ticket maps to one
module. Every module shares the `com.cognition.clbs` base package.

| Module              | Package                        | Mirrors                                        | Depends on              |
| ------------------- | ------------------------------ | ---------------------------------------------- | ----------------------- |
| `clbs-common`       | `com.cognition.clbs.common`    | `src/programs/common`, `src/copybook`, `src/database` | Spring Data JPA, Flyway |
| `clbs-portfolio`    | `com.cognition.clbs.portfolio` | `src/programs/portfolio`                       | common                  |
| `clbs-batch`        | `com.cognition.clbs.batch`     | `src/programs/batch`, `src/jcl` (Spring Batch) | common, portfolio       |
| `clbs-online`       | `com.cognition.clbs.online`    | `src/programs/online`, `src/maps` (Spring MVC) | common, portfolio       |
| `clbs-reporting`    | `com.cognition.clbs.reporting` | `RPT*` programs                                | common, portfolio       |
| `clbs-utility`      | `com.cognition.clbs.utility`   | `src/programs/utility`                         | common                  |
| `clbs-app`          | `com.cognition.clbs`           | Spring Boot entry point wiring all domains     | all of the above        |
| `clbs-test-harness` | `com.cognition.clbs.testing`   | `src/programs/test` (golden-file comparisons)  | JUnit 5, AssertJ (test scope everywhere) |
| `clbs-coverage`     | –                              | JaCoCo aggregate report                        | all modules             |

Dependency direction is enforced by Maven: a module can only see what it declares, so e.g.
`clbs-portfolio` cannot reach into `clbs-batch`.

### Data layer (`clbs-common`)

| Package / path                                   | Contents                                                                                          |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------- |
| `common.cobol`                                   | Fixed-width codec: `PackedDecimal` (COMP-3), `BinaryField` (COMP), `RecordReader`/`RecordWriter`, ASCII + IBM-1047 |
| `common.copybook.{common,batch,db2,online}`      | One Java `record`/enum per copybook with its exact `LENGTH`; `BigDecimal` for every COMP-3 field |
| `src/main/resources/db/migration`                | Flyway `V1..V6`: DB2 DDL translated to portable SQL, VSAM files and the audit log as tables        |
| `common.persistence.{entity,repository,mapper}`  | JPA entities with composite keys mirroring VSAM keys, Spring Data repositories, record<->entity mappers |

Hibernate runs with `ddl-auto=validate`, so the entities are checked against the Flyway schema at
start-up and in `EntityRoundTripTest`. The embedded H2 datasource in `clbs-app/application.yml` is
a local default; set `SPRING_DATASOURCE_URL` (and credentials) for a real database. See
[ADR 0002](../docs/adr/0002-relational-data-layer-jpa-flyway.md).

## Quality gates

All gates run in `./mvnw verify` and fail the build:

| Gate           | Tool                                   | Where configured                              |
| -------------- | -------------------------------------- | --------------------------------------------- |
| Formatting     | Spotless + google-java-format          | `pom.xml` (`spotless-maven-plugin`)           |
| Lint           | Checkstyle                             | `config/checkstyle/checkstyle.xml`            |
| Unit tests     | JUnit 5, AssertJ, Mockito (Surefire)   | `spring-boot-starter-test` in every module    |
| Coverage       | JaCoCo, **80 % line coverage per module** | `jacoco.minimum.line.coverage` in `pom.xml` |
| Toolchain      | Maven Enforcer (JDK 21+, Maven 3.9+, dependency convergence) | `pom.xml`           |

Formatting is owned entirely by Spotless; Checkstyle only covers correctness and design smells a
formatter cannot fix. `*Application` classes and `package-info` are excluded from coverage.

## Golden-file testing

COBOL programs are validated by comparing their output byte-for-byte with baselines. The
`clbs-test-harness` module provides the same mechanism for the Java ports.

```java
@ExtendWith(GoldenFileExtension.class)
class RptPos00Test {

  @Test
  void printsPositionReport(GoldenFile golden) {
    String report = new PositionReport(fixtures()).render();
    golden.assertText(report);                 // src/test/resources/golden/RptPos00Test/printsPositionReport.txt
  }

  @Test
  void writesPositionExtract(GoldenFile golden) {
    byte[] records = new PositionExtractWriter(fixtures()).write();
    golden.assertRecords(records, 200);        // fixed-length 200-byte records, .dat
  }
}
```

- **Text comparison** (`assertText`) is line-based. By default line endings are normalised and
  trailing blank lines ignored, but trailing whitespace is significant because COBOL report lines
  are space-padded. Use `TextOptions.strict()` or `.ignoringTrailingWhitespace()` to adjust.
- **Record comparison** (`assertRecords`) treats both sides as fixed-length records (LRECL) and
  reports the record number and byte offset of the first mismatch. Partial trailing records always
  fail.
- Failures print a unified report of the first 25 differences plus the golden path.
- `GoldenFiles.assertMatchesText/Records(Path, ...)` and `GoldenComparator` are available for
  tests that do not use the extension.

Golden files live in `src/test/resources/golden/<TestClass>/<testMethod>.{txt,dat}`. To create or
intentionally regenerate them run

```bash
./mvnw test -Dclbs.golden.update=true      # or CLBS_GOLDEN_UPDATE=true
```

then review the resulting diff and commit it. `.gitattributes` marks golden files as binary so git
never rewrites their line endings.

## CI

[`.github/workflows/java-ci.yml`](../.github/workflows/java-ci.yml) runs on every pull request and
push to `main` that touches `java/`: format check, lint, `./mvnw verify` (build, unit tests,
coverage gate), and uploads Surefire reports and the aggregate JaCoCo report as artifacts.
Dependabot keeps Maven dependencies and GitHub Actions current
([`.github/dependabot.yml`](../.github/dependabot.yml)).

## Adding code to a module

1. Put production classes under `clbs-<domain>/src/main/java/com/cognition/clbs/<domain>/`.
2. Put tests under `clbs-<domain>/src/test/java/...`; golden files under
   `clbs-<domain>/src/test/resources/golden/`.
3. Declare any new inter-module dependency in the module `pom.xml` (versions are managed in the
   parent `dependencyManagement`).
4. Run `./mvnw spotless:apply verify` before pushing.
