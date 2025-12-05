# Data Model Mapping Documentation

## Overview

This document describes the data model mapping from the legacy COBOL/VSAM/DB2 system to the new PostgreSQL-based Java implementation. The migration preserves all business logic, data integrity rules, and audit trail requirements from the original system.

## Source System Architecture

The legacy system uses a dual-storage strategy:
- **VSAM Files**: Operational data with high-performance indexed access
- **DB2 Tables**: Historical and analytical data with SQL query capabilities

## Data Type Conversions

### COBOL to Java Type Mapping

| COBOL Type | Description | Java Type | PostgreSQL Type |
|------------|-------------|-----------|-----------------|
| PIC X(n) | Alphanumeric | String | VARCHAR(n) |
| PIC 9(n) | Numeric display | int/long | INTEGER/BIGINT |
| PIC S9(n)V9(m) COMP-3 | Packed decimal | BigDecimal | DECIMAL(n+m, m) |
| PIC S9(n) COMP | Binary | int/long | INTEGER/BIGINT |
| PIC 9(8) (YYYYMMDD) | Date | LocalDate | DATE |
| PIC 9(6) (HHMMSS) | Time | LocalTime | TIME |
| PIC X(26) | Timestamp | OffsetDateTime | TIMESTAMP WITH TIME ZONE |

### Critical Financial Data Handling

**IMPORTANT**: All financial calculations use `BigDecimal` to preserve precision. Never use `float` or `double` for monetary values.

Example conversions:
- `PIC S9(13)V99 COMP-3` → `BigDecimal` with scale 2 → `DECIMAL(15,2)`
- `PIC S9(11)V9(4) COMP-3` → `BigDecimal` with scale 4 → `DECIMAL(15,4)`

## Entity Mappings

### 1. Portfolio Master

**Source**: VSAM PORTMSTR (KSDS, 400 bytes)
**COBOL Copybook**: PORTFLIO.cpy
**Target**: `portfolio_master` table

| COBOL Field | Java Field | PostgreSQL Column | Notes |
|-------------|------------|-------------------|-------|
| PORT-ID | portfolioId | portfolio_id | VARCHAR(8), Part of composite key |
| PORT-ACCOUNT-NO | accountNo | account_no | VARCHAR(10) |
| PORT-CLIENT-NAME | clientName | client_name | VARCHAR(30) |
| PORT-CLIENT-TYPE | clientType | client_type | CHAR(1): I/C/T |
| PORT-CREATE-DATE | openDate | open_date | DATE |
| PORT-STATUS | status | status | CHAR(1): A/C/S |
| PORT-TOTAL-VALUE | totalValue | total_value | DECIMAL(15,2) |
| PORT-CASH-BALANCE | cashBalance | cash_balance | DECIMAL(15,2) |

**Key Structure**:
- VSAM Key: Portfolio ID (8) + Account Type (2) + Branch ID (2)
- PostgreSQL: Unique constraint on (portfolio_id, account_type, branch_id)

### 2. Investment Positions

**Source**: VSAM POSHIST (KSDS, 350 bytes)
**COBOL Copybook**: POSREC.cpy
**Target**: `investment_positions` table

| COBOL Field | Java Field | PostgreSQL Column | Notes |
|-------------|------------|-------------------|-------|
| POS-PORTFOLIO-ID | portfolioId | portfolio_id | VARCHAR(8) |
| POS-DATE | positionDate | position_date | DATE |
| POS-INVESTMENT-ID | investmentId | investment_id | VARCHAR(10) |
| POS-QUANTITY | quantity | quantity | DECIMAL(15,4) |
| POS-COST-BASIS | costBasis | cost_basis | DECIMAL(15,2) |
| POS-MARKET-VALUE | marketValue | market_value | DECIMAL(15,2) |
| POS-CURRENCY | currencyCode | currency_code | CHAR(3) |
| POS-STATUS | status | status | CHAR(1): A/C/P |

**Key Structure**:
- VSAM Key: Portfolio ID (8) + Date (8) + Investment ID (10)
- PostgreSQL: Unique constraint on (portfolio_id, investment_id, position_date)

### 3. Transaction Records

**Source**: VSAM TRANHIST (KSDS, 300 bytes)
**COBOL Copybook**: TRNREC.cpy
**Target**: `transaction_records` table

| COBOL Field | Java Field | PostgreSQL Column | Notes |
|-------------|------------|-------------------|-------|
| TRN-DATE | transactionDate | transaction_date | DATE |
| TRN-TIME | transactionTime | transaction_time | TIME |
| TRN-PORTFOLIO-ID | portfolioId | portfolio_id | VARCHAR(8) |
| TRN-SEQUENCE-NO | sequenceNo | sequence_no | VARCHAR(6) |
| TRN-INVESTMENT-ID | investmentId | investment_id | VARCHAR(10) |
| TRN-TYPE | transactionType | transaction_type | CHAR(2): BU/SL/TR/FE |
| TRN-QUANTITY | quantity | quantity | DECIMAL(15,4) |
| TRN-PRICE | price | price | DECIMAL(15,4) |
| TRN-AMOUNT | amount | amount | DECIMAL(15,2) |
| TRN-CURRENCY | currencyCode | currency_code | CHAR(3) |
| TRN-STATUS | status | status | CHAR(1): P/D/F/R |

**Key Structure**:
- VSAM Key: Date (8) + Time (6) + Portfolio ID (8) + Sequence (6)
- PostgreSQL: Unique constraint on (transaction_date, transaction_time, portfolio_id, sequence_no)

### 4. Position History

