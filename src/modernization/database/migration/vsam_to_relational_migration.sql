--=====================================================================
-- VSAM to Relational Database Migration Script
-- Phase 1 - Foundation and Data Migration
-- Version: 1.0
-- Date: 2024
--=====================================================================
-- This script provides the SQL statements for migrating data from
-- VSAM files to the new relational database tables.
--
-- Source VSAM Files:
-- - PORTMSTR (Portfolio Master) -> POSITION_MASTER
-- - TRANHIST (Transaction History) -> TRANSACTION_HISTORY
-- - POSHIST (Position History) -> HISTORY_LOG
--
-- Prerequisites:
-- 1. Target tables must be created (V1_001, V1_002 scripts)
-- 2. VSAM data must be exported to staging tables
-- 3. Data validation framework should be run post-migration
--=====================================================================

--====================================================================
-- STAGING TABLE DEFINITIONS
-- These tables receive the raw VSAM export data before transformation
--====================================================================

-- Staging table for VSAM Position Master (PORTMSTR/POSFILE)
CREATE TABLE STG_VSAM_POSITION (
    VSAM_RECORD_KEY     VARCHAR(26)     NOT NULL,
    POS_PORTFOLIO_ID    CHAR(8)         NOT NULL,
    POS_DATE            CHAR(8)         NOT NULL,
    POS_INVESTMENT_ID   CHAR(10)        NOT NULL,
    POS_QUANTITY        DECIMAL(15,4),
    POS_COST_BASIS      DECIMAL(15,2),
    POS_MARKET_VALUE    DECIMAL(15,2),
    POS_CURRENCY        CHAR(3),
    POS_STATUS          CHAR(1),
    POS_LAST_MAINT_DATE CHAR(26),
    POS_LAST_MAINT_USER CHAR(8),
    POS_FILLER          VARCHAR(50),
    LOAD_TIMESTAMP      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    LOAD_STATUS         CHAR(1)         DEFAULT 'P',
    ERROR_MESSAGE       VARCHAR(500),
    PRIMARY KEY (VSAM_RECORD_KEY)
);

-- Staging table for VSAM Transaction History (TRANHIST)
CREATE TABLE STG_VSAM_TRANSACTION (
    VSAM_RECORD_KEY     VARCHAR(28)     NOT NULL,
    TRN_DATE            CHAR(8)         NOT NULL,
    TRN_TIME            CHAR(6)         NOT NULL,
    TRN_PORTFOLIO_ID    CHAR(8)         NOT NULL,
    TRN_SEQUENCE_NO     CHAR(6)         NOT NULL,
    TRN_INVESTMENT_ID   CHAR(10),
    TRN_TYPE            CHAR(2),
    TRN_QUANTITY        DECIMAL(15,4),
    TRN_PRICE           DECIMAL(15,4),
    TRN_AMOUNT          DECIMAL(15,2),
    TRN_CURRENCY        CHAR(3),
    TRN_STATUS          CHAR(1),
    TRN_PROCESS_DATE    CHAR(26),
    TRN_PROCESS_USER    CHAR(8),
    TRN_FILLER          VARCHAR(50),
    LOAD_TIMESTAMP      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    LOAD_STATUS         CHAR(1)         DEFAULT 'P',
    ERROR_MESSAGE       VARCHAR(500),
    PRIMARY KEY (VSAM_RECORD_KEY)
);

