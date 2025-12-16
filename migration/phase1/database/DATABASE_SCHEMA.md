# Database Schema Documentation

## Overview

This document describes the PostgreSQL database schema for the modernized Investment Portfolio Management System. The schema is designed to replace the legacy VSAM files and DB2 tables while maintaining data integrity and supporting the same business operations.

## Schema: `portfolio`

All tables are created in the `portfolio` schema to provide logical separation from other applications.

## Tables

### Core Business Tables

#### `portfolios`

Portfolio master records, migrated from PORTMSTR VSAM file.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| portfolio_id | VARCHAR(8) | NO | Business identifier (COBOL key) |
| account_type | VARCHAR(2) | NO | Account type code |
| branch_id | VARCHAR(2) | NO | Branch identifier |
| client_id | VARCHAR(10) | NO | Client identifier |
| portfolio_name | VARCHAR(50) | NO | Portfolio display name |
| currency_code | CHAR(3) | NO | Currency (default: USD) |
| risk_level | CHAR(1) | NO | Risk level (H/M/L) |
| status | ENUM | NO | ACTIVE, CLOSED, SUSPENDED |
| open_date | DATE | NO | Portfolio open date |
| close_date | DATE | YES | Portfolio close date |
| total_value | DECIMAL(18,2) | YES | Total market value |
| total_cost_basis | DECIMAL(18,2) | YES | Total cost basis |
| created_at | TIMESTAMP | NO | Record creation timestamp |
| updated_at | TIMESTAMP | NO | Last update timestamp |
| created_by | VARCHAR(8) | YES | User who created record |
| updated_by | VARCHAR(8) | YES | User who last updated |

**Unique Constraint**: (portfolio_id, account_type, branch_id)

#### `positions`

Portfolio positions, migrated from POSFILE VSAM (PORTFOLIO.POSITION.VSAM).

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| portfolio_id | VARCHAR(8) | NO | Portfolio identifier |
| position_date | DATE | NO | Position date |
| investment_id | VARCHAR(10) | NO | Investment/security identifier |
| cusip | VARCHAR(9) | YES | CUSIP number |
| quantity | DECIMAL(15,4) | NO | Number of shares/units |
| cost_basis | DECIMAL(15,2) | NO | Total cost basis |
| market_value | DECIMAL(15,2) | NO | Current market value |
| average_cost | DECIMAL(15,4) | YES | Average cost per share |
| currency_code | CHAR(3) | NO | Currency (default: USD) |
| status | ENUM | NO | ACTIVE, CLOSED, PENDING |
| last_transaction_id | VARCHAR(20) | YES | Last transaction reference |
| last_transaction_date | DATE | YES | Last transaction date |
| created_at | TIMESTAMP | NO | Record creation timestamp |
| updated_at | TIMESTAMP | NO | Last update timestamp |

**Unique Constraint**: (portfolio_id, position_date, investment_id)

**Source COBOL Structure** (POSREC.cpy):
```cobol
01 POSITION-RECORD.
   05 POS-KEY.
      10 POS-PORTFOLIO-ID    PIC X(8).
      10 POS-DATE            PIC X(8).
      10 POS-INVESTMENT-ID   PIC X(10).
   05 POS-QUANTITY           PIC S9(11)V9(4) COMP-3.
   05 POS-COST-BASIS         PIC S9(13)V99 COMP-3.
   05 POS-MARKET-VALUE       PIC S9(13)V99 COMP-3.
   05 POS-CURRENCY           PIC X(3).
   05 POS-STATUS             PIC X.
```

#### `transactions`

Transaction records, migrated from TRANHIST VSAM file.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| transaction_id | VARCHAR(20) | NO | Unique transaction identifier |
| portfolio_id | VARCHAR(8) | NO | Portfolio identifier |
| transaction_date | DATE | NO | Transaction date |
| transaction_time | TIME | NO | Transaction time |
| sequence_no | VARCHAR(6) | NO | Sequence number |
| investment_id | VARCHAR(10) | NO | Investment identifier |
| transaction_type | ENUM | NO | BUY, SELL, TRANSFER, FEE |
| quantity | DECIMAL(15,4) | NO | Transaction quantity |
| price | DECIMAL(15,4) | NO | Price per unit |
| amount | DECIMAL(15,2) | NO | Transaction amount |
| fees | DECIMAL(15,2) | YES | Transaction fees |
| total_amount | DECIMAL(15,2) | NO | Total including fees |
| currency_code | CHAR(3) | NO | Currency (default: USD) |
| status | ENUM | NO | PENDING, COMPLETED, FAILED, REVERSED |
| before_balance | DECIMAL(15,4) | YES | Balance before transaction |
| after_balance | DECIMAL(15,4) | YES | Balance after transaction |
| result_code | VARCHAR(4) | YES | Processing result code |
| process_date | TIMESTAMP | YES | Processing timestamp |
| process_user | VARCHAR(8) | YES | User who processed |

