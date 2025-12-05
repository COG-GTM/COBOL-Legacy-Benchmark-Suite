# Database Migration Plan

## Overview

This document outlines the strategy for migrating data from the legacy mainframe storage systems (VSAM files and DB2 for z/OS) to the modern PostgreSQL database. The migration is a critical component of the COBOL Legacy Benchmark Suite modernization effort.

## Current State Analysis

### VSAM Files

The legacy system uses three primary VSAM files:

| File Name | Type | Record Length | Key Structure | Description |
|-----------|------|---------------|---------------|-------------|
| PORTMSTR | KSDS | 400 bytes | Portfolio ID (8) + Account Type (2) + Branch ID (2) | Portfolio master data |
| TRANHIST | KSDS | 300 bytes | Trans Date (8) + Trans Time (6) + Portfolio ID (8) + Seq No (6) | Transaction history |
| POSHIST | KSDS | 350 bytes | Portfolio ID (8) + Position Date (8) + Investment ID (10) | Position history |

### DB2 Tables

The legacy system uses the following DB2 tables:

| Table Name | Primary Key | Description |
|------------|-------------|-------------|
| POSHIST | ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME | Position history with transaction details |
| ERRLOG | ERROR_TIMESTAMP, PROGRAM_ID | Error logging |
| RTNCODES | TIMESTAMP, PROGRAM_ID | Return code tracking |

## Target State

### PostgreSQL Schema

The new PostgreSQL database will consolidate and normalize the data structures:

| New Table | Source | Key Changes |
|-----------|--------|-------------|
| portfolio | VSAM PORTMSTR | Normalized structure, added audit columns |
| position | VSAM POSHIST | Composite key, foreign key to portfolio |
| transaction | VSAM TRANHIST | Normalized, added status tracking |
| position_history | DB2 POSHIST | Direct migration with schema updates |
| error_log | DB2 ERRLOG | Added cleanup function |
| audit_log | COBOL AUDITLOG | New table for comprehensive audit trail |
| batch_control | VSAM BCHCTL | Job control and checkpoint support |
| return_codes | DB2 RTNCODES | Direct migration |

## Migration Approach

### Phase 1: Schema Creation (Week 1)

1. Deploy Flyway migrations to create target schema
2. Validate schema structure matches requirements
3. Create indexes and constraints
4. Set up database monitoring

### Phase 2: Data Extraction (Week 2)

1. **VSAM Data Extraction**
   - Use IDCAMS REPRO to export VSAM data to sequential files
   - Convert EBCDIC to ASCII encoding
   - Handle packed decimal (COMP-3) field conversions
   - Generate CSV files for each VSAM file

2. **DB2 Data Extraction**
   - Use DB2 UNLOAD utility to export table data
   - Generate CSV files with proper delimiters
   - Include header rows for validation

### Phase 3: Data Transformation (Week 3)

1. **Field Mapping**

   **Portfolio (PORTMSTR → portfolio)**
   | COBOL Field | PostgreSQL Column | Transformation |
   |-------------|-------------------|----------------|
   | PORT-ID | portfolio_id | Direct mapping |
   | PORT-ACCOUNT-NO | account_number | Direct mapping |
   | PORT-CLIENT-NAME | client_name | Trim spaces |
   | PORT-CLIENT-TYPE | client_type | Direct mapping (I/C/T) |
   | PORT-CREATE-DATE | create_date | YYYYMMDD → DATE |
   | PORT-TOTAL-VALUE | total_value | COMP-3 → DECIMAL |
   | PORT-CASH-BALANCE | cash_balance | COMP-3 → DECIMAL |
   | PORT-STATUS | status | Direct mapping (A/C/S) |

   **Transaction (TRANHIST → transaction)**
   | COBOL Field | PostgreSQL Column | Transformation |
   |-------------|-------------------|----------------|
   | TRN-DATE | transaction_date | YYYYMMDD → DATE |
   | TRN-TIME | transaction_time | HHMMSS → TIME |
   | TRN-PORTFOLIO-ID | portfolio_id | Direct mapping |
   | TRN-SEQUENCE-NO | sequence_number | Direct mapping |
   | TRN-INVESTMENT-ID | investment_id | Direct mapping |
   | TRN-TYPE | transaction_type | Direct mapping (BU/SL/TR/FE) |
   | TRN-QUANTITY | quantity | COMP-3 → DECIMAL |
   | TRN-PRICE | price | COMP-3 → DECIMAL |
   | TRN-AMOUNT | amount | COMP-3 → DECIMAL |
   | TRN-STATUS | status | Direct mapping (P/D/F/R) |