-- Staging table for VSAM Position History (POSHIST)
CREATE TABLE STG_VSAM_HISTORY (
    VSAM_RECORD_KEY     VARCHAR(26)     NOT NULL,
    HIST_PORTFOLIO_ID   CHAR(8)         NOT NULL,
    HIST_DATE           CHAR(8)         NOT NULL,
    HIST_TIME           CHAR(6)         NOT NULL,
    HIST_SEQ_NO         CHAR(4)         NOT NULL,
    HIST_RECORD_TYPE    CHAR(2),
    HIST_ACTION_CODE    CHAR(1),
    HIST_BEFORE_IMAGE   VARCHAR(400),
    HIST_AFTER_IMAGE    VARCHAR(400),
    HIST_REASON_CODE    CHAR(4),
    HIST_PROCESS_DATE   CHAR(26),
    HIST_PROCESS_USER   CHAR(8),
    HIST_FILLER         VARCHAR(50),
    LOAD_TIMESTAMP      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    LOAD_STATUS         CHAR(1)         DEFAULT 'P',
    ERROR_MESSAGE       VARCHAR(500),
    PRIMARY KEY (VSAM_RECORD_KEY)
);

--====================================================================
-- DATA TRANSFORMATION AND MIGRATION PROCEDURES
--====================================================================

-- Procedure to migrate Position Master data
CREATE PROCEDURE MIGRATE_POSITIONS()
LANGUAGE SQL
BEGIN
    DECLARE V_MIGRATED_COUNT INTEGER DEFAULT 0;
    DECLARE V_ERROR_COUNT INTEGER DEFAULT 0;
    DECLARE V_START_TIME TIMESTAMP;
    
    SET V_START_TIME = CURRENT_TIMESTAMP;
    
    -- Insert transformed data into POSITION_MASTER
    INSERT INTO POSITION_MASTER (
        PORTFOLIO_ID,
        ACCOUNT_NUMBER,
        FUND_ID,
        UNITS,
        COST_BASIS,
        MARKET_VALUE,
        CURRENCY_CODE,
        STATUS,
        POSITION_DATE,
        LAST_UPDATE,
        LAST_MAINT_USER,
        VSAM_MIGRATION_DATE,
        VSAM_RECORD_KEY
    )
    SELECT 
        -- Generate unique portfolio ID from VSAM key components
        TRIM(POS_PORTFOLIO_ID) || '-' || TRIM(POS_INVESTMENT_ID),
        -- Extract account number from portfolio ID (first 8 chars)
        TRIM(POS_PORTFOLIO_ID),
        -- Fund ID from investment ID
        TRIM(POS_INVESTMENT_ID),
        -- Numeric fields direct mapping
        POS_QUANTITY,
        POS_COST_BASIS,
        POS_MARKET_VALUE,
        -- Currency with default
        COALESCE(NULLIF(TRIM(POS_CURRENCY), ''), 'USD'),
        -- Status with default
        COALESCE(NULLIF(TRIM(POS_STATUS), ''), 'A'),
        -- Convert YYYYMMDD to DATE
        DATE(SUBSTR(POS_DATE, 1, 4) || '-' || 
             SUBSTR(POS_DATE, 5, 2) || '-' || 
             SUBSTR(POS_DATE, 7, 2)),
        -- Convert timestamp string to TIMESTAMP
        CASE 
            WHEN LENGTH(TRIM(POS_LAST_MAINT_DATE)) >= 19 
            THEN TIMESTAMP(POS_LAST_MAINT_DATE)
            ELSE CURRENT_TIMESTAMP
        END,
        -- User with default
        COALESCE(NULLIF(TRIM(POS_LAST_MAINT_USER), ''), 'MIGRATION'),
        -- Migration tracking
        CURRENT_TIMESTAMP,
        VSAM_RECORD_KEY
    FROM STG_VSAM_POSITION
    WHERE LOAD_STATUS = 'P';
    
    -- Get count of migrated records
    GET DIAGNOSTICS V_MIGRATED_COUNT = ROW_COUNT;
    
    -- Update staging table status for successful migrations
    UPDATE STG_VSAM_POSITION
    SET LOAD_STATUS = 'C'
    WHERE LOAD_STATUS = 'P'
      AND VSAM_RECORD_KEY IN (
          SELECT VSAM_RECORD_KEY FROM POSITION_MASTER
          WHERE VSAM_MIGRATION_DATE >= V_START_TIME
      );
    
    -- Log migration results
    INSERT INTO MIGRATION_LOG (
        MIGRATION_TYPE, SOURCE_TABLE, TARGET_TABLE,
        RECORDS_PROCESSED, RECORDS_MIGRATED, RECORDS_FAILED,
        START_TIME, END_TIME, STATUS
    ) VALUES (
        'VSAM_TO_RELATIONAL', 'STG_VSAM_POSITION', 'POSITION_MASTER',
        (SELECT COUNT(*) FROM STG_VSAM_POSITION),
        V_MIGRATED_COUNT, V_ERROR_COUNT,
        V_START_TIME, CURRENT_TIMESTAMP, 'COMPLETED'
    );
