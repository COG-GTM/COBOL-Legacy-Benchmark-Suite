# Data Validation Procedures

## Overview

This document defines the validation procedures to ensure data integrity during and after the migration from legacy mainframe systems (VSAM files and DB2) to the modern PostgreSQL database. These procedures are critical for verifying that all data has been accurately migrated without loss or corruption.

## Validation Framework

### Validation Levels

1. **Pre-Migration Validation**: Verify source data quality before extraction
2. **In-Flight Validation**: Monitor data during transformation and loading
3. **Post-Migration Validation**: Comprehensive verification after migration completion
4. **Ongoing Validation**: Continuous data quality monitoring

## Pre-Migration Validation

### Source Data Quality Checks

#### VSAM File Validation

```sql
-- Pseudo-code for COBOL validation program
-- VALIDATE-PORTMSTR-FILE
PERFORM VARYING WS-RECORD-COUNT FROM 1 BY 1
    UNTIL END-OF-FILE
    READ PORTMSTR-FILE INTO PORT-RECORD
    
    -- Validate Portfolio ID format
    IF PORT-ID NOT NUMERIC
        ADD 1 TO WS-ERROR-COUNT
        WRITE ERROR-RECORD
    END-IF
    
    -- Validate Status code
    IF PORT-STATUS NOT = 'A' AND 'C' AND 'S'
        ADD 1 TO WS-ERROR-COUNT
    END-IF
    
    -- Validate Date format
    IF PORT-CREATE-DATE NOT NUMERIC
       OR PORT-CREATE-DATE < 19000101
       OR PORT-CREATE-DATE > 20991231
        ADD 1 TO WS-ERROR-COUNT
    END-IF
END-PERFORM
```

#### DB2 Table Validation

```sql
-- Validate POSHIST table before migration
SELECT 
    COUNT(*) as total_records,
    COUNT(DISTINCT ACCOUNT_NO || PORTFOLIO_ID) as unique_accounts,
    MIN(TRANS_DATE) as earliest_date,
    MAX(TRANS_DATE) as latest_date,
    SUM(CASE WHEN QUANTITY < 0 THEN 1 ELSE 0 END) as negative_quantities,
    SUM(CASE WHEN AMOUNT IS NULL THEN 1 ELSE 0 END) as null_amounts
FROM POSHIST;

-- Check for orphan records
SELECT COUNT(*) as orphan_count
FROM POSHIST ph
WHERE NOT EXISTS (
    SELECT 1 FROM PORTFOLIO_MASTER pm 
    WHERE pm.PORTFOLIO_ID = ph.PORTFOLIO_ID
);
```

## In-Flight Validation

### Transformation Validation

#### Record Count Tracking

```python
# Python script for tracking record counts during ETL
import logging

class MigrationValidator:
    def __init__(self):
        self.source_counts = {}
        self.target_counts = {}
        self.error_counts = {}
    
    def validate_batch(self, table_name, source_count, target_count, errors):
        """Validate each batch during migration"""
        self.source_counts[table_name] = self.source_counts.get(table_name, 0) + source_count
        self.target_counts[table_name] = self.target_counts.get(table_name, 0) + target_count
        self.error_counts[table_name] = self.error_counts.get(table_name, 0) + errors
        
        if source_count != target_count + errors:
            logging.error(f"Count mismatch in {table_name}: "
                         f"source={source_count}, target={target_count}, errors={errors}")
            return False
        return True
    
    def generate_report(self):
        """Generate validation report"""
        report = []
        for table in self.source_counts:
            report.append({
                'table': table,
                'source_count': self.source_counts[table],
                'target_count': self.target_counts[table],
                'error_count': self.error_counts[table],
                'success_rate': (self.target_counts[table] / self.source_counts[table]) * 100
            })
        return report
```

#### Checksum Validation

```sql
-- Generate checksum for source data (DB2)
SELECT 
    SUM(CAST(ACCOUNT_NO AS BIGINT)) as account_checksum,
    SUM(QUANTITY * 1000000) as quantity_checksum,
    SUM(AMOUNT * 100) as amount_checksum
FROM POSHIST
WHERE TRANS_DATE BETWEEN '2024-01-01' AND '2024-12-31';

-- Verify checksum in target (PostgreSQL)
SELECT 
    SUM(CAST(account_number AS BIGINT)) as account_checksum,
    SUM(quantity * 1000000)::BIGINT as quantity_checksum,
    SUM(amount * 100)::BIGINT as amount_checksum
FROM position_history
WHERE transaction_date BETWEEN '2024-01-01' AND '2024-12-31';
```

