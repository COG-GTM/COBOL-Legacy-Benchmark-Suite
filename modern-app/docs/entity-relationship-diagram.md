# Entity-Relationship Diagram (ERD)

## Overview

This document presents the Entity-Relationship Diagram for the modernized Portfolio Management System database schema. The schema replaces the legacy VSAM files and DB2 tables with a normalized PostgreSQL database design.

## ERD Diagram (Mermaid)

```mermaid
erDiagram
    PORTFOLIO ||--o{ POSITION : contains
    PORTFOLIO ||--o{ TRANSACTION : has
    PORTFOLIO ||--o{ AUDIT_LOG : tracks

    PORTFOLIO {
        varchar(8) portfolio_id PK
        varchar(10) account_number
        varchar(10) client_id
        varchar(30) client_name
        char(1) client_type
        varchar(50) portfolio_name
        char(3) currency_code
        char(1) risk_level
        char(1) status
        decimal(15_2) total_value
        decimal(15_2) cash_balance
        date create_date
        date close_date
        date last_transaction_date
        varchar(8) last_maintenance_user
        timestamp created_at
        timestamp updated_at
    }

    POSITION {
        varchar(8) portfolio_id PK_FK
        date position_date PK
        varchar(10) investment_id PK
        decimal(15_4) quantity
        decimal(15_2) cost_basis
        decimal(15_2) market_value
        char(3) currency_code
        char(1) status
        varchar(8) last_maintenance_user
        timestamp created_at
        timestamp updated_at
    }

    TRANSACTION {
        varchar(28) transaction_id PK
        varchar(8) portfolio_id FK
        date transaction_date
        time transaction_time
        varchar(6) sequence_number
        varchar(10) investment_id
        char(2) transaction_type
        decimal(15_4) quantity
        decimal(15_4) price
        decimal(15_2) amount
        char(3) currency_code
        char(1) status
        varchar(8) process_user
        timestamp process_timestamp
    }

    POSITION_HISTORY {
        varchar(8) account_number PK
        varchar(10) portfolio_id PK
        date transaction_date PK
        time transaction_time PK
        char(2) transaction_type
        varchar(12) security_id
        decimal(15_3) quantity
        decimal(15_3) price
        decimal(15_2) amount
        decimal(15_2) fees
        decimal(15_2) total_amount
        decimal(15_2) cost_basis
        decimal(15_2) gain_loss
        date process_date
        time process_time
        varchar(8) program_id
        varchar(8) user_id
        timestamp audit_timestamp
    }

    ERROR_LOG {
        timestamp error_timestamp PK
        varchar(8) program_id PK
        char(1) error_type
        integer error_severity
        varchar(8) error_code
        varchar(200) error_message
        date process_date
        time process_time
        varchar(8) user_id
        varchar(500) additional_info
    }

    AUDIT_LOG {
        bigserial audit_id PK
        timestamp timestamp
        varchar(8) system_id
        varchar(8) user_id
        varchar(8) program_id
        varchar(8) terminal_id
        varchar(4) audit_type
        varchar(8) action_type
        varchar(4) status
        varchar(8) portfolio_id FK
        varchar(10) account_number
        varchar(100) before_image
        varchar(100) after_image
        varchar(100) message
    }

    BATCH_CONTROL {
        varchar(8) job_name PK
        varchar(8) process_date PK
        integer sequence_number PK
        char(1) status
        varchar(8) step_name
        varchar(8) program_name
        time start_time
        time end_time
        integer return_code
        varchar(80) error_description
        integer restart_count
        timestamp attempt_timestamp
        timestamp complete_timestamp
    }

    RETURN_CODES {
        timestamp timestamp PK
        varchar(8) program_id PK
        integer return_code
        integer highest_code
        char(1) status_code
        varchar(80) message_text
    }
```

## Table Descriptions

### Core Business Tables

#### PORTFOLIO
The central entity representing a client's investment portfolio. This table replaces the VSAM PORTMSTR file.

**Key Relationships:**
- One-to-Many with POSITION (a portfolio has multiple positions)
- One-to-Many with TRANSACTION (a portfolio has multiple transactions)
- One-to-Many with AUDIT_LOG (portfolio changes are tracked)

**Business Rules:**
- Portfolio ID is unique and immutable
- Status transitions: Active → Suspended → Closed
- Client types: Individual (I), Corporate (C), Trust (T)

#### POSITION
Represents investment holdings within a portfolio at a specific point in time. This table replaces the VSAM POSHIST file structure.

**Key Relationships:**
- Many-to-One with PORTFOLIO (foreign key constraint)