END;

-- Procedure to migrate Transaction History data
CREATE PROCEDURE MIGRATE_TRANSACTIONS()
LANGUAGE SQL
BEGIN
    DECLARE V_MIGRATED_COUNT INTEGER DEFAULT 0;
    DECLARE V_ERROR_COUNT INTEGER DEFAULT 0;
    DECLARE V_START_TIME TIMESTAMP;
    
    SET V_START_TIME = CURRENT_TIMESTAMP;
    
    -- Insert transformed data into TRANSACTION_HISTORY
    INSERT INTO TRANSACTION_HISTORY (
        TRANSACTION_ID,
        PORTFOLIO_ID,
        TRANSACTION_DATE,
        TRANSACTION_TIME,
        TRANSACTION_TYPE,
        INVESTMENT_ID,
        AMOUNT,
        UNITS,
        PRICE,
        CURRENCY_CODE,
        STATUS,
        SEQUENCE_NO,
        PROCESS_DATE,
        PROCESS_USER,
        VSAM_MIGRATION_DATE,
        VSAM_RECORD_KEY
    )
    SELECT 
        -- Generate transaction ID from VSAM key components
        TRN_DATE || TRN_TIME || TRIM(TRN_PORTFOLIO_ID) || TRN_SEQUENCE_NO,
        -- Portfolio ID reference
        TRIM(TRN_PORTFOLIO_ID),
        -- Convert YYYYMMDD to DATE
        DATE(SUBSTR(TRN_DATE, 1, 4) || '-' || 
             SUBSTR(TRN_DATE, 5, 2) || '-' || 
             SUBSTR(TRN_DATE, 7, 2)),
        -- Convert HHMMSS to TIME
        TIME(SUBSTR(TRN_TIME, 1, 2) || ':' || 
             SUBSTR(TRN_TIME, 3, 2) || ':' || 
             SUBSTR(TRN_TIME, 5, 2)),
        -- Convert 2-char type to full name
        CASE TRIM(TRN_TYPE)
            WHEN 'BU' THEN 'BUY'
            WHEN 'SL' THEN 'SELL'
            WHEN 'TR' THEN 'TRANSFER'
            WHEN 'FE' THEN 'FEE'
            ELSE TRIM(TRN_TYPE)
        END,
        -- Investment ID
        TRIM(TRN_INVESTMENT_ID),
        -- Numeric fields
        TRN_AMOUNT,
        TRN_QUANTITY,
        TRN_PRICE,
        -- Currency with default
        COALESCE(NULLIF(TRIM(TRN_CURRENCY), ''), 'USD'),
        -- Status with default
        COALESCE(NULLIF(TRIM(TRN_STATUS), ''), 'D'),
        -- Sequence number
        TRN_SEQUENCE_NO,
        -- Convert timestamp string to TIMESTAMP
        CASE 
            WHEN LENGTH(TRIM(TRN_PROCESS_DATE)) >= 19 
            THEN TIMESTAMP(TRN_PROCESS_DATE)
            ELSE CURRENT_TIMESTAMP
        END,
        -- User with default
        COALESCE(NULLIF(TRIM(TRN_PROCESS_USER), ''), 'MIGRATION'),
        -- Migration tracking
        CURRENT_TIMESTAMP,
        VSAM_RECORD_KEY
    FROM STG_VSAM_TRANSACTION
    WHERE LOAD_STATUS = 'P';
    
    -- Get count of migrated records
    GET DIAGNOSTICS V_MIGRATED_COUNT = ROW_COUNT;
    
    -- Update staging table status
    UPDATE STG_VSAM_TRANSACTION
    SET LOAD_STATUS = 'C'
    WHERE LOAD_STATUS = 'P'
      AND VSAM_RECORD_KEY IN (
          SELECT VSAM_RECORD_KEY FROM TRANSACTION_HISTORY
          WHERE VSAM_MIGRATION_DATE >= V_START_TIME
      );
    
    -- Log migration results
    INSERT INTO MIGRATION_LOG (
        MIGRATION_TYPE, SOURCE_TABLE, TARGET_TABLE,
        RECORDS_PROCESSED, RECORDS_MIGRATED, RECORDS_FAILED,
        START_TIME, END_TIME, STATUS
    ) VALUES (
        'VSAM_TO_RELATIONAL', 'STG_VSAM_TRANSACTION', 'TRANSACTION_HISTORY',
        (SELECT COUNT(*) FROM STG_VSAM_TRANSACTION),
        V_MIGRATED_COUNT, V_ERROR_COUNT,
        V_START_TIME, CURRENT_TIMESTAMP, 'COMPLETED'
    );
