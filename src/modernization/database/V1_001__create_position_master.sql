--=====================================================================
-- Position Master Table Definition (Phase 1 - VSAM Migration)
-- Migrates Position Master VSAM file to relational database
-- Version: 1.0
-- Date: 2024
--=====================================================================

--====================================================================
-- POSITION_MASTER TABLE
-- Source: VSAM POSFILE (Position Master VSAM)
-- Maps to COBOL copybook: POSREC.cpy
--====================================================================

CREATE TABLESPACE POSMSTR
  IN POSMVP
  USING STOGROUP POSMVPSG
  PRIQTY 7200
  SECQTY 1440
  SEGSIZE 64
  COMPRESS YES;

CREATE TABLE POSITION_MASTER (
    -- Primary Key (composite from POSREC key structure)
    PORTFOLIO_ID      VARCHAR(20)     NOT NULL,
    
    -- Account Reference
    ACCOUNT_NUMBER    VARCHAR(15)     NOT NULL,
    
    -- Investment Details
    FUND_ID           VARCHAR(10)     NOT NULL,
    
    -- Position Quantities (from POS-DATA in POSREC.cpy)
    -- POS-QUANTITY: S9(11)V9(4) COMP-3 -> DECIMAL(15,4)
    UNITS             DECIMAL(15,4),
    
    -- Financial Values
    -- POS-COST-BASIS: S9(13)V9(2) COMP-3 -> DECIMAL(15,2)
    COST_BASIS        DECIMAL(15,2),
    
    -- POS-MARKET-VALUE: S9(13)V9(2) COMP-3 -> DECIMAL(15,2)
    MARKET_VALUE      DECIMAL(15,2),
    
    -- Currency (from POS-CURRENCY: X(03))
    CURRENCY_CODE     CHAR(3)         NOT NULL DEFAULT 'USD',
    
    -- Status (from POS-STATUS: X(01))
    -- A=Active, C=Closed, P=Pending
    STATUS            CHAR(1)         NOT NULL DEFAULT 'A',
    
    -- Position Date (from POS-DATE: X(08) YYYYMMDD)
    POSITION_DATE     DATE            NOT NULL,
    
    -- Audit Fields (from POS-AUDIT in POSREC.cpy)
    LAST_UPDATE       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LAST_MAINT_USER   VARCHAR(8)      NOT NULL,
    
    -- Migration Tracking
    VSAM_MIGRATION_DATE TIMESTAMP,
    VSAM_RECORD_KEY   VARCHAR(26),
    
    -- Constraints
    CONSTRAINT PK_POSITION_MASTER 
        PRIMARY KEY (PORTFOLIO_ID)
) IN POSMVP.POSMSTR;

--====================================================================
-- INDEXES
--====================================================================

-- Primary Index (clustered)
CREATE UNIQUE INDEX POSMSTR_PK
  ON POSITION_MASTER
  (PORTFOLIO_ID ASC)
  CLUSTER;

-- Account lookup index
CREATE INDEX POSMSTR_IX1
  ON POSITION_MASTER
  (ACCOUNT_NUMBER ASC,
   STATUS ASC);

-- Fund and date lookup index
CREATE INDEX POSMSTR_IX2
  ON POSITION_MASTER
  (FUND_ID ASC,
   LAST_UPDATE DESC);

-- Market value analysis index
CREATE INDEX POSMSTR_IX3
  ON POSITION_MASTER
  (MARKET_VALUE DESC,
   STATUS ASC);

--====================================================================
-- FOREIGN KEY CONSTRAINTS
-- Note: ACCOUNTS table reference - to be created if not exists
--====================================================================

-- Uncomment when ACCOUNTS table is available:
-- ALTER TABLE POSITION_MASTER
--   ADD CONSTRAINT FK_POSMSTR_ACCOUNT
--   FOREIGN KEY (ACCOUNT_NUMBER) 
--   REFERENCES ACCOUNTS(ACCOUNT_NUMBER);

--====================================================================
-- TABLE COMMENTS
--====================================================================

COMMENT ON TABLE POSITION_MASTER IS
  'Position Master Table - Migrated from VSAM POSFILE. Contains primary portfolio position data.';

COMMENT ON COLUMN POSITION_MASTER.PORTFOLIO_ID IS
  'Unique portfolio identifier (from POS-PORTFOLIO-ID)';
COMMENT ON COLUMN POSITION_MASTER.ACCOUNT_NUMBER IS
  'Associated account number';
COMMENT ON COLUMN POSITION_MASTER.FUND_ID IS
  'Investment/Fund identifier (from POS-INVESTMENT-ID)';
COMMENT ON COLUMN POSITION_MASTER.UNITS IS
  'Number of units held (from POS-QUANTITY)';
COMMENT ON COLUMN POSITION_MASTER.COST_BASIS IS
  'Total cost basis amount (from POS-COST-BASIS)';
COMMENT ON COLUMN POSITION_MASTER.MARKET_VALUE IS
  'Current market value (from POS-MARKET-VALUE)';
COMMENT ON COLUMN POSITION_MASTER.STATUS IS
  'Position status: A=Active, C=Closed, P=Pending (from POS-STATUS)';
COMMENT ON COLUMN POSITION_MASTER.VSAM_MIGRATION_DATE IS
  'Timestamp when record was migrated from VSAM';
COMMENT ON COLUMN POSITION_MASTER.VSAM_RECORD_KEY IS
  'Original VSAM record key for audit trail';

--====================================================================
-- GRANTS
--====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON POSITION_MASTER TO POSAPP;
GRANT SELECT ON POSITION_MASTER TO POSRPT;

--====================================================================
-- CHECK CONSTRAINTS
--====================================================================

ALTER TABLE POSITION_MASTER
  ADD CONSTRAINT CHK_POSMSTR_STATUS
  CHECK (STATUS IN ('A', 'C', 'P'));

ALTER TABLE POSITION_MASTER
  ADD CONSTRAINT CHK_POSMSTR_UNITS
  CHECK (UNITS IS NULL OR UNITS >= 0);

ALTER TABLE POSITION_MASTER
  ADD CONSTRAINT CHK_POSMSTR_CURRENCY
  CHECK (LENGTH(TRIM(CURRENCY_CODE)) = 3);

--====================================================================
-- NOTES:
--====================================================================
-- 1. PORTFOLIO_ID is VARCHAR(20) to accommodate future expansion
-- 2. DECIMAL precision matches COBOL COMP-3 field definitions
-- 3. VSAM_MIGRATION_DATE and VSAM_RECORD_KEY track migration lineage
-- 4. Status codes preserved from original COBOL 88-level conditions
-- 5. Indexes optimized for common query patterns from online programs
--====================================================================