## Post-Migration Validation

### 1. Record Count Validation

```sql
-- PostgreSQL validation queries

-- Portfolio count validation
SELECT 
    'portfolio' as table_name,
    COUNT(*) as record_count,
    COUNT(DISTINCT portfolio_id) as unique_keys
FROM portfolio;

-- Position count validation
SELECT 
    'position' as table_name,
    COUNT(*) as record_count,
    COUNT(DISTINCT portfolio_id || position_date::text || investment_id) as unique_keys
FROM position;

-- Transaction count validation
SELECT 
    'transaction' as table_name,
    COUNT(*) as record_count,
    COUNT(DISTINCT transaction_id) as unique_keys
FROM transaction;

-- Position history count validation
SELECT 
    'position_history' as table_name,
    COUNT(*) as record_count
FROM position_history;

-- Error log count validation
SELECT 
    'error_log' as table_name,
    COUNT(*) as record_count
FROM error_log;
```

### 2. Data Integrity Validation

```sql
-- Foreign key integrity check
SELECT 
    'position_orphans' as check_name,
    COUNT(*) as orphan_count
FROM position p
WHERE NOT EXISTS (
    SELECT 1 FROM portfolio pf 
    WHERE pf.portfolio_id = p.portfolio_id
);

SELECT 
    'transaction_orphans' as check_name,
    COUNT(*) as orphan_count
FROM transaction t
WHERE NOT EXISTS (
    SELECT 1 FROM portfolio pf 
    WHERE pf.portfolio_id = t.portfolio_id
);

-- Referential integrity summary
SELECT 
    'audit_log_orphans' as check_name,
    COUNT(*) as orphan_count
FROM audit_log al
WHERE al.portfolio_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM portfolio pf 
    WHERE pf.portfolio_id = al.portfolio_id
);
```

### 3. Business Rule Validation

```sql
-- Validate portfolio status rules
SELECT 
    'invalid_portfolio_status' as check_name,
    COUNT(*) as violation_count
FROM portfolio
WHERE status NOT IN ('A', 'C', 'S');

-- Validate transaction types
SELECT 
    'invalid_transaction_type' as check_name,
    COUNT(*) as violation_count
FROM transaction
WHERE transaction_type NOT IN ('BU', 'SL', 'TR', 'FE');

-- Validate position quantities (should not be negative for active positions)
SELECT 
    'negative_active_positions' as check_name,
    COUNT(*) as violation_count
FROM position
WHERE status = 'A' AND quantity < 0;

-- Validate date consistency
SELECT 
    'future_transaction_dates' as check_name,
    COUNT(*) as violation_count
FROM transaction
WHERE transaction_date > CURRENT_DATE;

-- Validate closed portfolios have close dates
SELECT 
    'closed_without_date' as check_name,
    COUNT(*) as violation_count
FROM portfolio
WHERE status = 'C' AND close_date IS NULL;
```

### 4. Numeric Precision Validation

```sql
-- Validate decimal precision was preserved
SELECT 
    'amount_precision_check' as check_name,
    COUNT(*) as records_checked,
    SUM(CASE WHEN amount != ROUND(amount, 2) THEN 1 ELSE 0 END) as precision_errors
FROM transaction;

SELECT 
    'quantity_precision_check' as check_name,
    COUNT(*) as records_checked,
    SUM(CASE WHEN quantity != ROUND(quantity, 4) THEN 1 ELSE 0 END) as precision_errors
FROM position;
```

### 5. Sample Data Comparison

```sql
-- Random sample validation (1% of records)
WITH sample_portfolios AS (
    SELECT portfolio_id
    FROM portfolio
    ORDER BY RANDOM()
    LIMIT (SELECT COUNT(*) / 100 FROM portfolio)
)
SELECT 
    p.portfolio_id,
    p.account_number,
    p.client_name,
    p.total_value,
    p.status
FROM portfolio p
JOIN sample_portfolios sp ON p.portfolio_id = sp.portfolio_id;

-- Export for manual comparison with source
\COPY (
    SELECT * FROM portfolio 
    WHERE portfolio_id IN (SELECT portfolio_id FROM sample_portfolios)
) TO '/tmp/sample_portfolios.csv' WITH CSV HEADER;
```

## Validation Scripts

### Comprehensive Validation Script