END;

-- Procedure to migrate History data
CREATE PROCEDURE MIGRATE_HISTORY()
LANGUAGE SQL
BEGIN
    DECLARE V_MIGRATED_COUNT INTEGER DEFAULT 0;
    DECLARE V_START_TIME TIMESTAMP;
    
    SET V_START_TIME = CURRENT_TIMESTAMP;
    
    -- Insert transformed data into HISTORY_LOG
    INSERT INTO HISTORY_LOG (
        PORTFOLIO_ID,
        HISTORY_DATE,
        HISTORY_TIME,
        SEQUENCE_NO,
        RECORD_TYPE,
        ACTION_CODE,
        BEFORE_IMAGE,
        AFTER_IMAGE,
        REASON_CODE,
        PROCESS_DATE,
        PROCESS_USER,
        VSAM_MIGRATION_DATE,
        VSAM_RECORD_KEY
    )
    SELECT 
        TRIM(HIST_PORTFOLIO_ID),
        -- Convert YYYYMMDD to DATE
        DATE(SUBSTR(HIST_DATE, 1, 4) || '-' || 
             SUBSTR(HIST_DATE, 5, 2) || '-' || 
             SUBSTR(HIST_DATE, 7, 2)),
        -- Convert HHMMSS to TIME
        TIME(SUBSTR(HIST_TIME, 1, 2) || ':' || 
             SUBSTR(HIST_TIME, 3, 2) || ':' || 
             SUBSTR(HIST_TIME, 5, 2)),
        HIST_SEQ_NO,
        HIST_RECORD_TYPE,
        HIST_ACTION_CODE,
        HIST_BEFORE_IMAGE,
        HIST_AFTER_IMAGE,
        TRIM(HIST_REASON_CODE),
        -- Convert timestamp string to TIMESTAMP
        CASE 
            WHEN LENGTH(TRIM(HIST_PROCESS_DATE)) >= 19 
            THEN TIMESTAMP(HIST_PROCESS_DATE)
            ELSE CURRENT_TIMESTAMP
        END,
        COALESCE(NULLIF(TRIM(HIST_PROCESS_USER), ''), 'MIGRATION'),
        CURRENT_TIMESTAMP,
        VSAM_RECORD_KEY
    FROM STG_VSAM_HISTORY
    WHERE LOAD_STATUS = 'P';
    
    GET DIAGNOSTICS V_MIGRATED_COUNT = ROW_COUNT;
    
    -- Update staging table status
    UPDATE STG_VSAM_HISTORY
    SET LOAD_STATUS = 'C'
    WHERE LOAD_STATUS = 'P';
    
    -- Log migration results
    INSERT INTO MIGRATION_LOG (
        MIGRATION_TYPE, SOURCE_TABLE, TARGET_TABLE,
        RECORDS_PROCESSED, RECORDS_MIGRATED, RECORDS_FAILED,
        START_TIME, END_TIME, STATUS
    ) VALUES (
        'VSAM_TO_RELATIONAL', 'STG_VSAM_HISTORY', 'HISTORY_LOG',
        (SELECT COUNT(*) FROM STG_VSAM_HISTORY),
        V_MIGRATED_COUNT, 0,
        V_START_TIME, CURRENT_TIMESTAMP, 'COMPLETED'
    );
