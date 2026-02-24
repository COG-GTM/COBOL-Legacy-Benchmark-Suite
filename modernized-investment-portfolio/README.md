# Modernized Investment Portfolio Management System

A Spring Boot / JPA / PostgreSQL modernization of the Investment Portfolio Management System, originally implemented in Enterprise COBOL for z/OS with DB2 and VSAM data stores.

## Overview

This project migrates the database layer of the COBOL-Legacy-Benchmark-Suite to a modern Java stack:

| Legacy Component | Modern Replacement |
|---|---|
| DB2 for z/OS | PostgreSQL |
| COBOL Copybooks | JPA Entity Classes |
| VSAM KSDS/ESDS | Spring Data JPA Repositories |
| DB2 Connection Pool (PORTPLAN) | HikariCP (100 connections, 300s timeout) |
| DB2 DDL Scripts | Flyway Migration Scripts |

## Schema Mapping: COBOL to PostgreSQL

### Tables

| DB2 Table | PostgreSQL Table | COBOL Copybook | JPA Entity |
|---|---|---|---|
| `PORTFOLIO_MASTER` | `portfolio_master` | `PORTFLIO.cpy` (PORT-RECORD) | `PortfolioMaster` |
| `INVESTMENT_POSITIONS` | `investment_positions` | `POSREC.cpy` (POSITION-RECORD) | `InvestmentPosition` |
| `TRANSACTION_HISTORY` | `transaction_history` | `TRNREC.cpy` (TRANSACTION-RECORD) | `Transaction` |
| `POSHIST` | `position_history` | `HISTREC.cpy` / `DBTBLS.cpy` | `TransactionHistory` |
| `ERRLOG` | `error_log` | `DBTBLS.cpy` (ERRLOG-RECORD) | `ErrorLog` |
| `RTNCODES` | `return_codes` | `RTNCODE.cpy` / `COMMON.cpy` | `ReturnCode` |

### Field Mapping Details

#### Portfolio Master (PORTFLIO.cpy -> portfolio_master)

| COBOL Field | PIC Clause | PostgreSQL Column | SQL Type |
|---|---|---|---|
| `PORT-ID` | `X(8)` | `portfolio_id` | `CHAR(8)` PK |
| `PORT-ACCOUNT-NO` | `X(10)` | `client_id` | `CHAR(10)` |
| `PORT-CLIENT-NAME` | `X(30)` | `portfolio_name` | `VARCHAR(50)` |
| `PORT-CLIENT-TYPE` | `X(1)` | `account_type` | `CHAR(2)` |
| `PORT-STATUS` | `X(1)` | `status` | `CHAR(1)` |
| `PORT-CREATE-DATE` | `9(8)` | `open_date` | `DATE` |
| `PORT-LAST-MAINT` | `9(8)` | `last_maint_date` | `TIMESTAMP` |
| `PORT-LAST-USER` | `X(8)` | `last_maint_user` | `VARCHAR(8)` |

#### Investment Position (POSREC.cpy -> investment_positions)

| COBOL Field | PIC Clause | PostgreSQL Column | SQL Type |
|---|---|---|---|
| `POS-PORTFOLIO-ID` | `X(08)` | `portfolio_id` | `CHAR(8)` PK |
| `POS-INVESTMENT-ID` | `X(10)` | `investment_id` | `CHAR(10)` PK |
| `POS-DATE` | `X(08)` | `position_date` | `DATE` PK |
| `POS-QUANTITY` | `S9(11)V9(4)` | `quantity` | `NUMERIC(18,4)` |
| `POS-COST-BASIS` | `S9(13)V9(2)` | `cost_basis` | `NUMERIC(18,2)` |
| `POS-MARKET-VALUE` | `S9(13)V9(2)` | `market_value` | `NUMERIC(18,2)` |
| `POS-CURRENCY` | `X(03)` | `currency_code` | `CHAR(3)` |
| `POS-LAST-MAINT-DATE` | `X(26)` | `last_maint_date` | `TIMESTAMP` |
| `POS-LAST-MAINT-USER` | `X(08)` | `last_maint_user` | `VARCHAR(8)` |

#### Transaction (TRNREC.cpy -> transaction_history)