**Source COBOL Structure** (TRNREC.cpy):
```cobol
01 TRANSACTION-RECORD.
   05 TRN-KEY.
      10 TRN-DATE            PIC X(8).
      10 TRN-TIME            PIC X(6).
      10 TRN-PORTFOLIO-ID    PIC X(8).
      10 TRN-SEQUENCE-NO     PIC X(6).
   05 TRN-INVESTMENT-ID      PIC X(10).
   05 TRN-TYPE               PIC X(2).
   05 TRN-QUANTITY           PIC S9(11)V9(4) COMP-3.
   05 TRN-PRICE              PIC S9(11)V9(4) COMP-3.
   05 TRN-AMOUNT             PIC S9(13)V99 COMP-3.
   05 TRN-CURRENCY           PIC X(3).
   05 TRN-STATUS             PIC X.
```

### Historical and Reporting Tables

#### `position_history`

Position history for reporting, migrated from POSHIST DB2 table.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| account_no | VARCHAR(8) | NO | Account number |
| portfolio_id | VARCHAR(10) | NO | Portfolio identifier |
| trans_date | DATE | NO | Transaction date |
| trans_time | TIME | NO | Transaction time |
| trans_type | VARCHAR(2) | NO | Transaction type |
| security_id | VARCHAR(12) | NO | Security identifier |
| quantity | DECIMAL(15,3) | NO | Quantity |
| price | DECIMAL(15,3) | NO | Price |
| amount | DECIMAL(15,2) | NO | Amount |
| fees | DECIMAL(15,2) | YES | Fees |
| total_amount | DECIMAL(15,2) | NO | Total amount |
| cost_basis | DECIMAL(15,2) | NO | Cost basis |
| gain_loss | DECIMAL(15,2) | NO | Realized gain/loss |
| process_date | DATE | NO | Processing date |
| process_time | TIME | NO | Processing time |
| program_id | VARCHAR(8) | NO | Processing program |
| user_id | VARCHAR(8) | NO | Processing user |
| audit_timestamp | TIMESTAMP | NO | Audit timestamp |

**Source DB2 Structure** (POSHIST.sql):
```sql
CREATE TABLE POSHIST (
    ACCOUNT_NO      CHAR(8) NOT NULL,
    PORTFOLIO_ID    CHAR(10) NOT NULL,
    TRANS_DATE      DATE NOT NULL,
    TRANS_TIME      TIME NOT NULL,
    TRANS_TYPE      CHAR(2) NOT NULL,
    SECURITY_ID     CHAR(12) NOT NULL,
    QUANTITY        DECIMAL(15,3) NOT NULL,
    PRICE           DECIMAL(15,3) NOT NULL,
    AMOUNT          DECIMAL(15,2) NOT NULL,
    ...
);
```

### Security and Audit Tables

#### `users`

User accounts, migrated from AUTHFILE DB2 table.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| user_id | VARCHAR(8) | NO | Legacy user ID |
| username | VARCHAR(50) | NO | Login username |
| password_hash | VARCHAR(255) | NO | BCrypt password hash |
| email | VARCHAR(100) | YES | Email address |
| first_name | VARCHAR(50) | YES | First name |
| last_name | VARCHAR(50) | YES | Last name |
| department | VARCHAR(50) | YES | Department |
| role | VARCHAR(20) | NO | User role |
| is_active | BOOLEAN | NO | Account active flag |
| failed_login_attempts | INTEGER | YES | Failed login counter |
| last_login_at | TIMESTAMP | YES | Last login timestamp |
| password_changed_at | TIMESTAMP | YES | Password change timestamp |

#### `user_authorizations`

User resource authorizations, migrated from AUTHFILE structure.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| user_id | VARCHAR(8) | NO | User identifier (FK) |
| resource | VARCHAR(8) | NO | Resource name |
| access_type | VARCHAR(8) | NO | Access type (READ, UPDATE, etc.) |
| granted_at | TIMESTAMP | NO | Grant timestamp |
| granted_by | VARCHAR(8) | YES | Granting user |
| expires_at | TIMESTAMP | YES | Expiration timestamp |
| is_active | BOOLEAN | NO | Authorization active flag |

#### `audit_log`

