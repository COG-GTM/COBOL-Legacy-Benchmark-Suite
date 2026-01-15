--=====================================================================
-- Transaction History Table Definition (Phase 1 - VSAM Migration)
-- Migrates Transaction History VSAM file to relational database
-- Version: 1.0
-- Date: 2024
--=====================================================================

--====================================================================
-- TRANSACTION_HISTORY TABLE
-- Source: VSAM TRANHIST (Transaction History VSAM)
-- Maps to COBOL copybook: TRNREC.cpy
--====================================================================

CREATE TABLESPACE TRNHIST
  IN POSMVP
  USING STOGROUP POSMVPSG
  PRIQTY 14400
  SECQTY 2880
  SEGSIZE 64
  COMPRESS YES
  PARTITION BY RANGE(TRANSACTION_DATE)
  (PARTITION 1 ENDING AT ('2024-03-31'),
   PARTITION 2 ENDING AT ('2024-06-30'),
   PARTITION 3 ENDING AT ('2024-09-30'),
   PARTITION 4 ENDING AT ('2024-12-31'),
   PARTITION 5 ENDING AT ('2025-03-31'),
   PARTITION 6 ENDING AT ('2025-06-30'),
   PARTITION 7 ENDING AT ('2025-09-30'),
   PARTITION 8 ENDING AT ('2025-12-31'));

CREATE TABLE TRANSACTION_HISTORY (
    -- Primary Key (composite from TRNREC key structure)
    -- TRN-KEY: TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO
    TRANSACTION_ID    VARCHAR(30)     NOT NULL,
    
    -- Portfolio Reference
    PORTFOLIO_ID      VARCHAR(20)     NOT NULL,
    
    -- Transaction Date/Time (from TRN-DATE: X(08), TRN-TIME: X(06))
    TRANSACTION_DATE  DATE            NOT NULL,
    TRANSACTION_TIME  TIME            NOT NULL,
    
    -- Transaction Details (from TRN-DATA in TRNREC.cpy)
    -- TRN-TYPE: X(02) - BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    TRANSACTION_TYPE  VARCHAR(10)     NOT NULL,
    
    -- Investment Reference (from TRN-INVESTMENT-ID: X(10))
    INVESTMENT_ID     VARCHAR(10)     NOT NULL,
    
    -- Financial Values
    -- TRN-AMOUNT: S9(13)V9(2) COMP-3 -> DECIMAL(15,2)
    AMOUNT            DECIMAL(15,2)   NOT NULL,
    
    -- TRN-QUANTITY: S9(11)V9(4) COMP-3 -> DECIMAL(15,4)
    UNITS             DECIMAL(15,4),
    
    -- TRN-PRICE: S9(11)V9(4) COMP-3 -> DECIMAL(15,4)
    PRICE             DECIMAL(15,4),
    
    -- Currency (from TRN-CURRENCY: X(03))
    CURRENCY_CODE     CHAR(3)         NOT NULL DEFAULT 'USD',
    
    -- Status (from TRN-STATUS: X(01))
    -- P=Pending, D=Done, F=Failed, R=Reversed
    STATUS            CHAR(1)         NOT NULL DEFAULT 'P',
    
    -- Sequence Number (from TRN-SEQUENCE-NO: X(06))
    SEQUENCE_NO       VARCHAR(6),
    
    -- Audit Fields (from TRN-AUDIT in TRNREC.cpy)
    PROCESS_DATE      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PROCESS_USER      VARCHAR(8)      NOT NULL,
    
    -- Migration Tracking
    VSAM_MIGRATION_DATE TIMESTAMP,
    VSAM_RECORD_KEY   VARCHAR(28),
    
    -- Constraints
    CONSTRAINT PK_TRANSACTION_HISTORY 
        PRIMARY KEY (TRANSACTION_ID)
) IN POSMVP.TRNHIST;

--====================================================================
-- INDEXES
--====================================================================

-- Primary Index (clustered by date for partition pruning)
CREATE UNIQUE INDEX TRNHIST_PK
  ON TRANSACTION_HISTORY
  (TRANSACTION_ID ASC)
  CLUSTER;

-- Portfolio and date lookup index
CREATE INDEX TRNHIST_IX1
  ON TRANSACTION_HISTORY
  (PORTFOLIO_ID ASC,
   TRANSACTION_DATE DESC);

-- Date range queries index
CREATE INDEX TRNHIST_IX2
  ON TRANSACTION_HISTORY
  (TRANSACTION_DATE ASC,
   TRANSACTION_TYPE ASC);

-- Investment lookup index
CREATE INDEX TRNHIST_IX3
  ON TRANSACTION_HISTORY
  (INVESTMENT_ID ASC,
   TRANSACTION_DATE DESC);

-- Status monitoring index
CREATE INDEX TRNHIST_IX4
  ON TRANSACTION_HISTORY
  (STATUS ASC,
   PROCESS_DATE DESC);

--====================================================================
-- FOREIGN KEY CONSTRAINTS
--====================================================================

ALTER TABLE TRANSACTION_HISTORY
  ADD CONSTRAINT FK_TRNHIST_PORTFOLIO
  FOREIGN KEY (PORTFOLIO_ID) 
  REFERENCES POSITION_MASTER(PORTFOLIO_ID);

--====================================================================
-- TABLE COMMENTS
--====================================================================