**Business Rules:**
- Composite primary key ensures unique position per portfolio/date/investment
- Status: Active (A), Closed (C), Pending (P)
- Quantity and values must be non-negative

#### TRANSACTION
Records all financial transactions affecting portfolios. This table replaces the VSAM TRANHIST file.

**Key Relationships:**
- Many-to-One with PORTFOLIO (foreign key constraint)

**Business Rules:**
- Transaction types: Buy (BU), Sell (SL), Transfer (TR), Fee (FE)
- Status: Pending (P), Done (D), Failed (F), Reversed (R)
- Transaction ID format: YYYYMMDDHHMMSS + sequence

### Historical and Audit Tables

#### POSITION_HISTORY
Detailed transaction history for reporting and compliance. This table replaces the DB2 POSHIST table.

**Purpose:**
- Maintains complete audit trail of position changes
- Supports regulatory reporting requirements
- Enables historical analysis and reconciliation

#### AUDIT_LOG
Comprehensive audit trail for all system activities. This table is based on the COBOL AUDITLOG copybook.

**Audit Types:**
- TRAN: Transaction-related events
- USER: User actions (login, logout, inquiries)
- SYST: System events (startup, shutdown)

#### ERROR_LOG
Application error logging for monitoring and troubleshooting. This table replaces the DB2 ERRLOG table.

**Severity Levels:**
- 1: Informational
- 2: Warning
- 3: Error
- 4: Severe/Critical

### Operational Tables

#### BATCH_CONTROL
Job control and checkpoint/restart support for batch processing. This table replaces the VSAM BCHCTL file.

**Status Values:**
- R: Ready
- A: Active
- W: Waiting
- D: Done
- E: Error

#### RETURN_CODES
Tracks program execution return codes for monitoring and analysis. This table replaces the DB2 RTNCODES table.

## Data Type Mappings

| COBOL Type | PostgreSQL Type | Notes |
|------------|-----------------|-------|
| PIC X(n) | VARCHAR(n) | Character fields |
| PIC 9(n) | INTEGER/BIGINT | Numeric fields |
| PIC S9(n)V9(m) COMP-3 | DECIMAL(n+m, m) | Packed decimal |
| PIC 9(8) (date) | DATE | YYYYMMDD format |
| PIC 9(6) (time) | TIME | HHMMSS format |
| TIMESTAMP | TIMESTAMP | Direct mapping |

## Index Strategy

### Primary Indexes
All tables have primary key indexes automatically created by PostgreSQL.

### Secondary Indexes

| Table | Index Name | Columns | Purpose |
|-------|------------|---------|---------|
| portfolio | idx_portfolio_client | client_id, status | Client lookups |
| portfolio | idx_portfolio_account | account_number | Account lookups |
| position | idx_position_date | position_date, portfolio_id | Date range queries |
| position | idx_position_investment | investment_id | Investment lookups |
| transaction | idx_transaction_portfolio | portfolio_id, transaction_date | Portfolio history |
| transaction | idx_transaction_date | transaction_date, portfolio_id | Date range queries |
| audit_log | idx_audit_portfolio | portfolio_id | Portfolio audit trail |
| audit_log | idx_audit_user | user_id, timestamp | User activity |
| error_log | idx_errlog_process | process_date, error_severity | Error analysis |

## Constraints

### Foreign Key Constraints
- position.portfolio_id → portfolio.portfolio_id
- transaction.portfolio_id → portfolio.portfolio_id
- audit_log.portfolio_id → portfolio.portfolio_id (nullable)

### Check Constraints
- portfolio.client_type IN ('I', 'C', 'T')
- portfolio.status IN ('A', 'C', 'S')
- position.status IN ('A', 'C', 'P')
- transaction.transaction_type IN ('BU', 'SL', 'TR', 'FE')
- transaction.status IN ('P', 'D', 'F', 'R')
- error_log.error_type IN ('S', 'A', 'D')
- error_log.error_severity BETWEEN 1 AND 4

## Migration Mapping Summary

| Legacy Source | Target Table | Migration Notes |
|---------------|--------------|-----------------|
| VSAM PORTMSTR | portfolio | Field normalization, date conversion |
| VSAM POSHIST | position | Composite key restructure |
| VSAM TRANHIST | transaction | Key format change |
| DB2 POSHIST | position_history | Direct migration |
| DB2 ERRLOG | error_log | Schema alignment |
| DB2 RTNCODES | return_codes | Direct migration |
| COBOL AUDITLOG | audit_log | New table structure |
| VSAM BCHCTL | batch_control | Checkpoint support |
