# Investment Portfolio Management System — Java Migration

This project is the Java/Spring Boot migration target for the COBOL Legacy Benchmark Suite's Investment Portfolio Management System. It replaces the mainframe COBOL + CICS + DB2 + VSAM stack with a modern Spring Boot 3.x + Spring Batch 5.x application.

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+

### Build

```bash
mvn clean install
```

### Run (Development Mode — H2 in-memory DB)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application starts on `http://localhost:8080`. An H2 console is available at `http://localhost:8080/h2-console`.

### Run Tests

```bash
mvn test
```

## Architecture Overview

### COBOL → Java Mapping

| COBOL Source | Java Equivalent | Description |
|---|---|---|
| `PORTMSTR.cbl` | `PortfolioService` / `PortfolioController` | Portfolio CRUD operations |
| `PORTTRAN.cbl` | `TransactionService` | Transaction processing (Buy/Sell/Transfer/Fee) |
| `PORTVALD.cbl` | `ValidationService` | Input validation subroutines |
| `AUDPROC.cbl` + `AUDITLOG.cpy` | `AuditService` | Audit trail logging |
| `SECMGR.cbl` | `SecurityConfig` | Security and access control |
| `BCHCTL00` | `BatchConfig` + `BatchJobListener` | Batch job orchestration |
| `TRNVAL00` (JCL) | `TransactionValidationJobConfig` | Transaction validation batch job |
| `POSUPD00` (JCL) | `PositionUpdateJobConfig` | Position update batch job |
| `HISTLD00` (JCL) | `HistoryLoadJobConfig` | History load batch job |
| `RPTPOS00` (JCL) | `ReportGenerationJobConfig` | Report generation batch job |
| `INQONLN` (CICS) | `PortfolioController` / `TransactionController` | Online inquiry REST API |
| `ERRHAND.cpy` | `PortfolioException` hierarchy | Error handling (RC 4/8/12) |
| `PORTFLIO.cpy` | `Portfolio` (JPA entity) | Portfolio master record |
| `TRNREC.cpy` | `TransactionRecord` (JPA entity) | Transaction record |
| `POSREC.cpy` | `InvestmentPosition` (JPA entity) | Investment position record |
| `AUDITLOG.cpy` | `AuditRecord` (JPA entity) | Audit log record |
| `POSHIST.sql` | `PositionHistory` (JPA entity) | Position history table |
| VSAM files | Spring Data JPA + PostgreSQL/H2 | Persistence layer |
| DB2 host variables | JPA `@Column` annotations | Data access |
| JCL + `CKPRST.cpy` | Spring Batch (checkpoint/restart) | Batch processing |
| BMS Maps | REST API (JSON) | User interface layer |

### Data Type Mapping

| COBOL Type | Java Type | Notes |
|---|---|---|
| `PIC S9(13)V99 COMP-3` | `BigDecimal(15,2)` | Monetary amounts |
| `PIC S9(11)V9(4) COMP-3` | `BigDecimal(15,4)` | Quantities and prices |
| `PIC X(n)` | `String` with `@Column(length=n)` | Fixed-length character fields |
| `PIC 9(8)` (date) | `LocalDate` | Date fields (YYYYMMDD) |
| `PIC X(26)` (timestamp) | `LocalDateTime` | Timestamp fields |
| 88-level conditions | Java `enum` with `@Enumerated` | Status/type codes |
| Composite keys (POS-KEY) | `@EmbeddedId` | Multi-field primary keys |

### Project Structure

```
java-migration/
├── pom.xml                              # Maven build with Spring Boot 3.2.x parent
├── src/main/java/com/coggtm/portfolio/
│   ├── PortfolioApplication.java        # @SpringBootApplication entry point
│   ├── config/                          # Infrastructure configuration
│   ├── domain/                          # JPA entities + enums (from COBOL copybooks)
│   ├── repository/                      # Spring Data JPA repositories
│   ├── service/                         # Business logic interfaces + stubs
│   ├── batch/                           # Spring Batch job configurations
│   ├── api/                             # REST controllers
│   └── exception/                       # Exception hierarchy (ERRHAND.cpy mapping)
├── src/main/resources/
│   ├── application.yml                  # Default config
│   ├── application-dev.yml              # Dev profile (H2)
│   ├── application-prod.yml             # Prod profile (DB2)
│   └── db/migration/V1__initial_schema.sql  # Flyway migration (from DB2 DDL)
└── src/test/                            # Unit and integration tests
```

### Profiles

| Profile | Database | Flyway | Use Case |
|---|---|---|---|
| `dev` | H2 in-memory | Disabled (ddl-auto: create-drop) | Local development |
| `test` | H2 in-memory | Disabled (ddl-auto: create-drop) | Automated tests |
| `prod` | DB2 | Enabled | Production deployment |
| (default) | PostgreSQL | Enabled | Staging / alternative RDBMS |

## Migration Status

- [x] Project scaffold (pom.xml, directory structure)
- [x] JPA entities from COBOL copybooks (PORTFLIO, TRNREC, POSREC, AUDITLOG)
- [x] Enums from 88-level conditions
- [x] Flyway migration from DB2 DDL
- [x] Spring Data JPA repositories
- [x] Service interfaces (stubs) — PORTMSTR, PORTTRAN, PORTVALD, AUDPROC
- [x] Exception hierarchy from ERRHAND.cpy
- [x] Spring Batch job placeholders — TRNVAL00, POSUPD00, HISTLD00, RPTPOS00
- [x] REST controller placeholders — INQONLN mapping
- [x] Security configuration placeholder — SECMGR
- [x] Basic tests (context load, repository, validation)
- [ ] Full business logic — PORTMSTR.cbl EVALUATE block
- [ ] Full business logic — PORTTRAN.cbl transaction processing
- [ ] Full validation rules — PORTVALD.cbl
- [ ] Full audit logging — AUDPROC integration
- [ ] Batch job readers/processors/writers
- [ ] CICS COMMAREA replacement (REST API contracts)
- [ ] Integration tests with full data flows
- [ ] Performance tuning (array fetching, commit thresholds)
- [ ] Security hardening (role-based access control)