Audit trail, migrated from AUDITLOG DB2 table.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| audit_timestamp | TIMESTAMP | NO | Event timestamp |
| system_id | VARCHAR(8) | YES | System identifier |
| user_id | VARCHAR(8) | NO | User who performed action |
| program_id | VARCHAR(8) | YES | Program identifier |
| terminal_id | VARCHAR(8) | YES | Terminal identifier |
| transaction_id | VARCHAR(4) | YES | CICS transaction ID |
| event_type | ENUM | NO | TRANSACTION, USER_ACTION, SYSTEM_EVENT |
| action | ENUM | NO | CREATE, UPDATE, DELETE, INQUIRE, etc. |
| status | ENUM | NO | SUCCESS, FAILURE, WARNING |
| portfolio_id | VARCHAR(8) | YES | Related portfolio |
| account_no | VARCHAR(10) | YES | Related account |
| resource_name | VARCHAR(50) | YES | Accessed resource |
| access_type | VARCHAR(8) | YES | Access type |
| before_image | TEXT | YES | Data before change |
| after_image | TEXT | YES | Data after change |
| message | TEXT | YES | Additional message |
| ip_address | INET | YES | Client IP address |
| user_agent | TEXT | YES | Client user agent |

**Source COBOL Structure** (AUDITLOG.cpy):
```cobol
01 AUDIT-RECORD.
   05 AUD-TIMESTAMP          PIC X(26).
   05 AUD-SYSTEM-ID          PIC X(8).
   05 AUD-USER-ID            PIC X(8).
   05 AUD-PROGRAM            PIC X(8).
   05 AUD-TERMINAL           PIC X(8).
   05 AUD-TYPE               PIC X(4).
   05 AUD-ACTION             PIC X(8).
   05 AUD-STATUS             PIC X(4).
   ...
```

### Batch Processing Tables

#### `batch_control`

Batch control records, migrated from BCHCTL VSAM file.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| process_date | DATE | NO | Processing date |
| process_id | VARCHAR(8) | NO | Process identifier |
| status | VARCHAR(1) | NO | Status (W/R/C/F) |
| start_time | TIMESTAMP | YES | Start timestamp |
| end_time | TIMESTAMP | YES | End timestamp |
| record_count | INTEGER | YES | Records processed |
| error_count | INTEGER | YES | Error count |
| last_position | INTEGER | YES | Last checkpoint position |
| return_code | INTEGER | YES | Return code |
| message | VARCHAR(50) | YES | Status message |

#### `checkpoints`

Checkpoint/restart records for batch processing.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| process_date | DATE | NO | Processing date |
| process_id | VARCHAR(8) | NO | Process identifier |
| last_transaction_id | VARCHAR(12) | YES | Last processed transaction |
| last_account | VARCHAR(9) | YES | Last processed account |
| last_fund | VARCHAR(6) | YES | Last processed fund |
| records_processed | INTEGER | YES | Records processed count |
| checkpoint_timestamp | TIMESTAMP | NO | Checkpoint timestamp |

## Indexes

All tables have appropriate indexes for common query patterns:

- Primary key indexes (automatic)
- Foreign key indexes
- Business key indexes (portfolio_id, user_id, etc.)
- Date range indexes for time-based queries
- Status indexes for filtering

## Views

### `active_portfolios`
Active portfolios with no close date or future close date.

### `current_positions`
Latest position for each portfolio/investment combination.

### `daily_transaction_summary`
Aggregated transaction statistics by date and type.

### `error_summary`
Error counts by date, program, and severity for the last 30 days.

## Data Type Mappings

| COBOL Type | PostgreSQL Type |
|------------|-----------------|
| PIC X(n) | VARCHAR(n) |
| PIC 9(n) | INTEGER or BIGINT |
| PIC S9(n)V9(m) COMP-3 | DECIMAL(n+m, m) |
| PIC S9(n) COMP | INTEGER or BIGINT |
| DATE | DATE |
| TIME | TIME |
| TIMESTAMP | TIMESTAMP WITH TIME ZONE |

## Value Transformations

### Status Codes
| COBOL | PostgreSQL |
|-------|------------|
| A | ACTIVE |
| C | CLOSED |
| P | PENDING |
| S | SUSPENDED |

### Transaction Types
| COBOL | PostgreSQL |
|-------|------------|
| BU | BUY |
| SL | SELL |
| TR | TRANSFER |
| FE | FEE |

### Transaction Status
| COBOL | PostgreSQL |
|-------|------------|
| P | PENDING |
| D | COMPLETED |
| F | FAILED |
| R | REVERSED |
