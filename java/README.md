# Java Migration — Investment Portfolio Management

Java/Spring Boot migration of the COBOL Legacy Benchmark Suite (CICS, DB2, VSAM, JCL, BMS investment portfolio system).

## Requirements

- Java 17+
- Maven 3.6+

## Module Layout

Multi-module Maven project (Spring Boot 3.x parent BOM), base package `com.benchmark.portfolio.<module>`:

| Module      | Purpose | COBOL counterparts |
|-------------|---------|--------------------|
| `common`    | Shared record types, copybook mappings, utilities (Spring Data JPA) | `src/copybook/common` (PORTFLIO, TRNREC, POSREC, HISTREC, PORTVAL, ERRHAND, RTNCODE) |
| `portfolio` | Portfolio domain services: master record CRUD, transactions, positions (Spring Data JPA) | `src/programs/portfolio` (PORTADD, PORTREAD, PORTUPDT, PORTDEL, PORTTRAN, PORTVALD) |
| `batch`     | Batch jobs (Spring Batch): position updates, transaction processing | `src/programs/batch` (POSUPDT, RTNCDE00), `src/jcl` |
| `online`    | Online/interactive services (Spring Web + Spring Security): inquiry API | `src/programs/online` (INQPORT, ERRHNDL), CICS/BMS maps |
| `reporting` | Reporting and history/audit jobs | DB2 history reporting |

## Build

```bash
cd java
mvn -q verify
```

## CI

`.github/workflows/java-ci.yml` runs `mvn -q verify` in `java/` on pull requests touching `java/**`.

## Conventions

- COBOL `COMP-3` / `PIC S9(n)V9(m)` → `java.math.BigDecimal` (never float/double for money); `PIC X(n)` → `String`; `PIC 9(n)` → `long`/`Integer`.
- COBOL paragraph names are preserved as camelCase method names for traceability.
- Domain naming uses "Portfolio" (e.g. `PortfolioService`).