END;

--====================================================================
-- MIGRATION LOG TABLE
--====================================================================

CREATE TABLE MIGRATION_LOG (
    LOG_ID              INTEGER         NOT NULL GENERATED ALWAYS AS IDENTITY,
    MIGRATION_TYPE      VARCHAR(50)     NOT NULL,
    SOURCE_TABLE        VARCHAR(50)     NOT NULL,
    TARGET_TABLE        VARCHAR(50)     NOT NULL,
    RECORDS_PROCESSED   INTEGER         DEFAULT 0,
    RECORDS_MIGRATED    INTEGER         DEFAULT 0,
    RECORDS_FAILED      INTEGER         DEFAULT 0,
    START_TIME          TIMESTAMP       NOT NULL,
    END_TIME            TIMESTAMP,
    STATUS              VARCHAR(20)     DEFAULT 'IN_PROGRESS',
    ERROR_DETAILS       VARCHAR(2000),
    PRIMARY KEY (LOG_ID)
);

--====================================================================
-- VALIDATION QUERIES
--====================================================================

-- Query to validate position migration counts
-- SELECT 
--     'POSITION_MASTER' AS TABLE_NAME,
--     (SELECT COUNT(*) FROM STG_VSAM_POSITION) AS SOURCE_COUNT,
--     (SELECT COUNT(*) FROM POSITION_MASTER WHERE VSAM_MIGRATION_DATE IS NOT NULL) AS TARGET_COUNT,
--     (SELECT COUNT(*) FROM STG_VSAM_POSITION WHERE LOAD_STATUS = 'E') AS ERROR_COUNT;

-- Query to validate transaction migration counts
-- SELECT 
--     'TRANSACTION_HISTORY' AS TABLE_NAME,
--     (SELECT COUNT(*) FROM STG_VSAM_TRANSACTION) AS SOURCE_COUNT,
--     (SELECT COUNT(*) FROM TRANSACTION_HISTORY WHERE VSAM_MIGRATION_DATE IS NOT NULL) AS TARGET_COUNT,
--     (SELECT COUNT(*) FROM STG_VSAM_TRANSACTION WHERE LOAD_STATUS = 'E') AS ERROR_COUNT;

-- Query to validate data integrity
-- SELECT 
--     p.PORTFOLIO_ID,
--     p.UNITS AS POSITION_UNITS,
--     SUM(CASE WHEN t.TRANSACTION_TYPE IN ('BUY', 'BU') THEN t.UNITS ELSE 0 END) -
--     SUM(CASE WHEN t.TRANSACTION_TYPE IN ('SELL', 'SL') THEN t.UNITS ELSE 0 END) AS CALCULATED_UNITS,
--     p.UNITS - (SUM(CASE WHEN t.TRANSACTION_TYPE IN ('BUY', 'BU') THEN t.UNITS ELSE 0 END) -
--                SUM(CASE WHEN t.TRANSACTION_TYPE IN ('SELL', 'SL') THEN t.UNITS ELSE 0 END)) AS VARIANCE
-- FROM POSITION_MASTER p
-- LEFT JOIN TRANSACTION_HISTORY t ON p.PORTFOLIO_ID = t.PORTFOLIO_ID AND t.STATUS = 'D'
-- GROUP BY p.PORTFOLIO_ID, p.UNITS
-- HAVING ABS(p.UNITS - (SUM(CASE WHEN t.TRANSACTION_TYPE IN ('BUY', 'BU') THEN t.UNITS ELSE 0 END) -
--                       SUM(CASE WHEN t.TRANSACTION_TYPE IN ('SELL', 'SL') THEN t.UNITS ELSE 0 END))) > 0.0001;

