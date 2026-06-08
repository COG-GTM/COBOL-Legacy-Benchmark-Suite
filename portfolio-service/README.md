# Portfolio Service — COBOL-to-Java Migration

Domain-driven Java migration of the **Investment Portfolio Management System**
originally implemented in Enterprise COBOL for z/OS.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4.x |
| Batch | Spring Batch 5.x |
| Persistence | Spring Data JPA / Hibernate 6 |
| DB Migrations | Flyway |
| API Docs | springdoc-openapi (Swagger UI) |
| Security | Spring Security |
| Dev DB | H2 (in-memory) |
| Build | Maven |

## Bounded Contexts

The COBOL codebase is decomposed into the following bounded contexts,
each mapping to a Java package:

### 1. Domain Model (`com.portfolio.domain.model`)

Core entities, value objects, and enums translated from COBOL copybooks.

| COBOL Copybook | Java Class | Role |
|----------------|-----------|------|
| `PORTFLIO.cpy` | `Portfolio` | Aggregate root |
| `POSREC.cpy` | `Position` | Entity |
| `TRNREC.cpy` | `TransactionCommand` | Command record |
| `TRNREC.cpy` | `TransactionType`, `TransactionStatus` | Enums |
| `HISTREC.cpy` | `HistoryRecord` | Entity |
| `AUDITLOG.cpy` | `AuditRecord` | Infra entity |
| `ERRHAND.cpy` | `ErrorCategory`, `ErrorSeverity` | Enums |
| `RTNCODE.cpy` | `ReturnCode` | Enum |
| `PORTVAL.cpy` | Validation constants in `TransactionValidator` | — |
| `DBTBLS.cpy` | `PositionHistory` (JPA entity) | Infra entity |

### 2. Transaction Processing (`com.portfolio.domain.service`)

Business logic ported from `PORTTRAN.cbl` and `PORTVALD.cbl`.

- **TransactionValidator** — guard-clause validation (portfolio checks,
  transaction type checks, amount range checks)
- **TransactionProcessor** — dispatches buy/sell/fee/transfer operations
- **Portfolio.applyBuy/applySell/applyFee** — aggregate root methods

### 3. Portfolio CRUD (`com.portfolio.application`)

Ported from `PORTADD.cbl`, `PORTREAD.cbl`, `PORTUPDT.cbl`, `PORTDEL.cbl`,
`PORTMSTR.cbl`.

- **PortfolioManagementService** — create, read, update, delete

### 4. Audit & Error Infrastructure (`com.portfolio.infrastructure.audit`, `.error`)

Ported from `AUDPROC.cbl`, `ERRPROC.cbl`.

- **AuditService** — `@TransactionalEventListener` for `TransactionProcessedEvent`
- **ErrorProcessor** — batch `SkipListener` + `@ControllerAdvice`
- Audit action mapping: `BU→CREATE`, `SL→DELETE`, `TR→UPDATE`, `FE→UPDATE`
- Error categories: `VS` (VSAM), `VL` (validation), `PR` (processing), `SY` (system)

### 5. Batch Processing (`com.portfolio.batch`)

Ported from `BCHCTL00.cbl`, `PRCSEQ00.cbl`, `CKPRST.cbl`, `HISTLD00.cbl`,
and the main loop in `PORTTRAN.cbl`.

- **TransactionBatchJob** — Spring Batch `Job` with reader/processor/writer
- **BatchConfiguration** — skip policies (mirrors `WS-ERROR-COUNT > 100`)
- **CheckpointRestartPolicy** — ports CKPRST logic

### 6. DB2 Integration (`com.portfolio.infrastructure.persistence`)

Ported from `DB2CONN.cbl`, `DB2CMT.cbl`, `DB2ERR.cbl`, `DB2STAT.cbl`,
`DBTBLS.cpy`, and the DDL under `src/database/db2/`.

- **PositionHistoryRepository** — JPA for `POSHIST` table
- Flyway migrations from DB2 DDL
- History load and report batch jobs