**Source**: DB2 POSHIST table
**COBOL Copybook**: DBTBLS.cpy
**Target**: `position_history` table (partitioned)

| DB2 Column | Java Field | PostgreSQL Column | Notes |
|------------|------------|-------------------|-------|
| ACCOUNT_NO | accountNo | account_no | VARCHAR(8) |
| PORTFOLIO_ID | portfolioId | portfolio_id | VARCHAR(10) |
| TRANS_DATE | transDate | trans_date | DATE (partition key) |
| TRANS_TIME | transTime | trans_time | TIME |
| TRANS_TYPE | transType | trans_type | CHAR(2) |
| SECURITY_ID | securityId | security_id | VARCHAR(12) |
| QUANTITY | quantity | quantity | DECIMAL(15,3) |
| PRICE | price | price | DECIMAL(15,3) |
| AMOUNT | amount | amount | DECIMAL(15,2) |
| FEES | fees | fees | DECIMAL(15,2) |
| TOTAL_AMOUNT | totalAmount | total_amount | DECIMAL(15,2) |
| COST_BASIS | costBasis | cost_basis | DECIMAL(15,2) |
| GAIN_LOSS | gainLoss | gain_loss | DECIMAL(15,2) |

**Partitioning**:
- Quarterly partitions by trans_date
- Partitions created for 2024-2025

### 5. Error Log

**Source**: DB2 ERRLOG table
**Target**: `error_log` table

| DB2 Column | Java Field | PostgreSQL Column | Notes |
|------------|------------|-------------------|-------|
| ERROR_TIMESTAMP | errorTimestamp | error_timestamp | TIMESTAMP WITH TIME ZONE |
| PROGRAM_ID | programId | program_id | VARCHAR(8) |
| ERROR_TYPE | errorType | error_type | CHAR(1): S/A/D |
| ERROR_SEVERITY | errorSeverity | error_severity | INTEGER (1-4) |
| ERROR_CODE | errorCode | error_code | VARCHAR(8) |
| ERROR_MESSAGE | errorMessage | error_message | VARCHAR(200) |

### 6. Audit Log

**Source**: COBOL AUDITLOG copybook
**Target**: `audit_log` table

| COBOL Field | Java Field | PostgreSQL Column | Notes |
|-------------|------------|-------------------|-------|
| AUD-TIMESTAMP | auditTimestamp | audit_timestamp | TIMESTAMP WITH TIME ZONE |
| AUD-SYSTEM-ID | systemId | system_id | VARCHAR(8) |
| AUD-USER-ID | userId | user_id | VARCHAR(8) |
| AUD-PROGRAM | programId | program_id | VARCHAR(8) |
| AUD-TYPE | auditType | audit_type | VARCHAR(4): TRAN/USER/SYST |
| AUD-ACTION | action | action | VARCHAR(8) |
| AUD-STATUS | status | status | VARCHAR(4): SUCC/FAIL/WARN |

## Locking Strategy

### VSAM Record-Level Locking → PostgreSQL Row-Level Locking

The legacy VSAM system uses record-level locking for concurrent access. This is replicated in PostgreSQL using:

1. **Pessimistic Locking**: JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` for update operations
2. **Optimistic Locking**: `@Version` field for conflict detection
3. **Row-Level Locks**: PostgreSQL `SELECT ... FOR UPDATE` via JPA

Example repository method:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PortfolioMaster p WHERE p.portfolioId = :portfolioId")
Optional<PortfolioMaster> findByKeyWithLock(@Param("portfolioId") String portfolioId);
```

## Caching Strategy

### Redis Cache Configuration

| Cache Name | TTL | Purpose |
|------------|-----|---------|
| portfolioCache | 3600s (1 hour) | Portfolio master data |
| positionCache | 900s (15 min) | Investment positions |
| transactionCache | 1800s (30 min) | Transaction records |
| historyCache | 1800s (30 min) | Historical data queries |

### Cache Invalidation

- Cache entries are evicted on create/update/delete operations
- Uses Spring Cache `@CacheEvict` annotations
- Supports graceful degradation if Redis is unavailable

## Batch Processing

### Spring Batch Migration Jobs

1. **portfolioMigrationJob**: Migrates VSAM PORTMSTR to portfolio_master
2. **transactionMigrationJob**: Migrates VSAM TRANHIST to transaction_records

### Checkpoint/Restart Support

- `batch_control` table tracks job execution status
- `checkpoint_record` table stores restart positions
- Configurable chunk size (default: 1000 records)
- Retry logic with configurable max retries (default: 3)

## Index Strategy

### Primary Indexes

All tables have UUID primary keys with automatic generation.

### Secondary Indexes

Indexes are created to support common query patterns:

| Table | Index | Columns |
|-------|-------|---------|
| portfolio_master | idx_portfolio_master_client | client_id, status |
| portfolio_master | idx_portfolio_master_status | status, open_date |
| investment_positions | idx_positions_portfolio | portfolio_id, position_date |
| transaction_records | idx_transactions_portfolio | portfolio_id, transaction_date |
| position_history | idx_poshist_account | account_no, portfolio_id, trans_date |

## Migration Validation

### Data Integrity Checks

1. **Record Count Validation**: Compare source and target record counts
2. **Checksum Validation**: Verify financial totals match
3. **Key Uniqueness**: Ensure no duplicate keys after migration
4. **Referential Integrity**: Validate foreign key relationships

### Rollback Strategy

- All migrations run within transactions
- Failed batches can be restarted from last checkpoint
- Original VSAM/DB2 data remains unchanged during migration