```sql
-- Create validation results table
CREATE TABLE IF NOT EXISTS migration_validation_results (
    validation_id SERIAL PRIMARY KEY,
    validation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    check_name VARCHAR(100) NOT NULL,
    check_category VARCHAR(50) NOT NULL,
    expected_value BIGINT,
    actual_value BIGINT,
    status VARCHAR(10) NOT NULL,
    details TEXT
);

-- Run all validations and store results
DO $$
DECLARE
    v_count BIGINT;
    v_expected BIGINT;
BEGIN
    -- Record count validations
    SELECT COUNT(*) INTO v_count FROM portfolio;
    INSERT INTO migration_validation_results 
        (check_name, check_category, actual_value, status)
    VALUES ('portfolio_count', 'record_count', v_count, 
            CASE WHEN v_count > 0 THEN 'PASS' ELSE 'FAIL' END);
    
    SELECT COUNT(*) INTO v_count FROM position;
    INSERT INTO migration_validation_results 
        (check_name, check_category, actual_value, status)
    VALUES ('position_count', 'record_count', v_count,
            CASE WHEN v_count > 0 THEN 'PASS' ELSE 'FAIL' END);
    
    SELECT COUNT(*) INTO v_count FROM transaction;
    INSERT INTO migration_validation_results 
        (check_name, check_category, actual_value, status)
    VALUES ('transaction_count', 'record_count', v_count,
            CASE WHEN v_count > 0 THEN 'PASS' ELSE 'FAIL' END);
    
    -- Orphan record validations
    SELECT COUNT(*) INTO v_count FROM position p
    WHERE NOT EXISTS (SELECT 1 FROM portfolio pf WHERE pf.portfolio_id = p.portfolio_id);
    INSERT INTO migration_validation_results 
        (check_name, check_category, expected_value, actual_value, status)
    VALUES ('position_orphans', 'integrity', 0, v_count,
            CASE WHEN v_count = 0 THEN 'PASS' ELSE 'FAIL' END);
    
    SELECT COUNT(*) INTO v_count FROM transaction t
    WHERE NOT EXISTS (SELECT 1 FROM portfolio pf WHERE pf.portfolio_id = t.portfolio_id);
    INSERT INTO migration_validation_results 
        (check_name, check_category, expected_value, actual_value, status)
    VALUES ('transaction_orphans', 'integrity', 0, v_count,
            CASE WHEN v_count = 0 THEN 'PASS' ELSE 'FAIL' END);
    
    -- Business rule validations
    SELECT COUNT(*) INTO v_count FROM portfolio WHERE status NOT IN ('A', 'C', 'S');
    INSERT INTO migration_validation_results 
        (check_name, check_category, expected_value, actual_value, status)
    VALUES ('invalid_portfolio_status', 'business_rule', 0, v_count,
            CASE WHEN v_count = 0 THEN 'PASS' ELSE 'FAIL' END);
    
    SELECT COUNT(*) INTO v_count FROM transaction WHERE transaction_type NOT IN ('BU', 'SL', 'TR', 'FE');
    INSERT INTO migration_validation_results 
        (check_name, check_category, expected_value, actual_value, status)
    VALUES ('invalid_transaction_type', 'business_rule', 0, v_count,
            CASE WHEN v_count = 0 THEN 'PASS' ELSE 'FAIL' END);
    
END $$;

-- Generate validation report
SELECT 
    check_category,
    check_name,
    expected_value,
    actual_value,
    status,
    validation_date
FROM migration_validation_results
ORDER BY 
    CASE status WHEN 'FAIL' THEN 0 ELSE 1 END,
    check_category,
    check_name;
```

### Validation Summary Report

```sql
-- Generate summary report
SELECT 
    check_category,
    COUNT(*) as total_checks,
    SUM(CASE WHEN status = 'PASS' THEN 1 ELSE 0 END) as passed,
    SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END) as failed,
    ROUND(
        SUM(CASE WHEN status = 'PASS' THEN 1 ELSE 0 END)::NUMERIC / 
        COUNT(*)::NUMERIC * 100, 2
    ) as pass_rate
FROM migration_validation_results
GROUP BY check_category
ORDER BY check_category;
```

## Automated Validation Pipeline

### Java Validation Service