COMMENT ON TABLE TRANSACTION_HISTORY IS
  'Transaction History Table - Migrated from VSAM TRANHIST. Contains historical transaction records.';

COMMENT ON COLUMN TRANSACTION_HISTORY.TRANSACTION_ID IS
  'Unique transaction identifier (generated from TRN-KEY components)';
COMMENT ON COLUMN TRANSACTION_HISTORY.PORTFOLIO_ID IS
  'Portfolio identifier (from TRN-PORTFOLIO-ID)';
COMMENT ON COLUMN TRANSACTION_HISTORY.TRANSACTION_DATE IS
  'Transaction date (from TRN-DATE YYYYMMDD)';
COMMENT ON COLUMN TRANSACTION_HISTORY.TRANSACTION_TIME IS
  'Transaction time (from TRN-TIME HHMMSS)';
COMMENT ON COLUMN TRANSACTION_HISTORY.TRANSACTION_TYPE IS
  'Transaction type: BUY, SELL, TRANSFER, FEE (from TRN-TYPE)';
COMMENT ON COLUMN TRANSACTION_HISTORY.AMOUNT IS
  'Transaction amount (from TRN-AMOUNT)';
COMMENT ON COLUMN TRANSACTION_HISTORY.UNITS IS
  'Number of units (from TRN-QUANTITY)';
COMMENT ON COLUMN TRANSACTION_HISTORY.PRICE IS
  'Unit price (from TRN-PRICE)';
COMMENT ON COLUMN TRANSACTION_HISTORY.STATUS IS
  'Transaction status: P=Pending, D=Done, F=Failed, R=Reversed (from TRN-STATUS)';
COMMENT ON COLUMN TRANSACTION_HISTORY.VSAM_MIGRATION_DATE IS
  'Timestamp when record was migrated from VSAM';
COMMENT ON COLUMN TRANSACTION_HISTORY.VSAM_RECORD_KEY IS
  'Original VSAM record key for audit trail';

--====================================================================
-- GRANTS
--====================================================================

GRANT SELECT, INSERT, UPDATE ON TRANSACTION_HISTORY TO POSAPP;
GRANT SELECT ON TRANSACTION_HISTORY TO POSRPT;

--====================================================================
-- CHECK CONSTRAINTS
--====================================================================

ALTER TABLE TRANSACTION_HISTORY
  ADD CONSTRAINT CHK_TRNHIST_TYPE
  CHECK (TRANSACTION_TYPE IN ('BUY', 'SELL', 'TRANSFER', 'FEE', 'BU', 'SL', 'TR', 'FE'));

ALTER TABLE TRANSACTION_HISTORY
  ADD CONSTRAINT CHK_TRNHIST_STATUS
  CHECK (STATUS IN ('P', 'D', 'F', 'R'));

ALTER TABLE TRANSACTION_HISTORY
  ADD CONSTRAINT CHK_TRNHIST_AMOUNT
  CHECK (AMOUNT IS NOT NULL);

ALTER TABLE TRANSACTION_HISTORY
  ADD CONSTRAINT CHK_TRNHIST_CURRENCY
  CHECK (LENGTH(TRIM(CURRENCY_CODE)) = 3);

--====================================================================
-- STORED PROCEDURES
--====================================================================

-- Procedure to generate transaction ID from COBOL key components
CREATE PROCEDURE GENERATE_TRANSACTION_ID
  (IN P_DATE DATE,
   IN P_TIME TIME,
   IN P_PORTFOLIO_ID VARCHAR(20),
   IN P_SEQUENCE_NO VARCHAR(6),
   OUT P_TRANSACTION_ID VARCHAR(30))
  LANGUAGE SQL
BEGIN
  SET P_TRANSACTION_ID = 
    REPLACE(CHAR(P_DATE, ISO), '-', '') ||
    REPLACE(CHAR(P_TIME, ISO), '.', '') ||
    TRIM(P_PORTFOLIO_ID) ||
    COALESCE(P_SEQUENCE_NO, '000001');
END;

-- Procedure to archive old transactions
CREATE PROCEDURE ARCHIVE_TRANSACTIONS
  (IN RETENTION_DAYS INTEGER)
  LANGUAGE SQL
BEGIN
  -- Archive transactions older than retention period
  INSERT INTO TRANSACTION_HISTORY_ARCHIVE
  SELECT * FROM TRANSACTION_HISTORY
  WHERE TRANSACTION_DATE < CURRENT DATE - RETENTION_DAYS DAYS
    AND STATUS = 'D';
    
  DELETE FROM TRANSACTION_HISTORY
  WHERE TRANSACTION_DATE < CURRENT DATE - RETENTION_DAYS DAYS
    AND STATUS = 'D';
END;

--====================================================================
-- NOTES:
--====================================================================
-- 1. TRANSACTION_ID format: YYYYMMDDHHMMSS + PORTFOLIO_ID + SEQUENCE_NO
-- 2. Partitioned by TRANSACTION_DATE for efficient date range queries
-- 3. DECIMAL precision matches COBOL COMP-3 field definitions
-- 4. Transaction types expanded from 2-char codes to readable values
-- 5. Status codes preserved from original COBOL 88-level conditions
-- 6. Indexes optimized for common query patterns from INQHIST program
--====================================================================