2. **Data Cleansing**
   - Remove trailing spaces from character fields
   - Validate date formats
   - Handle null/empty values
   - Validate numeric ranges

### Phase 4: Data Loading (Week 4)

1. **Load Sequence**
   - Load portfolio table first (parent table)
   - Load position table (references portfolio)
   - Load transaction table (references portfolio)
   - Load position_history table
   - Load error_log table
   - Load audit_log table
   - Load batch_control table
   - Load return_codes table

2. **Loading Strategy**
   - Use PostgreSQL COPY command for bulk loading
   - Disable indexes during load, rebuild after
   - Use batch commits (every 10,000 records)
   - Log progress and errors

### Phase 5: Validation (Week 5)

1. **Record Count Validation**
   - Compare source and target record counts
   - Validate counts by key segments

2. **Data Integrity Validation**
   - Verify foreign key relationships
   - Validate numeric totals (sum of amounts, quantities)
   - Check date ranges

3. **Sample Data Validation**
   - Random sample comparison (1% of records)
   - Full comparison of critical records

## Migration Scripts

### VSAM to CSV Extraction (JCL)

```jcl
//EXTRACT  JOB (ACCT),'VSAM EXTRACT',CLASS=A
//STEP1    EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.MASTER.FILE,DISP=SHR
//OUTFILE  DD DSN=PORTFOLIO.EXTRACT.CSV,
//            DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(100,20)),
//            DCB=(RECFM=VB,LRECL=500)
//SYSIN    DD *
  REPRO INFILE(INFILE) OUTFILE(OUTFILE)
/*
```

### PostgreSQL Load Script

```sql
-- Disable triggers and indexes for faster loading
ALTER TABLE portfolio DISABLE TRIGGER ALL;

-- Load data using COPY
COPY portfolio (
    portfolio_id, account_number, client_id, client_name,
    client_type, portfolio_name, currency_code, risk_level,
    status, total_value, cash_balance, create_date,
    close_date, last_transaction_date, last_maintenance_user
)
FROM '/data/portfolio_extract.csv'
WITH (FORMAT csv, HEADER true, NULL '');

-- Re-enable triggers
ALTER TABLE portfolio ENABLE TRIGGER ALL;

-- Rebuild indexes
REINDEX TABLE portfolio;

-- Update statistics
ANALYZE portfolio;
```

## Rollback Plan

In case of migration failure:

1. **Immediate Rollback**
   - Drop all migrated tables
   - Restore from pre-migration backup
   - Revert application to legacy system

2. **Partial Rollback**
   - Identify failed tables
   - Truncate and reload specific tables
   - Validate data integrity

## Timeline

| Phase | Duration | Start | End |
|-------|----------|-------|-----|
| Schema Creation | 1 week | Week 1 | Week 1 |
| Data Extraction | 1 week | Week 2 | Week 2 |
| Data Transformation | 1 week | Week 3 | Week 3 |
| Data Loading | 1 week | Week 4 | Week 4 |
| Validation | 1 week | Week 5 | Week 5 |
| **Total** | **5 weeks** | | |

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Data loss during extraction | Multiple extraction runs, checksum validation |
| Character encoding issues | Test EBCDIC→ASCII conversion with sample data |
| Performance issues during load | Use parallel loading, optimize batch sizes |
| Foreign key violations | Load in correct sequence, validate relationships |
| Downtime impact | Schedule migration during maintenance window |

## Success Criteria

1. All records migrated with 100% accuracy
2. Data integrity validated across all tables
3. Application functionality verified with migrated data
4. Performance benchmarks met (query response times)
5. No data loss or corruption detected