--====================================================================
-- ROLLBACK PROCEDURES
--====================================================================

-- Procedure to rollback position migration
CREATE PROCEDURE ROLLBACK_POSITION_MIGRATION(IN P_MIGRATION_DATE TIMESTAMP)
LANGUAGE SQL
BEGIN
    -- Delete migrated records
    DELETE FROM POSITION_MASTER
    WHERE VSAM_MIGRATION_DATE >= P_MIGRATION_DATE;
    
    -- Reset staging table status
    UPDATE STG_VSAM_POSITION
    SET LOAD_STATUS = 'P'
    WHERE LOAD_STATUS = 'C';
    
    -- Log rollback
    INSERT INTO MIGRATION_LOG (
        MIGRATION_TYPE, SOURCE_TABLE, TARGET_TABLE,
        START_TIME, END_TIME, STATUS
    ) VALUES (
        'ROLLBACK', 'POSITION_MASTER', 'STG_VSAM_POSITION',
        P_MIGRATION_DATE, CURRENT_TIMESTAMP, 'ROLLED_BACK'
    );
END;

-- Procedure to rollback transaction migration
CREATE PROCEDURE ROLLBACK_TRANSACTION_MIGRATION(IN P_MIGRATION_DATE TIMESTAMP)
LANGUAGE SQL
BEGIN
    DELETE FROM TRANSACTION_HISTORY
    WHERE VSAM_MIGRATION_DATE >= P_MIGRATION_DATE;
    
    UPDATE STG_VSAM_TRANSACTION
    SET LOAD_STATUS = 'P'
    WHERE LOAD_STATUS = 'C';
    
    INSERT INTO MIGRATION_LOG (
        MIGRATION_TYPE, SOURCE_TABLE, TARGET_TABLE,
        START_TIME, END_TIME, STATUS
    ) VALUES (
        'ROLLBACK', 'TRANSACTION_HISTORY', 'STG_VSAM_TRANSACTION',
        P_MIGRATION_DATE, CURRENT_TIMESTAMP, 'ROLLED_BACK'
    );
END;

--====================================================================
-- GRANTS
--====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON STG_VSAM_POSITION TO POSAPP;
GRANT SELECT, INSERT, UPDATE, DELETE ON STG_VSAM_TRANSACTION TO POSAPP;
GRANT SELECT, INSERT, UPDATE, DELETE ON STG_VSAM_HISTORY TO POSAPP;
GRANT SELECT, INSERT ON MIGRATION_LOG TO POSAPP;
GRANT SELECT ON MIGRATION_LOG TO POSRPT;
GRANT EXECUTE ON PROCEDURE MIGRATE_POSITIONS TO POSAPP;
GRANT EXECUTE ON PROCEDURE MIGRATE_TRANSACTIONS TO POSAPP;
GRANT EXECUTE ON PROCEDURE MIGRATE_HISTORY TO POSAPP;
GRANT EXECUTE ON PROCEDURE ROLLBACK_POSITION_MIGRATION TO POSAPP;
GRANT EXECUTE ON PROCEDURE ROLLBACK_TRANSACTION_MIGRATION TO POSAPP;

--====================================================================
-- NOTES:
--====================================================================
-- 1. VSAM data must be exported using IDCAMS REPRO or equivalent
-- 2. Staging tables receive raw COBOL field formats
-- 3. Migration procedures handle data type conversions
-- 4. LOAD_STATUS: P=Pending, C=Completed, E=Error
-- 5. VSAM_MIGRATION_DATE tracks migration lineage
-- 6. Rollback procedures allow reverting failed migrations
-- 7. Run validation queries after migration to verify integrity
--====================================================================
