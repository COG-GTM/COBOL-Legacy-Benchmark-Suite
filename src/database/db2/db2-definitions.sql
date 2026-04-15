--********************************************************************
-- DB2 TABLE DEFINITIONS FOR INVESTMENT PORTFOLIO MANAGEMENT SYSTEM
-- VERSION: 1.0
-- DATE: 2024
--
-- Master DDL script defining all core tables, indexes, and views
-- for the portfolio management system. Tables use referential
-- integrity (foreign keys) to enforce portfolio-to-position and
-- portfolio-to-transaction relationships.
--
-- Table hierarchy:
--   PORTFOLIO_MASTER   -> INVESTMENT_POSITIONS (1:N by PORTFOLIO_ID)
--   PORTFOLIO_MASTER   -> TRANSACTION_HISTORY  (1:N by PORTFOLIO_ID)
--
-- See also: POSHIST.sql (partitioned history), ERRLOG.sql (errors),
--           RTNCODES.sql (return codes), PORTPLAN.sql (DB2 plan)
--********************************************************************

--====================================================================
-- PORTFOLIO MASTER TABLE
-- Primary entity: one row per portfolio.
-- Key: PORTFOLIO_ID (8-char, e.g., 'PORT0001')
-- Status: A=Active, C=Closed, S=Suspended
-- Risk: 1=Low, 2=Medium, 3=High, 4=Aggressive
--====================================================================
CREATE TABLE PORTFOLIO_MASTER (
    PORTFOLIO_ID      CHAR(8)         NOT NULL,
    ACCOUNT_TYPE      CHAR(2)         NOT NULL,
    BRANCH_ID         CHAR(2)         NOT NULL,
    CLIENT_ID         CHAR(10)        NOT NULL,
    PORTFOLIO_NAME    VARCHAR(50)     NOT NULL,
    CURRENCY_CODE     CHAR(3)         NOT NULL,
    RISK_LEVEL        CHAR(1)         NOT NULL,
    STATUS            CHAR(1)         NOT NULL,
    OPEN_DATE         DATE            NOT NULL,
    CLOSE_DATE        DATE,
    LAST_MAINT_DATE   TIMESTAMP       NOT NULL,
    LAST_MAINT_USER   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (PORTFOLIO_ID)
);

--====================================================================
-- INVESTMENT POSITIONS TABLE
-- One row per portfolio + investment + date combination.
-- FK to PORTFOLIO_MASTER ensures referential integrity.
-- Position date allows point-in-time valuation queries.
--====================================================================
CREATE TABLE INVESTMENT_POSITIONS (
    PORTFOLIO_ID      CHAR(8)         NOT NULL,
    INVESTMENT_ID     CHAR(10)        NOT NULL,
    POSITION_DATE     DATE            NOT NULL,
    QUANTITY          DECIMAL(18,4)   NOT NULL,
    COST_BASIS        DECIMAL(18,2)   NOT NULL,
    MARKET_VALUE      DECIMAL(18,2)   NOT NULL,
    CURRENCY_CODE     CHAR(3)         NOT NULL,
    LAST_MAINT_DATE   TIMESTAMP       NOT NULL,
    LAST_MAINT_USER   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE),
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER(PORTFOLIO_ID)
);

--====================================================================
-- TRANSACTION HISTORY TABLE
-- One row per trade event. TRANSACTION_ID format:
-- YYYYMMDDHHMMSS + 6-digit sequence number.
-- Types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
-- Status: P=Processed, F=Failed, R=Reversed
--====================================================================
CREATE TABLE TRANSACTION_HISTORY (
    TRANSACTION_ID    CHAR(20)        NOT NULL,
    PORTFOLIO_ID      CHAR(8)         NOT NULL,
    TRANSACTION_DATE  DATE            NOT NULL,
    TRANSACTION_TIME  TIME            NOT NULL,
    INVESTMENT_ID     CHAR(10)        NOT NULL,
    TRANSACTION_TYPE  CHAR(2)         NOT NULL,
    QUANTITY          DECIMAL(18,4)   NOT NULL,
    PRICE            DECIMAL(18,4)   NOT NULL,
    AMOUNT           DECIMAL(18,2)   NOT NULL,
    CURRENCY_CODE    CHAR(3)         NOT NULL,
    STATUS           CHAR(1)         NOT NULL,
    PROCESS_DATE     TIMESTAMP       NOT NULL,
    PROCESS_USER     VARCHAR(8)      NOT NULL,
    PRIMARY KEY (TRANSACTION_ID),
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER(PORTFOLIO_ID)
);

--====================================================================
-- INDEXES - Optimized for common query patterns
--====================================================================
-- Find all portfolios for a client, filtered by status
CREATE INDEX IDX_PORT_MASTER_CLIENT 
    ON PORTFOLIO_MASTER (CLIENT_ID, STATUS);

-- Point-in-time position lookups across portfolios
CREATE INDEX IDX_POSITIONS_DATE 
    ON INVESTMENT_POSITIONS (POSITION_DATE, PORTFOLIO_ID);

-- Transaction history by portfolio (date range queries)
CREATE INDEX IDX_TRANS_HIST_PORT 
    ON TRANSACTION_HISTORY (PORTFOLIO_ID, TRANSACTION_DATE);

-- Date-first index for daily processing and reporting
CREATE INDEX IDX_TRANS_HIST_DATE 
    ON TRANSACTION_HISTORY (TRANSACTION_DATE, PORTFOLIO_ID);

--====================================================================
-- VIEWS - Convenience views for common query patterns
--====================================================================
-- All non-closed portfolios (excludes future-closed)
CREATE VIEW ACTIVE_PORTFOLIOS AS
    SELECT *
    FROM PORTFOLIO_MASTER
    WHERE STATUS = 'A'
    AND (CLOSE_DATE IS NULL OR CLOSE_DATE > CURRENT DATE);

-- Yesterday's positions joined with portfolio names (for T+1 reports)
CREATE VIEW CURRENT_POSITIONS AS
    SELECT p.*, pm.PORTFOLIO_NAME, pm.CLIENT_ID
    FROM INVESTMENT_POSITIONS p
    JOIN PORTFOLIO_MASTER pm ON p.PORTFOLIO_ID = pm.PORTFOLIO_ID
    WHERE p.POSITION_DATE = CURRENT DATE - 1 DAY;

--====================================================================
-- NOTES:
--====================================================================
-- 1. All tables include audit fields (LAST_MAINT_DATE, LAST_MAINT_USER)
-- 2. TRANSACTION_ID format: YYYYMMDDHHMMSS + 6-digit sequence
-- 3. Status codes:
--    - Portfolio: 'A'=Active, 'C'=Closed, 'S'=Suspended
--    - Transaction: 'P'=Processed, 'F'=Failed, 'R'=Reversed
-- 4. Transaction types:
--    - 'BU'=Buy, 'SL'=Sell, 'TR'=Transfer, 'FE'=Fee
-- 5. Indexes optimized for common query patterns
--********************************************************************  