| COBOL Field | PIC Clause | PostgreSQL Column | SQL Type |
|---|---|---|---|
| `TRN-KEY` (composite) | `X(28)` | `transaction_id` | `CHAR(20)` PK |
| `TRN-PORTFOLIO-ID` | `X(08)` | `portfolio_id` | `CHAR(8)` FK |
| `TRN-DATE` | `X(08)` | `transaction_date` | `DATE` |
| `TRN-TIME` | `X(06)` | `transaction_time` | `TIME` |
| `TRN-INVESTMENT-ID` | `X(10)` | `investment_id` | `CHAR(10)` |
| `TRN-TYPE` | `X(02)` | `transaction_type` | `CHAR(2)` |
| `TRN-QUANTITY` | `S9(11)V9(4)` | `quantity` | `NUMERIC(18,4)` |
| `TRN-PRICE` | `S9(11)V9(4)` | `price` | `NUMERIC(18,4)` |
| `TRN-AMOUNT` | `S9(13)V9(2)` | `amount` | `NUMERIC(18,2)` |
| `TRN-CURRENCY` | `X(03)` | `currency_code` | `CHAR(3)` |
| `TRN-STATUS` | `X(01)` | `status` | `CHAR(1)` |
| `TRN-PROCESS-DATE` | `X(26)` | `process_date` | `TIMESTAMP` |
| `TRN-PROCESS-USER` | `X(08)` | `process_user` | `VARCHAR(8)` |

#### Position History (HISTREC.cpy / POSHIST.sql -> position_history)

| COBOL/DB2 Field | Type | PostgreSQL Column | SQL Type |
|---|---|---|---|
| `ACCOUNT_NO` | `CHAR(8)` | `account_no` | `CHAR(8)` PK |
| `PORTFOLIO_ID` | `CHAR(10)` | `portfolio_id` | `CHAR(10)` PK |
| `TRANS_DATE` | `DATE` | `trans_date` | `DATE` PK |
| `TRANS_TIME` | `TIME` | `trans_time` | `TIME` PK |
| `TRANS_TYPE` | `CHAR(2)` | `trans_type` | `CHAR(2)` |
| `SECURITY_ID` | `CHAR(12)` | `security_id` | `CHAR(12)` |
| `QUANTITY` | `DECIMAL(15,3)` | `quantity` | `NUMERIC(15,3)` |
| `PRICE` | `DECIMAL(15,3)` | `price` | `NUMERIC(15,3)` |
| `AMOUNT` | `DECIMAL(15,2)` | `amount` | `NUMERIC(15,2)` |
| `FEES` | `DECIMAL(15,2)` | `fees` | `NUMERIC(15,2)` |
| `TOTAL_AMOUNT` | `DECIMAL(15,2)` | `total_amount` | `NUMERIC(15,2)` |
| `COST_BASIS` | `DECIMAL(15,2)` | `cost_basis` | `NUMERIC(15,2)` |
| `GAIN_LOSS` | `DECIMAL(15,2)` | `gain_loss` | `NUMERIC(15,2)` |
| `PROCESS_DATE` | `DATE` | `process_date` | `DATE` |
| `PROCESS_TIME` | `TIME` | `process_time` | `TIME` |
| `PROGRAM_ID` | `CHAR(8)` | `program_id` | `CHAR(8)` |
| `USER_ID` | `CHAR(8)` | `user_id` | `CHAR(8)` |
| `AUDIT_TIMESTAMP` | `TIMESTAMP` | `audit_timestamp` | `TIMESTAMP` |

### Key Design Decisions

#### Composite Keys (EmbeddedId)

The COBOL VSAM files use composite keys. These are preserved in JPA using `@EmbeddedId`:

- **InvestmentPosition**: `(portfolio_id, investment_id, position_date)` - matches VSAM POSMSTRE key
- **TransactionHistory**: `(account_no, portfolio_id, trans_date, trans_time)` - matches POSHIST PK
- **ErrorLog**: `(error_timestamp, program_id)` - matches ERRLOG PK
- **ReturnCode**: `(log_timestamp, program_id)` - matches RTNCODES PK

#### Date-Range Indexing (Replacing DB2 Partitioning)

The original DB2 POSHIST table used quarterly range partitioning on `TRANS_DATE`. PostgreSQL replaces this with:

1. **BRIN indexes** on date columns for efficient sequential range scans
2. Standard B-tree indexes for point lookups

This provides comparable performance for the date-range query patterns used in the COBOL batch processing.

#### DB2-to-PostgreSQL Type Mapping

| DB2 Type | PostgreSQL Type | Notes |
|---|---|---|
| `CHAR(n)` | `CHAR(n)` | Direct mapping |
| `VARCHAR(n)` | `VARCHAR(n)` | Direct mapping |
| `DECIMAL(p,s)` | `NUMERIC(p,s)` | PostgreSQL NUMERIC is equivalent |
| `DATE` | `DATE` | Direct mapping |
| `TIME` | `TIME` | Direct mapping |
| `TIMESTAMP` | `TIMESTAMP` | Direct mapping |
| `INTEGER` | `INTEGER` | Direct mapping |

