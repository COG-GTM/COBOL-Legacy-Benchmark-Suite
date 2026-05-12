# Portfolio Master Entity — COBOL-to-Java Spring Boot Modernization Roadmap

Version: 1.0
Date: 2026-05-12

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Source System Analysis](#2-source-system-analysis)
3. [Target Architecture](#3-target-architecture)
4. [Copybook-to-JPA Entity Mapping](#4-copybook-to-jpa-entity-mapping)
5. [Service & API Layer Design](#5-service--api-layer-design)
6. [Data Migration Strategy](#6-data-migration-strategy)
7. [Test Plan](#7-test-plan)
8. [Migration Phases & Timeline](#8-migration-phases--timeline)
9. [Risk Register](#9-risk-register)
10. [Appendices](#10-appendices)

---

## 1. Executive Summary

This roadmap defines the migration of the **Portfolio Master** entity and its dependent data structures from the COBOL Legacy Benchmark Suite (CLBS) to a Java 21 / Spring Boot application backed by a relational database (PostgreSQL). The Portfolio Master is the central entity in the Investment Portfolio Management System — it is referenced by positions, transactions, history records, and audit logs, making it the natural first candidate for modernization.

### Scope

| In Scope | Out of Scope (future phases) |
|---|---|
| Portfolio Master VSAM KSDS → JPA entity | Batch reporting programs (RPTPOS00, RPTAUD00, RPTSTA00) |
| Position Record → JPA entity | CICS online inquiry screens (INQONLN, INQPORT) |
| Transaction Record → JPA entity | DB2 history loader (HISTLD00) |
| History Record → JPA entity | Batch control framework (BCHCTL, CKPRST) |
| Audit Log → JPA entity | Utility programs (UTLMNT00, UTLMON00, UTLVAL00) |
| CRUD REST API (PORTMSTR equivalent) | |
| Validation service (PORTVALD equivalent) | |
| Transaction processing (PORTTRAN equivalent) | |
| Data migration pipeline | |

### Source Artifacts Analyzed

| Artifact | Path | Purpose |
|---|---|---|
| `PORTFLIO.cpy` | `src/copybook/common/` | Portfolio Master record layout |
| `POSREC.cpy` | `src/copybook/common/` | Position record layout |
| `TRNREC.cpy` | `src/copybook/common/` | Transaction record layout |
| `HISTREC.cpy` | `src/copybook/common/` | History record layout |
| `AUDITLOG.cpy` | `src/copybook/common/` | Audit trail record layout |
| `PORTVAL.cpy` | `src/copybook/common/` | Validation rules and constants |
| `COMMON.cpy` | `src/copybook/common/` | Shared constants and codes |
| `ERRHAND.cpy` | `src/copybook/common/` | Error handling definitions |
| `RETHND.cpy` | `src/copybook/common/` | Return code handling |
| `PORTMSTR.cbl` | `src/programs/portfolio/` | Portfolio Master CRUD program |
| `PORTVALD.cbl` | `src/programs/portfolio/` | Validation subroutine |
| `PORTADD.cbl` | `src/programs/portfolio/` | Batch add program |
| `PORTREAD.cbl` | `src/programs/portfolio/` | Batch read/list program |
| `PORTUPDT.cbl` | `src/programs/portfolio/` | Batch update program |
| `PORTDEL.cbl` | `src/programs/portfolio/` | Batch delete with audit program |
| `PORTTRAN.cbl` | `src/programs/portfolio/` | Transaction processing program |
| `PORTTEST.cbl` | `src/programs/portfolio/` | Test data generator |
| `db2-definitions.sql` | `src/database/db2/` | DB2 table DDL |
| `POSHIST.sql` | `src/database/db2/` | Position history table DDL |
| `vsam-definitions.txt` | `src/database/vsam/` | VSAM cluster definitions |

---

## 2. Source System Analysis

### 2.1 Portfolio Master Record Layout (PORTFLIO.cpy)

The VSAM KSDS record is 400 bytes with a 12-byte composite key:

```
Offset  Length  Field                Type          Description
------  ------  -------------------  -----------   --------------------------
0       8       PORT-ID              PIC X(8)      Portfolio identifier
8       10      PORT-ACCOUNT-NO      PIC X(10)     Account number
18      30      PORT-CLIENT-NAME     PIC X(30)     Client name
48      1       PORT-CLIENT-TYPE     PIC X(1)      I=Individual, C=Corporate, T=Trust
49      8       PORT-CREATE-DATE     PIC 9(8)      Creation date (YYYYMMDD)
57      8       PORT-LAST-MAINT      PIC 9(8)      Last maintenance date (YYYYMMDD)
65      1       PORT-STATUS          PIC X(1)      A=Active, C=Closed, S=Suspended
66      8       PORT-TOTAL-VALUE     COMP-3        S9(13)V99  Total portfolio value
74      8       PORT-CASH-BALANCE    COMP-3        S9(13)V99  Cash balance
82      8       PORT-LAST-USER       PIC X(8)      Last update user ID
90      8       PORT-LAST-TRANS      PIC 9(8)      Last transaction date
98      50      PORT-FILLER          PIC X(50)     Reserved
```

### 2.2 COBOL Program-to-Business Capability Map

| COBOL Program | Business Capability | Key Operations |
|---|---|---|
| **PORTMSTR** | Portfolio CRUD | Create, Read, Update, Delete via VSAM I-O; validates via inline `2100-VALIDATE-PORTFOLIO` |
| **PORTVALD** | Validation Subroutine | Validates Portfolio ID (PORT prefix + 4 digits), Account (10 numeric digits), Investment Type (STK/BND/MMF/ETF), Amount range |
| **PORTADD** | Batch Portfolio Creation | Reads sequential input, validates, writes to VSAM KSDS; counts adds/dups/errors |
| **PORTREAD** | Batch Portfolio Listing | Sequential scan of all portfolio records with display |
| **PORTUPDT** | Batch Portfolio Update | Processes update file with action codes (S=Status, V=Value, N=Name) |
| **PORTDEL** | Batch Portfolio Deletion | Processes deletion requests with reason codes (01=Closed, 02=Transferred, 03=Requested); writes audit trail |
| **PORTTRAN** | Transaction Processing | Validates and processes BUY/SELL/TRANSFER/FEE transactions; updates portfolio totals; writes audit trail via AUDPROC |
| **PORTTEST** | Test Data Generation | Generates 100 synthetic portfolio records with random client types, statuses, and financial values |

### 2.3 Data Relationships

```
PORTFOLIO_MASTER (VSAM KSDS)
    │
    ├──< INVESTMENT_POSITIONS (VSAM KSDS)
    │       FK: POS-PORTFOLIO-ID → PORT-ID
    │       Key: PORTFOLIO-ID + DATE + INVESTMENT-ID
    │
    ├──< TRANSACTION_HISTORY (VSAM KSDS)
    │       FK: TRN-PORTFOLIO-ID → PORT-ID
    │       Key: DATE + TIME + PORTFOLIO-ID + SEQ-NO
    │
    ├──< HISTORY_RECORD (VSAM ESDS)
    │       FK: HIST-PORTFOLIO-ID → PORT-ID
    │       Key: PORTFOLIO-ID + DATE + TIME + SEQ-NO
    │
    ├──< AUDIT_LOG
    │       FK: AUD-PORTFOLIO-ID → PORT-ID
    │
    └──< POSHIST (DB2 table)
            FK: PORTFOLIO_ID → PORT-ID
            Partitioned by TRANS_DATE (quarterly)
```

### 2.4 Validation Rules (from PORTVALD.cbl and PORTMSTR.cbl)

| Rule ID | Field | Rule | Source |
|---|---|---|---|
| V-001 | Portfolio ID | Must start with `PORT` followed by 4 numeric digits | PORTVALD `1000-VALIDATE-ID` |
| V-002 | Account Number | Must be 10 numeric digits, non-zero | PORTVALD `2000-VALIDATE-ACCOUNT` |
| V-003 | Investment Type | Must be `STK`, `BND`, `MMF`, or `ETF` | PORTVALD `3000-VALIDATE-TYPE` |
| V-004 | Amount | Must be within ±9,999,999,999,999.99 | PORTVALD `4000-VALIDATE-AMOUNT` |
| V-005 | Portfolio Name | Must not be spaces | PORTMSTR `2100-VALIDATE-PORTFOLIO` |
| V-006 | Status | Must be `A` (Active), `I` (Inactive), or `C` (Closed) | PORTMSTR `2100-VALIDATE-PORTFOLIO` |
| V-007 | Client Type | Must be `I` (Individual), `C` (Corporate), or `T` (Trust) | PORTFLIO.cpy Level-88 |
| V-008 | Transaction Quantity | Must be > 0 | PORTTRAN `2130-CHECK-AMOUNTS` |
| V-009 | Transaction Price | Must be > 0 (except for transfers) | PORTTRAN `2130-CHECK-AMOUNTS` |
| V-010 | Sell Quantity | Must not exceed current portfolio units | PORTTRAN `2220-PROCESS-SELL` |

---

## 3. Target Architecture

### 3.1 Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| Language | Java 21 | LTS release; virtual threads for concurrency; pattern matching |
| Framework | Spring Boot 3.3+ | Industry standard; auto-configuration; production-ready |
| Persistence | Spring Data JPA / Hibernate | Type-safe ORM; automatic DDL; query methods |
| Database | PostgreSQL 16 | ACID-compliant; native `DECIMAL` for financial precision; partitioning support |
| Migration | Flyway | Version-controlled SQL migrations; rollback support |
| API | Spring Web (REST) + OpenAPI 3.x | RESTful; auto-generated Swagger docs |
| Validation | Jakarta Bean Validation (Hibernate Validator) | Annotation-driven; custom validators |
| Build | Gradle (Kotlin DSL) | Consistent with org standards; flexible dependency management |
| Testing | JUnit 5, Mockito, Testcontainers, REST Assured | Comprehensive test coverage at all layers |
| Audit | Spring Data Envers / custom JPA auditing | Automatic change tracking; mirrors COBOL audit trail |

### 3.2 Package Structure

```
com.portfolio.management/
├── config/                       # Spring configuration classes
│   ├── JpaAuditingConfig.java
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
├── entity/                       # JPA entities (from copybooks)
│   ├── Portfolio.java            # ← PORTFLIO.cpy
│   ├── InvestmentPosition.java   # ← POSREC.cpy
│   ├── Transaction.java          # ← TRNREC.cpy
│   ├── HistoryRecord.java        # ← HISTREC.cpy
│   ├── AuditLog.java             # ← AUDITLOG.cpy
│   └── enums/
│       ├── ClientType.java       # I, C, T
│       ├── PortfolioStatus.java   # A, C, S
│       ├── TransactionType.java   # BU, SL, TR, FE
│       ├── TransactionStatus.java # P, D, F, R
│       ├── PositionStatus.java    # A, C, P
│       ├── AuditAction.java       # CREATE, UPDATE, DELETE, INQUIRE
│       └── HistoryActionCode.java # A, C, D
├── repository/                   # Spring Data JPA repositories
│   ├── PortfolioRepository.java
│   ├── InvestmentPositionRepository.java
│   ├── TransactionRepository.java
│   ├── HistoryRecordRepository.java
│   └── AuditLogRepository.java
├── service/                      # Business logic (from COBOL programs)
│   ├── PortfolioService.java     # ← PORTMSTR paragraphs
│   ├── PortfolioValidationService.java  # ← PORTVALD
│   ├── TransactionService.java   # ← PORTTRAN
│   └── AuditService.java         # ← AUDPROC calls
├── controller/                   # REST API controllers
│   ├── PortfolioController.java
│   └── TransactionController.java
├── dto/                          # Request/Response DTOs
│   ├── PortfolioRequest.java
│   ├── PortfolioResponse.java
│   ├── TransactionRequest.java
│   └── ErrorResponse.java
├── exception/                    # Exception handling
│   ├── PortfolioNotFoundException.java
│   ├── DuplicatePortfolioException.java
│   ├── InsufficientUnitsException.java
│   ├── ValidationException.java
│   └── GlobalExceptionHandler.java
└── migration/                    # Data migration utilities
    ├── CobolRecordParser.java
    └── DataMigrationService.java
```

---

## 4. Copybook-to-JPA Entity Mapping

### 4.1 Portfolio Entity (PORTFLIO.cpy → Portfolio.java)

| COBOL Field | PIC Clause | JPA Field | Java Type | Column Definition | Notes |
|---|---|---|---|---|---|
| `PORT-ID` | `X(8)` | `portfolioId` | `String` | `@Id @Column(length=8)` | Primary key; pattern `PORT\d{4}` |
| `PORT-ACCOUNT-NO` | `X(10)` | `accountNumber` | `String` | `@Column(length=10, nullable=false)` | Numeric string |
| `PORT-CLIENT-NAME` | `X(30)` | `clientName` | `String` | `@Column(length=50, nullable=false)` | Expanded from 30 to 50 (matches DB2 DDL) |
| `PORT-CLIENT-TYPE` | `X(1)` | `clientType` | `ClientType` | `@Enumerated(STRING) @Column(length=1)` | Enum: INDIVIDUAL, CORPORATE, TRUST |
| `PORT-CREATE-DATE` | `9(8)` | `createDate` | `LocalDate` | `@Column(nullable=false)` | Converted from YYYYMMDD numeric |
| `PORT-LAST-MAINT` | `9(8)` | `lastMaintenanceDate` | `LocalDateTime` | `@Column(nullable=false)` | Upgraded to timestamp for precision |
| `PORT-STATUS` | `X(1)` | `status` | `PortfolioStatus` | `@Enumerated(STRING) @Column(length=1)` | Enum: ACTIVE, CLOSED, SUSPENDED |
| `PORT-TOTAL-VALUE` | `S9(13)V99 COMP-3` | `totalValue` | `BigDecimal` | `@Column(precision=15, scale=2)` | **Must be BigDecimal, never double** |
| `PORT-CASH-BALANCE` | `S9(13)V99 COMP-3` | `cashBalance` | `BigDecimal` | `@Column(precision=15, scale=2)` | **Must be BigDecimal, never double** |
| `PORT-LAST-USER` | `X(8)` | `lastUser` | `String` | `@Column(length=8)` | JPA auditing via `@LastModifiedBy` |
| `PORT-LAST-TRANS` | `9(8)` | `lastTransactionDate` | `LocalDate` | | Converted from YYYYMMDD numeric |
| `PORT-FILLER` | `X(50)` | *(dropped)* | — | — | FILLER not needed in modern schema |

#### Additional DB2 DDL Fields (from db2-definitions.sql)

The DB2 `PORTFOLIO_MASTER` table includes fields not present in the VSAM copybook. These should be included in the JPA entity:

| DB2 Column | JPA Field | Java Type | Notes |
|---|---|---|---|
| `ACCOUNT_TYPE` | `accountType` | `String` | `@Column(length=2)` |
| `BRANCH_ID` | `branchId` | `String` | `@Column(length=2)` |
| `CLIENT_ID` | `clientId` | `String` | `@Column(length=10)` |
| `CURRENCY_CODE` | `currencyCode` | `String` | `@Column(length=3)` — default `USD` |
| `RISK_LEVEL` | `riskLevel` | `String` | `@Column(length=1)` |
| `CLOSE_DATE` | `closeDate` | `LocalDate` | Nullable |

#### Proposed Entity Class

```java
@Entity
@Table(name = "portfolio_master", indexes = {
    @Index(name = "idx_portfolio_client", columnList = "clientId, status"),
    @Index(name = "idx_portfolio_account", columnList = "accountNumber")
})
@EntityListeners(AuditingEntityListener.class)
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "account_number", length = 10, nullable = false)
    private String accountNumber;

    @Column(name = "account_type", length = 2)
    private String accountType;

    @Column(name = "branch_id", length = 2)
    private String branchId;

    @Column(name = "client_id", length = 10, nullable = false)
    private String clientId;

    @Column(name = "client_name", length = 50, nullable = false)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 1, nullable = false)
    private ClientType clientType;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "risk_level", length = 1)
    private String riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 1, nullable = false)
    private PortfolioStatus status;

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "total_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    @LastModifiedDate
    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintenanceDate;

    @LastModifiedBy
    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastUser;

    // One-to-many relationships
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvestmentPosition> positions = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();
}
```

### 4.2 Investment Position Entity (POSREC.cpy → InvestmentPosition.java)

| COBOL Field | PIC Clause | JPA Field | Java Type | Column Definition |
|---|---|---|---|---|
| `POS-PORTFOLIO-ID` | `X(8)` | `portfolio` | `Portfolio` | `@ManyToOne` FK |
| `POS-DATE` | `X(8)` | `positionDate` | `LocalDate` | Part of composite key |
| `POS-INVESTMENT-ID` | `X(10)` | `investmentId` | `String` | `@Column(length=10)`, part of composite key |
| `POS-QUANTITY` | `S9(11)V9(4) COMP-3` | `quantity` | `BigDecimal` | `precision=18, scale=4` |
| `POS-COST-BASIS` | `S9(13)V9(2) COMP-3` | `costBasis` | `BigDecimal` | `precision=18, scale=2` |
| `POS-MARKET-VALUE` | `S9(13)V9(2) COMP-3` | `marketValue` | `BigDecimal` | `precision=18, scale=2` |
| `POS-CURRENCY` | `X(3)` | `currencyCode` | `String` | `@Column(length=3)` |
| `POS-STATUS` | `X(1)` | `status` | `PositionStatus` | Enum: ACTIVE, CLOSED, PENDING |
| `POS-LAST-MAINT-DATE` | `X(26)` | `lastMaintenanceDate` | `LocalDateTime` | Timestamp |
| `POS-LAST-MAINT-USER` | `X(8)` | `lastMaintenanceUser` | `String` | `@Column(length=8)` |
| `POS-FILLER` | `X(50)` | *(dropped)* | — | — |

### 4.3 Transaction Entity (TRNREC.cpy → Transaction.java)

| COBOL Field | PIC Clause | JPA Field | Java Type | Column Definition |
|---|---|---|---|---|
| `TRN-DATE` | `X(8)` | `transactionDate` | `LocalDate` | |
| `TRN-TIME` | `X(6)` | `transactionTime` | `LocalTime` | |
| `TRN-PORTFOLIO-ID` | `X(8)` | `portfolio` | `Portfolio` | `@ManyToOne` FK |
| `TRN-SEQUENCE-NO` | `X(6)` | `sequenceNumber` | `String` | `@Column(length=6)` |
| `TRN-INVESTMENT-ID` | `X(10)` | `investmentId` | `String` | `@Column(length=10)` |
| `TRN-TYPE` | `X(2)` | `transactionType` | `TransactionType` | Enum: BUY, SELL, TRANSFER, FEE |
| `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | `quantity` | `BigDecimal` | `precision=18, scale=4` |
| `TRN-PRICE` | `S9(11)V9(4) COMP-3` | `price` | `BigDecimal` | `precision=18, scale=4` |
| `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | `amount` | `BigDecimal` | `precision=18, scale=2` |
| `TRN-CURRENCY` | `X(3)` | `currencyCode` | `String` | `@Column(length=3)` |
| `TRN-STATUS` | `X(1)` | `status` | `TransactionStatus` | Enum: PENDING, DONE, FAILED, REVERSED |
| `TRN-PROCESS-DATE` | `X(26)` | `processTimestamp` | `LocalDateTime` | |
| `TRN-PROCESS-USER` | `X(8)` | `processUser` | `String` | `@Column(length=8)` |
| `TRN-FILLER` | `X(50)` | *(dropped)* | — | — |

### 4.4 History Record Entity (HISTREC.cpy → HistoryRecord.java)

| COBOL Field | PIC Clause | JPA Field | Java Type | Column Definition |
|---|---|---|---|---|
| `HIST-PORTFOLIO-ID` | `X(8)` | `portfolio` | `Portfolio` | `@ManyToOne` FK |
| `HIST-DATE` | `X(8)` | `historyDate` | `LocalDate` | |
| `HIST-TIME` | `X(6)` | `historyTime` | `LocalTime` | |
| `HIST-SEQ-NO` | `X(4)` | `sequenceNumber` | `String` | `@Column(length=4)` |
| `HIST-RECORD-TYPE` | `X(2)` | `recordType` | `String` | PT=Portfolio, PS=Position, TR=Transaction |
| `HIST-ACTION-CODE` | `X(1)` | `actionCode` | `HistoryActionCode` | Enum: ADD, CHANGE, DELETE |
| `HIST-BEFORE-IMAGE` | `X(400)` | `beforeImage` | `String` | `@Column(length=2000)` — JSON in modern system |
| `HIST-AFTER-IMAGE` | `X(400)` | `afterImage` | `String` | `@Column(length=2000)` — JSON in modern system |
| `HIST-REASON-CODE` | `X(4)` | `reasonCode` | `String` | `@Column(length=4)` |
| `HIST-PROCESS-DATE` | `X(26)` | `processTimestamp` | `LocalDateTime` | |
| `HIST-PROCESS-USER` | `X(8)` | `processUser` | `String` | `@Column(length=8)` |
| `HIST-FILLER` | `X(50)` | *(dropped)* | — | — |

**Modernization Enhancement**: The COBOL system stores before/after images as raw fixed-length byte strings. In the Spring Boot system, these will be stored as **JSON** representations of the entity state, enabling structured querying and better readability.

### 4.5 Audit Log Entity (AUDITLOG.cpy → AuditLog.java)

| COBOL Field | PIC Clause | JPA Field | Java Type | Column Definition |
|---|---|---|---|---|
| `AUD-TIMESTAMP` | `X(26)` | `timestamp` | `LocalDateTime` | Part of composite key |
| `AUD-SYSTEM-ID` | `X(8)` | `systemId` | `String` | `@Column(length=8)` |
| `AUD-USER-ID` | `X(8)` | `userId` | `String` | `@Column(length=8)` |
| `AUD-PROGRAM` | `X(8)` | `programName` | `String` | `@Column(length=8)` — maps to service class name |
| `AUD-TERMINAL` | `X(8)` | `terminal` | `String` | `@Column(length=16)` — maps to client IP/session |
| `AUD-TYPE` | `X(4)` | `auditType` | `String` | TRAN, USER, SYST |
| `AUD-ACTION` | `X(8)` | `action` | `AuditAction` | Enum: CREATE, UPDATE, DELETE, INQUIRE, etc. |
| `AUD-STATUS` | `X(4)` | `status` | `String` | SUCC, FAIL, WARN |
| `AUD-PORTFOLIO-ID` | `X(8)` | `portfolioId` | `String` | `@Column(length=8)` |
| `AUD-ACCOUNT-NO` | `X(10)` | `accountNumber` | `String` | `@Column(length=10)` |
| `AUD-BEFORE-IMAGE` | `X(100)` | `beforeImage` | `String` | `@Column(length=2000)` — JSON |
| `AUD-AFTER-IMAGE` | `X(100)` | `afterImage` | `String` | `@Column(length=2000)` — JSON |
| `AUD-MESSAGE` | `X(100)` | `message` | `String` | `@Column(length=500)` |

### 4.6 Enum Definitions

```java
public enum ClientType {
    INDIVIDUAL('I'), CORPORATE('C'), TRUST('T');
    // constructor, getValue(), fromCode(char)
}

public enum PortfolioStatus {
    ACTIVE('A'), CLOSED('C'), SUSPENDED('S');
}

public enum TransactionType {
    BUY("BU"), SELL("SL"), TRANSFER("TR"), FEE("FE");
}

public enum TransactionStatus {
    PENDING('P'), DONE('D'), FAILED('F'), REVERSED('R');
}

public enum PositionStatus {
    ACTIVE('A'), CLOSED('C'), PENDING('P');
}

public enum AuditAction {
    CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN;
}

public enum HistoryActionCode {
    ADD('A'), CHANGE('C'), DELETE('D');
}
```

### 4.7 Database Schema (Flyway Migration V1)

```sql
-- V1__create_portfolio_master_schema.sql

CREATE TABLE portfolio_master (
    portfolio_id        VARCHAR(8)      NOT NULL,
    account_number      VARCHAR(10)     NOT NULL,
    account_type        VARCHAR(2),
    branch_id           VARCHAR(2),
    client_id           VARCHAR(10)     NOT NULL,
    client_name         VARCHAR(50)     NOT NULL,
    client_type         CHAR(1)         NOT NULL,
    currency_code       VARCHAR(3)      NOT NULL DEFAULT 'USD',
    risk_level          CHAR(1),
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    create_date         DATE            NOT NULL,
    close_date          DATE,
    total_value         DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    cash_balance        DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    last_transaction_date DATE,
    last_maint_date     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user     VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id),
    CONSTRAINT chk_client_type CHECK (client_type IN ('I', 'C', 'T')),
    CONSTRAINT chk_status CHECK (status IN ('A', 'C', 'S'))
);

CREATE INDEX idx_portfolio_client ON portfolio_master (client_id, status);
CREATE INDEX idx_portfolio_account ON portfolio_master (account_number);

CREATE TABLE investment_positions (
    id                  BIGSERIAL       PRIMARY KEY,
    portfolio_id        VARCHAR(8)      NOT NULL,
    position_date       DATE            NOT NULL,
    investment_id       VARCHAR(10)     NOT NULL,
    quantity            DECIMAL(18,4)   NOT NULL,
    cost_basis          DECIMAL(18,2)   NOT NULL,
    market_value        DECIMAL(18,2)   NOT NULL,
    currency_code       VARCHAR(3)      NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    last_maint_date     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user     VARCHAR(8)      NOT NULL,
    CONSTRAINT fk_position_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id),
    CONSTRAINT uk_position UNIQUE (portfolio_id, investment_id, position_date),
    CONSTRAINT chk_pos_status CHECK (status IN ('A', 'C', 'P'))
);

CREATE INDEX idx_positions_date ON investment_positions (position_date, portfolio_id);

CREATE TABLE transactions (
    id                  BIGSERIAL       PRIMARY KEY,
    transaction_date    DATE            NOT NULL,
    transaction_time    TIME            NOT NULL,
    portfolio_id        VARCHAR(8)      NOT NULL,
    sequence_number     VARCHAR(6)      NOT NULL,
    investment_id       VARCHAR(10)     NOT NULL,
    transaction_type    VARCHAR(2)      NOT NULL,
    quantity            DECIMAL(18,4)   NOT NULL,
    price               DECIMAL(18,4)   NOT NULL,
    amount              DECIMAL(18,2)   NOT NULL,
    currency_code       VARCHAR(3)      NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'P',
    process_timestamp   TIMESTAMP,
    process_user        VARCHAR(8),
    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id),
    CONSTRAINT chk_txn_type CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT chk_txn_status CHECK (status IN ('P', 'D', 'F', 'R'))
);

CREATE INDEX idx_txn_portfolio_date ON transactions (portfolio_id, transaction_date);

CREATE TABLE history_records (
    id                  BIGSERIAL       PRIMARY KEY,
    portfolio_id        VARCHAR(8)      NOT NULL,
    history_date        DATE            NOT NULL,
    history_time        TIME            NOT NULL,
    sequence_number     VARCHAR(4)      NOT NULL,
    record_type         VARCHAR(2)      NOT NULL,
    action_code         CHAR(1)         NOT NULL,
    before_image        TEXT,
    after_image         TEXT,
    reason_code         VARCHAR(4),
    process_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_user        VARCHAR(8)      NOT NULL,
    CONSTRAINT fk_history_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id),
    CONSTRAINT chk_hist_action CHECK (action_code IN ('A', 'C', 'D'))
);

CREATE TABLE audit_log (
    id                  BIGSERIAL       PRIMARY KEY,
    timestamp           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    system_id           VARCHAR(8),
    user_id             VARCHAR(8)      NOT NULL,
    program_name        VARCHAR(8),
    terminal            VARCHAR(16),
    audit_type          VARCHAR(4)      NOT NULL,
    action              VARCHAR(8)      NOT NULL,
    status              VARCHAR(4)      NOT NULL,
    portfolio_id        VARCHAR(8),
    account_number      VARCHAR(10),
    before_image        TEXT,
    after_image         TEXT,
    message             VARCHAR(500)
);

CREATE INDEX idx_audit_portfolio ON audit_log (portfolio_id, timestamp);
CREATE INDEX idx_audit_timestamp ON audit_log (timestamp DESC);

-- Active portfolios view (mirrors DB2 ACTIVE_PORTFOLIOS view)
CREATE VIEW active_portfolios AS
    SELECT * FROM portfolio_master
    WHERE status = 'A'
    AND (close_date IS NULL OR close_date > CURRENT_DATE);
```

---

## 5. Service & API Layer Design

### 5.1 COBOL Paragraph-to-Java Method Traceability

#### PortfolioService (← PORTMSTR.cbl)

| COBOL Paragraph | Java Method | HTTP Endpoint | Description |
|---|---|---|---|
| `2000-CREATE-PORTFOLIO` | `createPortfolio(PortfolioRequest)` | `POST /api/v1/portfolios` | Create new portfolio |
| `3000-READ-PORTFOLIO` | `getPortfolio(String id)` | `GET /api/v1/portfolios/{id}` | Read by portfolio ID |
| — *(PORTREAD sequential scan)* | `listPortfolios(Pageable)` | `GET /api/v1/portfolios` | Paginated list |
| `4000-UPDATE-PORTFOLIO` | `updatePortfolio(String id, PortfolioRequest)` | `PUT /api/v1/portfolios/{id}` | Update portfolio |
| `5000-DELETE-PORTFOLIO` | `deletePortfolio(String id, String reason)` | `DELETE /api/v1/portfolios/{id}` | Soft-delete portfolio |

#### PortfolioValidationService (← PORTVALD.cbl)

| COBOL Paragraph | Java Method | Description |
|---|---|---|
| `1000-VALIDATE-ID` | `validatePortfolioId(String id)` | Validate PORT prefix + 4 digits |
| `2000-VALIDATE-ACCOUNT` | `validateAccountNumber(String acct)` | Validate 10 numeric digits |
| `3000-VALIDATE-TYPE` | `validateInvestmentType(String type)` | Validate STK/BND/MMF/ETF |
| `4000-VALIDATE-AMOUNT` | `validateAmount(BigDecimal amount)` | Validate within ±9999999999999.99 |

#### TransactionService (← PORTTRAN.cbl)

| COBOL Paragraph | Java Method | HTTP Endpoint |
|---|---|---|
| `2100-VALIDATE-TRANSACTION` | `validateTransaction(TransactionRequest)` | — (internal) |
| `2210-PROCESS-BUY` | `processBuy(TransactionRequest)` | `POST /api/v1/portfolios/{id}/transactions` |
| `2220-PROCESS-SELL` | `processSell(TransactionRequest)` | `POST /api/v1/portfolios/{id}/transactions` |
| `2240-PROCESS-FEE` | `processFee(TransactionRequest)` | `POST /api/v1/portfolios/{id}/transactions` |
| `2300-UPDATE-AUDIT-TRAIL` | `createAuditEntry(...)` | — (internal) |

### 5.2 Error Code Mapping

| COBOL Error | COBOL Code | HTTP Status | Java Exception |
|---|---|---|---|
| VSAM duplicate key (`22`) | `PORT-DUP-KEY` | `409 Conflict` | `DuplicatePortfolioException` |
| VSAM not found (`23`) | `PORT-NOT-FOUND` | `404 Not Found` | `PortfolioNotFoundException` |
| Validation failure | `VAL-INVALID-*` | `400 Bad Request` | `ValidationException` |
| Insufficient units | (inline check) | `422 Unprocessable` | `InsufficientUnitsException` |
| File open error | `WS-FILE-STATUS` | `503 Service Unavailable` | `ServiceUnavailableException` |
| Invalid command | (EVALUATE OTHER) | `400 Bad Request` | `IllegalArgumentException` |

### 5.3 OpenAPI Specification Summary

```yaml
openapi: 3.1.0
info:
  title: Portfolio Management API
  version: "1.0.0"
  description: Modernized Portfolio Master — migrated from COBOL CLBS
paths:
  /api/v1/portfolios:
    get:
      summary: List portfolios (replaces PORTREAD batch scan)
      parameters:
        - name: status
          in: query
          schema: { type: string, enum: [A, C, S] }
        - name: clientType
          in: query
          schema: { type: string, enum: [I, C, T] }
        - name: page
          in: query
          schema: { type: integer, default: 0 }
        - name: size
          in: query
          schema: { type: integer, default: 20 }
    post:
      summary: Create portfolio (replaces PORTMSTR CREATE)
  /api/v1/portfolios/{portfolioId}:
    get:
      summary: Get portfolio by ID (replaces PORTMSTR READ)
    put:
      summary: Update portfolio (replaces PORTMSTR UPDATE + PORTUPDT)
    delete:
      summary: Delete portfolio (replaces PORTMSTR DELETE + PORTDEL)
  /api/v1/portfolios/{portfolioId}/transactions:
    post:
      summary: Process transaction (replaces PORTTRAN)
    get:
      summary: List transactions for portfolio
  /api/v1/portfolios/{portfolioId}/positions:
    get:
      summary: List positions for portfolio
```

---

## 6. Data Migration Strategy

### 6.1 Overview

The migration follows a **staged, parallel-run** approach to minimize risk. Both the COBOL system and the Spring Boot application run simultaneously during the transition, with reconciliation checks ensuring data parity.

### 6.2 Migration Phases

#### Phase 1 — Extract (from Mainframe)

**Source**: VSAM KSDS files + DB2 tables on z/OS

| Source | Extract Method | Output Format |
|---|---|---|
| Portfolio Master VSAM | JCL IDCAMS REPRO → flat file export, or COBOL unload program | Fixed-length records (400 bytes) → CSV |
| Position History (VSAM) | JCL IDCAMS REPRO → flat file export | Fixed-length records → CSV |
| Transaction History (VSAM) | JCL IDCAMS REPRO → flat file export | Fixed-length records → CSV |
| POSHIST (DB2) | DB2 UNLOAD utility or DSNTIAUL | Delimited export → CSV |
| ERRLOG (DB2) | DB2 UNLOAD utility | Delimited export → CSV |

**Extract JCL Example** (Portfolio Master):
```jcl
//EXTRACT  EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.MASTER.FILE,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.EXTRACT.FILE,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(50,10)),
//            DCB=(RECFM=FB,LRECL=400,BLKSIZE=0)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
```

#### Phase 2 — Transform

A Java-based transform utility (`CobolRecordParser.java`) parses fixed-length COBOL records into domain objects. Key transformations:

| Transformation | COBOL Source | Java Target | Logic |
|---|---|---|---|
| COMP-3 → BigDecimal | `S9(13)V99 COMP-3` (8 bytes packed) | `BigDecimal` | Unpack BCD nibbles; divide by 10^scale; check sign nibble |
| Numeric date → LocalDate | `PIC 9(8)` (YYYYMMDD) | `LocalDate` | `LocalDate.parse(str, DateTimeFormatter.BASIC_ISO_DATE)` |
| Fixed-width string → trimmed | `PIC X(n)` (space-padded) | `String` | `value.trim()` |
| Level-88 code → Enum | `'I'`, `'C'`, `'T'` | `ClientType` | Enum `fromCode(char)` factory method |
| FILLER → dropped | `PIC X(50)` | — | Skip bytes during parsing |

**COMP-3 Unpacking Algorithm**:
```java
public static BigDecimal unpackComp3(byte[] data, int scale) {
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < data.length; i++) {
        int highNibble = (data[i] >> 4) & 0x0F;
        int lowNibble = data[i] & 0x0F;
        if (i < data.length - 1) {
            digits.append(highNibble).append(lowNibble);
        } else {
            digits.append(highNibble);
            // Low nibble of last byte is sign: C/F = positive, D = negative
            boolean negative = (lowNibble == 0x0D);
            BigDecimal result = new BigDecimal(digits.toString())
                .movePointLeft(scale);
            return negative ? result.negate() : result;
        }
    }
    return BigDecimal.ZERO;
}
```

#### Phase 3 — Load

Use Spring Batch for high-volume loading with checkpoint/restart (mirroring the COBOL CKPRST pattern):

| Component | Spring Batch Equivalent | COBOL Analog |
|---|---|---|
| `ItemReader<PortfolioRecord>` | `FlatFileItemReader` | `OPEN INPUT` / `READ` loop |
| `ItemProcessor<PortfolioRecord, Portfolio>` | Custom transformer | `2100-VALIDATE-AND-ADD` |
| `ItemWriter<Portfolio>` | `JpaItemWriter` | `WRITE PORTFOLIO-RECORD` |
| Chunk size | `commit-interval=1000` | `CK-COMMIT-FREQ` (1000) |
| Error threshold | `skip-limit=100` | `CK-MAX-ERRORS` (100) |
| Restart support | `Job restartability` | CKPRST `CK-MODE-RESTART` |

**Load Order** (respects referential integrity):
1. `portfolio_master` (no FK dependencies)
2. `investment_positions` (FK → portfolio_master)
3. `transactions` (FK → portfolio_master)
4. `history_records` (FK → portfolio_master)
5. `audit_log` (no FK, but references portfolio_id)

#### Phase 4 — Validate / Reconcile

| Validation Check | Method | Acceptance Criteria |
|---|---|---|
| Record count | Count VSAM records vs PostgreSQL rows | 100% match |
| Key integrity | Compare all PORT-ID values | 100% match |
| Financial totals | Sum PORT-TOTAL-VALUE in both systems | Difference < $0.01 per record |
| COMP-3 precision | Spot-check 1000 random records | Exact decimal match |
| Date conversion | Verify YYYYMMDD → LocalDate for all records | 0 parse errors |
| Status code mapping | Verify all status values are valid enum values | 0 unmapped values |
| Referential integrity | Verify all position/transaction FK references exist | 0 orphan records |

#### Phase 5 — Parallel Run

During parallel run, both systems process transactions. A reconciliation job runs nightly:

```
┌─────────────┐     ┌─────────────────┐
│  Mainframe   │────▶│  Change Data    │
│  VSAM/DB2   │     │  Capture (CDC)  │
└─────────────┘     └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Reconciliation  │
                    │  Service         │
                    └────────┬────────┘
                             │
┌─────────────┐     ┌────────▼────────┐
│  Spring Boot │◀───│  Delta Sync     │
│  PostgreSQL  │    │  Service         │
└─────────────┘     └─────────────────┘
```

### 6.3 Rollback Strategy

| Scenario | Action |
|---|---|
| Data load failure | Re-run Spring Batch job from last checkpoint |
| Data corruption detected | Truncate target tables; re-extract from mainframe |
| Application issues post-cutover | Route traffic back to CICS; mainframe remains source of truth until cutover is confirmed |
| Financial discrepancy found | Halt migration; investigate COMP-3 unpacking; re-validate |

---

## 7. Test Plan

### 7.1 Test Strategy Overview

| Test Level | Scope | Tools | COBOL Equivalent |
|---|---|---|---|
| Unit Tests | Individual service methods, validators, parsers | JUnit 5, Mockito | PORTTEST paragraph-level checks |
| Integration Tests | Repository + DB layer; REST API endpoints | Testcontainers (PostgreSQL), REST Assured | PORTTEST full-program run |
| Data Migration Tests | COMP-3 parsing, date conversion, record counts | JUnit 5, custom assertions | TSTVAL00 |
| Equivalence Tests | Same input → same output (COBOL vs Java) | Parameterized JUnit tests | TSTGEN00 + TSTVAL00 |
| Performance Tests | Batch load throughput; API response times | JMeter, Gatling | JCL COND CODE checks |
| End-to-End Tests | Full CRUD lifecycle via API | REST Assured, Playwright | Manual CICS testing |

### 7.2 Unit Test Cases

#### 7.2.1 Portfolio Validation (PORTVALD equivalence)

| Test ID | Test Name | Input | Expected Result | Traces To |
|---|---|---|---|---|
| UT-V-001 | Valid portfolio ID | `PORT0001` | Passes validation | PORTVALD `1000-VALIDATE-ID` |
| UT-V-002 | Invalid prefix | `ACCT0001` | Fails with "Invalid Portfolio ID format" | PORTVALD `1000-VALIDATE-ID` |
| UT-V-003 | Non-numeric suffix | `PORTABCD` | Fails with "Invalid Portfolio ID format" | PORTVALD `1000-VALIDATE-ID` |
| UT-V-004 | Valid account number | `1234567890` | Passes validation | PORTVALD `2000-VALIDATE-ACCOUNT` |
| UT-V-005 | Non-numeric account | `ABCDE12345` | Fails with "Invalid Account Number format" | PORTVALD `2000-VALIDATE-ACCOUNT` |
| UT-V-006 | Zero account number | `0000000000` | Fails with "Invalid Account Number format" | PORTVALD `2000-VALIDATE-ACCOUNT` |
| UT-V-007 | Valid investment type STK | `STK` | Passes | PORTVALD `3000-VALIDATE-TYPE` |
| UT-V-008 | Valid investment type BND | `BND` | Passes | PORTVALD `3000-VALIDATE-TYPE` |
| UT-V-009 | Valid investment type MMF | `MMF` | Passes | PORTVALD `3000-VALIDATE-TYPE` |
| UT-V-010 | Valid investment type ETF | `ETF` | Passes | PORTVALD `3000-VALIDATE-TYPE` |
| UT-V-011 | Invalid investment type | `ABC` | Fails with "Invalid Investment Type" | PORTVALD `3000-VALIDATE-TYPE` |
| UT-V-012 | Amount at max boundary | `9999999999999.99` | Passes | PORTVALD `4000-VALIDATE-AMOUNT` |
| UT-V-013 | Amount at min boundary | `-9999999999999.99` | Passes | PORTVALD `4000-VALIDATE-AMOUNT` |
| UT-V-014 | Amount exceeds max | `10000000000000.00` | Fails with "Amount outside valid range" | PORTVALD `4000-VALIDATE-AMOUNT` |
| UT-V-015 | Portfolio name required | `(spaces)` | Fails with "Portfolio Name is required" | PORTMSTR `2100-VALIDATE-PORTFOLIO` |
| UT-V-016 | Valid status A | `A` | Passes | PORTMSTR level-88 |
| UT-V-017 | Invalid status X | `X` | Fails with "Invalid Portfolio Status" | PORTMSTR level-88 |

#### 7.2.2 CRUD Operations (PORTMSTR equivalence)

| Test ID | Test Name | Expected Result | Traces To |
|---|---|---|---|
| UT-C-001 | Create valid portfolio | Portfolio persisted; returned with ID | PORTMSTR `2000-CREATE-PORTFOLIO` |
| UT-C-002 | Create duplicate portfolio | `DuplicatePortfolioException` (409) | PORTMSTR `PORT-DUP-KEY` |
| UT-C-003 | Read existing portfolio | Portfolio returned | PORTMSTR `3000-READ-PORTFOLIO` |
| UT-C-004 | Read non-existent portfolio | `PortfolioNotFoundException` (404) | PORTMSTR `PORT-NOT-FOUND` |
| UT-C-005 | Update portfolio name | Updated value persisted | PORTUPDT `UPDT-NAME` |
| UT-C-006 | Update portfolio status | Updated value persisted | PORTUPDT `UPDT-STATUS` |
| UT-C-007 | Update portfolio value | BigDecimal precision maintained | PORTUPDT `UPDT-VALUE` |
| UT-C-008 | Update non-existent portfolio | `PortfolioNotFoundException` (404) | PORTMSTR `PORT-NOT-FOUND` for update |
| UT-C-009 | Delete existing portfolio | Audit record created; portfolio removed/soft-deleted | PORTMSTR `5000-DELETE-PORTFOLIO` |
| UT-C-010 | Delete non-existent portfolio | `PortfolioNotFoundException` (404) | PORTMSTR `PORT-NOT-FOUND` for deletion |

#### 7.2.3 Transaction Processing (PORTTRAN equivalence)

| Test ID | Test Name | Expected Result | Traces To |
|---|---|---|---|
| UT-T-001 | Process buy transaction | Portfolio total value increases; audit logged | PORTTRAN `2210-PROCESS-BUY` |
| UT-T-002 | Process sell transaction | Portfolio total value decreases; audit logged | PORTTRAN `2220-PROCESS-SELL` |
| UT-T-003 | Sell exceeding available units | `InsufficientUnitsException` (422) | PORTTRAN `2220-PROCESS-SELL` check |
| UT-T-004 | Process fee | Portfolio total cost reduced by fee amount | PORTTRAN `2240-PROCESS-FEE` |
| UT-T-005 | Transaction with invalid portfolio | Error returned | PORTTRAN `2110-CHECK-PORTFOLIO` |
| UT-T-006 | Transaction with invalid type | Error returned | PORTTRAN `2120-CHECK-TRANSACTION-TYPE` |
| UT-T-007 | Transaction with zero quantity | Error returned | PORTTRAN `2130-CHECK-AMOUNTS` |
| UT-T-008 | Transaction with zero price (non-transfer) | Error returned | PORTTRAN `2130-CHECK-AMOUNTS` |
| UT-T-009 | Audit record created after transaction | Audit log entry with correct action/status | PORTTRAN `2300-UPDATE-AUDIT-TRAIL` |

### 7.3 Integration Test Cases

| Test ID | Test Name | Description | Scope |
|---|---|---|---|
| IT-001 | Full CRUD lifecycle | Create → Read → Update → Delete via REST API | API + DB |
| IT-002 | Batch portfolio import | Load 1,000 records via Spring Batch job | Batch + DB |
| IT-003 | Transaction cascade | Buy → Sell → Check balance updates | Service + DB |
| IT-004 | Concurrent updates | Parallel updates to same portfolio | Optimistic locking |
| IT-005 | Pagination and filtering | List portfolios with status/type filters | API + DB |
| IT-006 | Audit trail completeness | Every mutation produces audit record | Service + DB |
| IT-007 | Database constraints | Unique, FK, CHECK constraints enforced | DB layer |

### 7.4 Data Migration Test Cases

| Test ID | Test Name | Description | Acceptance |
|---|---|---|---|
| DM-001 | COMP-3 unpacking precision | Unpack known COMP-3 values and verify decimal result | Exact match to 2 decimal places |
| DM-002 | Date conversion YYYYMMDD | Parse all date formats including edge cases (leap year, end-of-month) | 0 parse errors |
| DM-003 | Fixed-width string trimming | Verify trailing spaces are removed, leading preserved | String equality after trim |
| DM-004 | Record count parity | Compare input record count with loaded row count | 100% match |
| DM-005 | Financial total reconciliation | Sum all PORT-TOTAL-VALUE in source vs target | Difference < $0.01 total |
| DM-006 | Status code mapping completeness | All source status codes map to valid enums | 0 unmapped values |
| DM-007 | Referential integrity post-load | All FK references resolve | 0 orphan records |
| DM-008 | Empty/null field handling | COBOL SPACES → null or empty; COBOL ZEROS → 0.00 | Consistent treatment |
| DM-009 | Checkpoint/restart | Interrupt load at 50%; restart; verify no duplicates | Idempotent load |
| DM-010 | Large-volume load | Load 1M records; verify throughput and correctness | < 30 minutes; 0 errors |

### 7.5 Equivalence Test Cases (COBOL vs Java)

These tests use **PORTTEST.cbl-generated data** as a common input, process it through both systems, and compare outputs:

| Test ID | Description | Input | Comparison Point |
|---|---|---|---|
| EQ-001 | Portfolio creation equivalence | PORTTEST-generated records | Record content match |
| EQ-002 | Validation rule parity | Invalid portfolio IDs from TSTVAL00 | Same accept/reject decisions |
| EQ-003 | Financial calculation precision | Buy/Sell transactions with known amounts | Balance delta exact to 0.01 |
| EQ-004 | Status transition parity | A→C, A→S, S→A transitions | Same allowed/rejected transitions |
| EQ-005 | Audit trail content parity | CRUD operations | Matching audit records (format may differ) |

### 7.6 Performance Test Criteria

| Metric | Target | COBOL Baseline | Notes |
|---|---|---|---|
| API response time (single read) | < 50ms p95 | ~10ms (VSAM direct) | DB indexed lookup |
| API response time (create) | < 100ms p95 | ~20ms (VSAM write) | Includes validation + audit |
| Batch load throughput | > 10,000 records/sec | ~50,000 records/sec (VSAM) | Acceptable for non-mainframe |
| Concurrent users | 100 simultaneous | N/A (CICS managed) | Spring thread pool |
| Database size (1M portfolios) | < 2 GB | ~400 MB (VSAM) | Acceptable overhead |

---

## 8. Migration Phases & Timeline

### Phase 0 — Foundation (Weeks 1–2)

| Task | Deliverable |
|---|---|
| Scaffold Spring Boot project (Java 21, Gradle) | `build.gradle.kts` with dependencies |
| Configure PostgreSQL + Flyway | `application.yml`, `V1__create_schema.sql` |
| Create JPA entities and enums | All entity classes per Section 4 |
| Create Spring Data repositories | Repository interfaces with custom queries |
| Set up JPA auditing | `AuditingEntityListener` configuration |
| Set up OpenAPI/Swagger | Swagger UI at `/swagger-ui.html` |

### Phase 1 — Core Services (Weeks 3–4)

| Task | Deliverable | COBOL Source |
|---|---|---|
| Implement `PortfolioValidationService` | Validation logic matching PORTVALD | `PORTVALD.cbl` |
| Implement `PortfolioService` CRUD | Create, Read, Update, Delete operations | `PORTMSTR.cbl` |
| Implement `AuditService` | Audit trail logging on all mutations | `AUDPROC` calls |
| Implement `PortfolioController` | REST API endpoints | — |
| Write unit tests for validation | 17+ test cases per Section 7.2.1 | `PORTVALD.cbl` |
| Write unit tests for CRUD | 10+ test cases per Section 7.2.2 | `PORTMSTR.cbl` |

### Phase 2 — Transaction Processing (Weeks 5–6)

| Task | Deliverable | COBOL Source |
|---|---|---|
| Implement `TransactionService` | Buy, Sell, Fee processing | `PORTTRAN.cbl` |
| Implement `TransactionController` | Transaction REST API | — |
| Write transaction unit tests | 9+ test cases per Section 7.2.3 | `PORTTRAN.cbl` |
| Write integration tests | 7 test cases per Section 7.3 | — |

### Phase 3 — Data Migration (Weeks 7–8)

| Task | Deliverable |
|---|---|
| Build `CobolRecordParser` (COMP-3, dates, strings) | Parser utility with unit tests |
| Build Spring Batch job for portfolio load | Job with chunk processing, skip/retry |
| Build load jobs for positions, transactions, history | Additional batch jobs |
| Run data migration tests | 10 test cases per Section 7.4 |
| Perform equivalence testing | 5 test cases per Section 7.5 |

### Phase 4 — Parallel Run & Cutover (Weeks 9–10)

| Task | Deliverable |
|---|---|
| Deploy Spring Boot app to staging | Running application |
| Execute full data migration | All records loaded and reconciled |
| Run parallel reconciliation for 1 week | Daily reconciliation reports |
| Performance testing | Results meeting criteria per Section 7.6 |
| Cutover decision gate | Go/no-go based on reconciliation results |
| Production cutover | Spring Boot becomes primary system |

---

## 9. Risk Register

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R-001 | COMP-3 unpacking produces incorrect decimal values | Medium | Critical | Extensive test suite with known COMP-3 values; bit-level verification |
| R-002 | Date conversion edge cases (leap years, century boundaries) | Low | High | Parameterized tests covering all edge cases; use `java.time` exclusively |
| R-003 | Financial precision loss during migration | Medium | Critical | **Never use float/double**; BigDecimal throughout; reconciliation checks to $0.01 |
| R-004 | Orphan records during migration (FK violations) | Medium | Medium | Load in FK-dependency order; validate referential integrity post-load |
| R-005 | Performance regression vs VSAM direct access | High | Medium | Index optimization; connection pooling; caching for hot portfolios |
| R-006 | Business rules undocumented in COBOL code | Medium | High | Extract rules during analysis; validate with business stakeholders |
| R-007 | Concurrent access patterns differ (VSAM SHARE vs DB locks) | Medium | Medium | Optimistic locking (`@Version`); test under concurrent load |
| R-008 | Batch window replacement impact | Low | Medium | Spring Batch with checkpoint/restart mirrors COBOL CKPRST pattern |
| R-009 | Mainframe extract availability during migration | Medium | High | Pre-schedule extract windows; maintain extract scripts in SCM |
| R-010 | CICS online inquiry not migrated in Phase 1 | Low | Low | Clearly scoped; CICS continues to run until Phase 2 migration |

---

## 10. Appendices

### Appendix A — COBOL-to-Java Type Quick Reference

| COBOL PIC Clause | Bytes | Java Type | JPA Column |
|---|---|---|---|
| `PIC X(n)` | n | `String` | `VARCHAR(n)` |
| `PIC 9(n)` | n | `int` / `long` | `INTEGER` / `BIGINT` |
| `PIC S9(n)V99 COMP-3` | ⌈(n+3)/2⌉ | `BigDecimal` | `DECIMAL(n+2, 2)` |
| `PIC S9(n)V9(m) COMP-3` | ⌈(n+m+1)/2⌉ | `BigDecimal` | `DECIMAL(n+m, m)` |
| `PIC S9(n) COMP` | 2 or 4 | `int` / `long` | `INTEGER` / `BIGINT` |
| `PIC 9(8)` (date) | 8 | `LocalDate` | `DATE` |
| `PIC X(26)` (timestamp) | 26 | `LocalDateTime` | `TIMESTAMP` |
| Level-88 condition | — | `enum` | `CHAR(1)` or `VARCHAR(2)` |
| `FILLER` | n | *(dropped)* | *(none)* |

### Appendix B — VSAM Status Code-to-Exception Map

| VSAM Status | Meaning | HTTP Status | Java Exception |
|---|---|---|---|
| `00` | Success | 200/201 | *(none)* |
| `10` | End of file | 200 (empty result) | *(none — empty list)* |
| `22` | Duplicate key | 409 | `DuplicatePortfolioException` |
| `23` | Record not found | 404 | `PortfolioNotFoundException` |
| `35` | File not found | 503 | `ServiceUnavailableException` |

### Appendix C — Source File Cross-Reference Index

| Source File | Section(s) Referenced |
|---|---|
| `src/copybook/common/PORTFLIO.cpy` | 2.1, 4.1 |
| `src/copybook/common/POSREC.cpy` | 4.2 |
| `src/copybook/common/TRNREC.cpy` | 4.3 |
| `src/copybook/common/HISTREC.cpy` | 4.4 |
| `src/copybook/common/AUDITLOG.cpy` | 4.5 |
| `src/copybook/common/PORTVAL.cpy` | 2.4, 4.6 |
| `src/copybook/common/COMMON.cpy` | 4.6 |
| `src/copybook/common/ERRHAND.cpy` | 5.2 |
| `src/programs/portfolio/PORTMSTR.cbl` | 2.2, 5.1, 7.2.2 |
| `src/programs/portfolio/PORTVALD.cbl` | 2.4, 5.1, 7.2.1 |
| `src/programs/portfolio/PORTADD.cbl` | 2.2, 6.2 |
| `src/programs/portfolio/PORTREAD.cbl` | 2.2, 5.1 |
| `src/programs/portfolio/PORTUPDT.cbl` | 2.2, 5.1 |
| `src/programs/portfolio/PORTDEL.cbl` | 2.2, 5.1 |
| `src/programs/portfolio/PORTTRAN.cbl` | 2.2, 5.1, 7.2.3 |
| `src/programs/portfolio/PORTTEST.cbl` | 7.5 |
| `src/database/db2/db2-definitions.sql` | 4.1, 4.7 |
| `src/database/db2/POSHIST.sql` | 2.3, 6.2 |
| `src/database/vsam/vsam-definitions.txt` | 2.1, 6.2 |
