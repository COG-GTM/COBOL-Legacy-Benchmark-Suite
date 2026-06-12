# CLBS Java Migration

Java 17 / Spring Boot 3.x migration of the COBOL Legacy Benchmark Suite
(Investment Portfolio Management System). This directory contains **Phase 0 —
Foundation**: the buildable multi-module skeleton, the relational schema and
entities translated from the VSAM copybooks, repositories, and the parallel-run
comparison framework used to prove COBOL/Java equivalence.

## Modules

| Module | Purpose | Migration target |
|---|---|---|
| `common` | COBOL type helpers (`CobolField`, `Comp3`), return codes, parallel-run comparison framework | COMMON.cpy / RTNCODE.cpy |
| `portfolio` | JPA entities, Spring Data repositories, Flyway schema | PORTFLIO/TRNREC/POSREC/HISTREC + VSAM KSDS |
| `batch` | Spring Batch tier (skeleton job) | POSUPDT, HISTLD00, RTNCDE00, ... |
| `online` | REST inquiry API + Spring Security baseline (bootable app) | CICS INQONLN/INQPORT/INQHIST |
| `reporting` | Report generation | RPTPOS00/RPTAUD00/RPTSTA00 |

## Build & test

```bash
cd java
./mvnw verify
```

H2 (in PostgreSQL mode) + Flyway are used for dev/CI so the suite runs with no
external database. The `postgres` Spring profile targets PostgreSQL for
production (`DB_URL` / `DB_USERNAME` / `DB_PASSWORD`).

## Run the online tier

```bash
cd java
./mvnw -pl online -am spring-boot:run
# GET http://localhost:8080/api/portfolios/{portId}
# GET http://localhost:8080/api/portfolios/{portId}/transactions
```

## Key docs
- `docs/field-mappings.md` — copybook → DDL/entity field mappings + ERD (task 0.2).

## Test data (task 0.7)
`TSTGEN00.cbl` is a non-runnable skeleton (8 undefined paragraphs), so golden
fixtures are generated deterministically by
`com.clbs.portfolio.testdata.GoldenFixtures` and committed under
`portfolio/src/test/resources/fixtures/` in the exact copybook fixed-width
layout. Regenerate with:

```bash
cd java
./mvnw -pl portfolio -am test-compile
java -cp "portfolio/target/test-classes:common/target/classes" \
  com.clbs.portfolio.testdata.GoldenFixtures portfolio/src/test/resources/fixtures
```

## Parallel-run comparison framework (task 0.6)
`com.clbs.common.parallelrun.RecordComparator` diffs legacy (COBOL) output
against migrated (Java) output record-by-record and field-by-field, producing a
`ComparisonResult` report. See `GoldenFixtureTest` for an end-to-end example.