### 7. Online Inquiry / REST API (`com.portfolio.api`)

Ported from `INQONLN.cbl`, `INQPORT.cbl`, `INQHIST.cbl`, `CURSMGR.cbl`,
`SECMGR.cbl`, and BMS maps.

- **PortfolioController** — CRUD endpoints (replaces CICS screens)
- **InquiryController** — read-only query endpoints
- **InquiryService** — pagination (replaces CURSMGR cursor logic)
- **SecurityService** — auth from SECMGR

## Migration Roadmap

```
Session 1: Domain Model (entities, enums, repository interfaces)
    ├── Session 2: Validation & Transaction Processing
    ├── Session 3: Portfolio CRUD
    └── Session 4: Audit & Error Infrastructure
        ├── Session 5: Batch Processing Layer (Spring Batch)
        ├── Session 6: DB2 Integration & Position History
        └── Session 7: Online Inquiry (REST API)
```

Sessions 2, 3, 4 run in parallel after Session 1.
Sessions 5, 6, 7 depend on earlier sessions as shown.

## Quick Start

```bash
cd portfolio-service

# Requires Java 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Build
mvn clean compile

# Run tests
mvn test

# Start the service (H2 in-memory, Swagger at /swagger-ui.html)
mvn spring-boot:run
```

## Package Structure

```
com.portfolio
├── PortfolioServiceApplication.java       # @SpringBootApplication
├── api/                                   # REST controllers (Session 7)
│   ├── PortfolioController.java
│   └── InquiryController.java
├── application/                           # Application services
│   ├── PortfolioManagementService.java    # (Session 3)
│   └── InquiryService.java               # (Session 7)
├── batch/                                 # Spring Batch (Session 5)
│   ├── BatchConfiguration.java
│   └── TransactionBatchJob.java
├── domain/
│   ├── command/
│   │   └── TransactionCommand.java
│   ├── event/
│   │   ├── PortfolioCreatedEvent.java
│   │   └── TransactionProcessedEvent.java
│   ├── exception/
│   │   ├── InsufficientUnitsException.java
│   │   └── ValidationException.java
│   ├── model/                             # Entities & enums (Session 1)
│   │   ├── Portfolio.java                 # Aggregate root
│   │   ├── Position.java
│   │   ├── HistoryRecord.java
│   │   ├── ClientType.java
│   │   ├── PortfolioStatus.java
│   │   ├── TransactionType.java
│   │   ├── TransactionStatus.java
│   │   ├── PositionStatus.java
│   │   ├── ErrorCategory.java
│   │   ├── ErrorSeverity.java
│   │   ├── ReturnCode.java
│   │   ├── HistoryRecordType.java
│   │   ├── HistoryActionCode.java
│   │   ├── AuditAction.java
│   │   └── AuditType.java
│   ├── repository/
│   │   ├── PortfolioRepository.java
│   │   ├── PositionRepository.java
│   │   └── HistoryRecordRepository.java
│   └── service/                           # Domain services (Session 2)
│       ├── TransactionValidator.java
│       └── TransactionProcessor.java
└── infrastructure/
    ├── audit/                             # (Session 4)
    │   ├── AuditRecord.java
    │   ├── AuditRepository.java
    │   └── AuditService.java
    ├── error/
    │   └── ErrorProcessor.java
    ├── persistence/                       # (Session 6)
    │   └── PositionHistoryRepository.java
    └── security/                          # (Session 7)
        ├── SecurityConfiguration.java
        └── SecurityService.java
```

## COBOL Source Reference

The original COBOL source lives alongside this Java project:

- **Copybooks**: `src/copybook/{common,batch,online,db2}/`
- **Programs**: `src/programs/{portfolio,batch,common,online,test,utility}/`
- **JCL**: `src/jcl/`
- **BMS Maps**: `src/maps/`
- **DB2 DDL**: `src/database/db2/`