```java
package com.portfolio.modernization.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataValidationService {

    private final JdbcTemplate jdbcTemplate;

    public List<ValidationResult> runAllValidations() {
        List<ValidationResult> results = new ArrayList<>();
        
        // Record count validations
        results.add(validateRecordCount("portfolio"));
        results.add(validateRecordCount("position"));
        results.add(validateRecordCount("transaction"));
        results.add(validateRecordCount("position_history"));
        
        // Integrity validations
        results.add(validateOrphanRecords("position", "portfolio", "portfolio_id"));
        results.add(validateOrphanRecords("transaction", "portfolio", "portfolio_id"));
        
        // Business rule validations
        results.add(validateStatusCodes());
        results.add(validateTransactionTypes());
        
        return results;
    }

    private ValidationResult validateRecordCount(String tableName) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName, Long.class);
        
        return ValidationResult.builder()
            .checkName(tableName + "_count")
            .category("record_count")
            .actualValue(count)
            .status(count > 0 ? "PASS" : "WARN")
            .build();
    }

    private ValidationResult validateOrphanRecords(String childTable, 
                                                    String parentTable, 
                                                    String keyColumn) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s c WHERE NOT EXISTS " +
            "(SELECT 1 FROM %s p WHERE p.%s = c.%s)",
            childTable, parentTable, keyColumn, keyColumn);
        
        Long orphanCount = jdbcTemplate.queryForObject(sql, Long.class);
        
        return ValidationResult.builder()
            .checkName(childTable + "_orphans")
            .category("integrity")
            .expectedValue(0L)
            .actualValue(orphanCount)
            .status(orphanCount == 0 ? "PASS" : "FAIL")
            .build();
    }

    private ValidationResult validateStatusCodes() {
        Long invalidCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM portfolio WHERE status NOT IN ('A', 'C', 'S')",
            Long.class);
        
        return ValidationResult.builder()
            .checkName("invalid_portfolio_status")
            .category("business_rule")
            .expectedValue(0L)
            .actualValue(invalidCount)
            .status(invalidCount == 0 ? "PASS" : "FAIL")
            .build();
    }

    private ValidationResult validateTransactionTypes() {
        Long invalidCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM transaction WHERE transaction_type NOT IN ('BU', 'SL', 'TR', 'FE')",
            Long.class);
        
        return ValidationResult.builder()
            .checkName("invalid_transaction_type")
            .category("business_rule")
            .expectedValue(0L)
            .actualValue(invalidCount)
            .status(invalidCount == 0 ? "PASS" : "FAIL")
            .build();
    }
}
```

## Acceptance Criteria

### Migration Success Criteria

| Criteria | Threshold | Action if Failed |
|----------|-----------|------------------|
| Record count match | 100% | Investigate missing records |
| Orphan records | 0 | Fix foreign key relationships |
| Invalid status codes | 0 | Correct data transformation |
| Checksum match | 100% | Re-run transformation |
| Sample comparison | 99.9% | Manual review of discrepancies |

### Sign-off Requirements

1. All validation checks pass
2. Record counts match source systems
3. No orphan records detected
4. Business rules validated
5. Sample data manually verified
6. Performance benchmarks met
7. Stakeholder approval obtained

## Monitoring and Alerting

### Ongoing Data Quality Monitoring

```sql
-- Create scheduled job for ongoing validation
CREATE OR REPLACE FUNCTION run_daily_validation()
RETURNS void AS $$
BEGIN
    -- Check for data anomalies
    INSERT INTO migration_validation_results 
        (check_name, check_category, actual_value, status, details)
    SELECT 
        'daily_transaction_count',
        'monitoring',
        COUNT(*),
        CASE WHEN COUNT(*) > 0 THEN 'PASS' ELSE 'WARN' END,
        'Transactions processed today'
    FROM transaction
    WHERE transaction_date = CURRENT_DATE;
    
    -- Check for error spikes
    INSERT INTO migration_validation_results 
        (check_name, check_category, actual_value, status, details)
    SELECT 
        'daily_error_count',
        'monitoring',
        COUNT(*),
        CASE WHEN COUNT(*) < 100 THEN 'PASS' ELSE 'WARN' END,
        'Errors logged today'
    FROM error_log
    WHERE process_date = CURRENT_DATE;
END;
$$ LANGUAGE plpgsql;
```

## Appendix: Validation Checklist

- [ ] Pre-migration source data quality verified
- [ ] Record counts documented for all source tables
- [ ] Checksums calculated for numeric fields
- [ ] Migration scripts tested in non-production environment
- [ ] Post-migration record counts match source
- [ ] Foreign key integrity verified
- [ ] Business rules validated
- [ ] Sample data manually compared
- [ ] Performance benchmarks met
- [ ] Validation report generated and reviewed
- [ ] Stakeholder sign-off obtained