#### Code/Status Value Mapping

| Domain | Values | Source |
|---|---|---|
| Portfolio Status | A=Active, C=Closed, S=Suspended | COMMON.cpy STATUS-CODES |
| Transaction Type | BU=Buy, SL=Sell, TR=Transfer, FE=Fee | COMMON.cpy TRANSACTION-TYPES |
| Transaction Status | P=Processed, F=Failed, R=Reversed | TRNREC.cpy TRN-STATUS |
| Error Type | S=System, A=Application, D=Data | ERRLOG.sql |
| Error Severity | 1=Info, 2=Warning, 3=Error, 4=Severe | ERRLOG.sql |
| Return Codes | 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Critical | COMMON.cpy RETURN-CODES |

## Project Structure

```
modernized-investment-portfolio/
├── build.gradle                          # Gradle build with Spring Boot, JPA, PostgreSQL, Flyway
├── src/main/java/com/investment/portfolio/
│   ├── ModernizedInvestmentPortfolioApplication.java
│   ├── config/
│   │   └── DataSourceConfig.java         # HikariCP configuration documentation
│   ├── entity/
│   │   ├── PortfolioMaster.java          # From PORTFLIO.cpy
│   │   ├── InvestmentPosition.java       # From POSREC.cpy (composite key)
│   │   ├── InvestmentPositionId.java     # @EmbeddedId for Position
│   │   ├── Transaction.java             # From TRNREC.cpy
│   │   ├── TransactionHistory.java       # From HISTREC.cpy / POSHIST.sql
│   │   ├── TransactionHistoryId.java     # @EmbeddedId for History
│   │   ├── ErrorLog.java                # From ERRLOG.sql
│   │   ├── ErrorLogId.java              # @EmbeddedId for ErrorLog
│   │   ├── ReturnCode.java              # From RTNCODES.sql
│   │   └── ReturnCodeId.java            # @EmbeddedId for ReturnCode
│   └── repository/
│       ├── PortfolioMasterRepository.java
│       ├── InvestmentPositionRepository.java
│       ├── TransactionRepository.java
│       ├── TransactionHistoryRepository.java
│       ├── ErrorLogRepository.java
│       └── ReturnCodeRepository.java
├── src/main/resources/
│   ├── application.properties            # PostgreSQL + HikariCP + Flyway config
│   └── db/migration/
│       ├── V1__create_portfolio_master.sql
│       ├── V2__create_investment_positions.sql
│       ├── V3__create_transaction_history.sql
│       ├── V4__create_position_history.sql
│       ├── V5__create_error_log.sql
│       ├── V6__create_return_codes.sql
│       └── V7__create_views.sql
└── src/test/
    ├── java/.../ModernizedInvestmentPortfolioApplicationTests.java
    └── resources/application-test.properties
```

## Dependencies

- **Spring Boot 3.2.5** (Java 17)
- **Spring Data JPA** - ORM / repository layer
- **PostgreSQL Driver** - database connectivity
- **HikariCP** - connection pooling (included with Spring Boot)
- **Flyway** - database migration management
- **Spring Boot Actuator** - health/metrics endpoints
- **Spring Boot Validation** - bean validation (JSR 380)

## HikariCP Configuration

Matches the original DB2 connection pool (PORTPLAN.sql):

| Property | Value | Rationale |
|---|---|---|
| `maximum-pool-size` | 100 | Matches DB2 pool capacity |
| `connection-timeout` | 300,000 ms (5 min) | Matches DB2 300s timeout |
| `minimum-idle` | 10 | Warm pool for batch processing |
| `idle-timeout` | 600,000 ms (10 min) | Cleanup idle connections |
| `max-lifetime` | 1,800,000 ms (30 min) | Prevent stale connections |

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 14+

### Database Setup

```sql
CREATE DATABASE investment_portfolio;
CREATE USER portfolio_user WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE investment_portfolio TO portfolio_user;
```

### Build and Run

```bash
./gradlew compileJava    # Compile
./gradlew test           # Run tests (uses H2 in-memory)
./gradlew bootRun        # Start application
```

Flyway will automatically run migrations on startup.

## References

- **Original DB2 DDL**: `src/database/db2/db2-definitions.sql`, `POSHIST.sql`, `ERRLOG.sql`, `RTNCODES.sql`
- **COBOL Copybooks**: `src/copybook/common/` (TRNREC, POSREC, HISTREC, PORTFLIO)
- **DB2 Copybooks**: `src/copybook/db2/` (DBTBLS, DBPROC)
- **Data Dictionary**: `documentation/technical/data-dictionary.md`
- **System Architecture**: `documentation/technical/system-architecture.md`
