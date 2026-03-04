-- =============================================================================
-- Portfolio Management System - Database Schema
-- Migrated from COBOL DB2 table definitions (DBTBLS.cpy, POSHIST.sql, etc.)
-- =============================================================================

-- ============================================================================
-- PORTFOLIO MASTER TABLE (from db2-definitions.sql)
-- ============================================================================
CREATE TABLE IF NOT EXISTS PORTFOLIO_MASTER (
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

-- ============================================================================
-- INVESTMENT POSITIONS TABLE (from db2-definitions.sql / POSREC.cpy)
-- ============================================================================
CREATE TABLE IF NOT EXISTS INVESTMENT_POSITIONS (
    PORTFOLIO_ID      CHAR(8)         NOT NULL,
    INVESTMENT_ID     CHAR(10)        NOT NULL,
    POSITION_DATE     DATE            NOT NULL,
    INVESTMENT_TYPE   CHAR(2),
    STATUS            CHAR(1),
    QUANTITY          DECIMAL(18,4)   NOT NULL,
    COST_BASIS        DECIMAL(18,2)   NOT NULL,
    MARKET_VALUE      DECIMAL(18,2)   NOT NULL,
    CURRENCY_CODE     CHAR(3)         NOT NULL,
    LAST_MAINT_DATE   TIMESTAMP       NOT NULL,
    LAST_MAINT_USER   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE),
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER(PORTFOLIO_ID)
);

-- ============================================================================
-- TRANSACTION HISTORY TABLE (from db2-definitions.sql / TRNREC.cpy)
-- ============================================================================
CREATE TABLE IF NOT EXISTS TRANSACTION_HISTORY (
    TRANSACTION_ID    CHAR(20)        NOT NULL,
    PORTFOLIO_ID      CHAR(8)         NOT NULL,
    TRANSACTION_DATE  DATE            NOT NULL,
    TRANSACTION_TIME  TIME            NOT NULL,
    INVESTMENT_ID     CHAR(10)        NOT NULL,
    TRANSACTION_TYPE  CHAR(2)         NOT NULL,
    QUANTITY          DECIMAL(18,4)   NOT NULL,
    PRICE             DECIMAL(18,4)   NOT NULL,
    AMOUNT            DECIMAL(18,2)   NOT NULL,
    CURRENCY_CODE     CHAR(3)         NOT NULL,
    STATUS            CHAR(1)         NOT NULL,
    PROCESS_DATE      TIMESTAMP       NOT NULL,
    PROCESS_USER      VARCHAR(8)      NOT NULL,
    PRIMARY KEY (TRANSACTION_ID),
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER(PORTFOLIO_ID)
);

-- ============================================================================
-- POSITION HISTORY TABLE (from POSHIST.sql / DBTBLS.cpy)
-- ============================================================================
CREATE TABLE IF NOT EXISTS POSHIST (
    ACCOUNT_NO        CHAR(8)         NOT NULL,
    PORTFOLIO_ID      CHAR(10)        NOT NULL,
    TRANS_DATE        DATE            NOT NULL,
    TRANS_TIME        TIME            NOT NULL,
    TRANS_TYPE        CHAR(2)         NOT NULL,
    SECURITY_ID       CHAR(12)        NOT NULL,
    QUANTITY          DECIMAL(15,3)   NOT NULL,
    PRICE             DECIMAL(15,3)   NOT NULL,
    AMOUNT            DECIMAL(15,2)   NOT NULL,
    FEES              DECIMAL(15,2)   NOT NULL DEFAULT 0,
    TOTAL_AMOUNT      DECIMAL(15,2)   NOT NULL,
    COST_BASIS        DECIMAL(15,2)   NOT NULL,
    GAIN_LOSS         DECIMAL(15,2)   NOT NULL,
    PROCESS_DATE      DATE            NOT NULL,
    PROCESS_TIME      TIME            NOT NULL,
    PROGRAM_ID        CHAR(8)         NOT NULL,
    USER_ID           CHAR(8)         NOT NULL,
    AUDIT_TIMESTAMP   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
);

-- ============================================================================
-- ERROR LOG TABLE (from ERRLOG.sql / DBTBLS.cpy)
-- ============================================================================
CREATE TABLE IF NOT EXISTS ERRLOG (
    ERROR_TIMESTAMP   TIMESTAMP       NOT NULL,
    PROGRAM_ID        CHAR(8)         NOT NULL,
    ERROR_TYPE        CHAR(1)         NOT NULL,
    ERROR_SEVERITY    INTEGER         NOT NULL,
    ERROR_CODE        CHAR(8)         NOT NULL,
    ERROR_MESSAGE     VARCHAR(200)    NOT NULL,
    PROCESS_DATE      DATE            NOT NULL,
    PROCESS_TIME      TIME            NOT NULL,
    USER_ID           CHAR(8)         NOT NULL,
    ADDITIONAL_INFO   VARCHAR(500),
    PRIMARY KEY (ERROR_TIMESTAMP, PROGRAM_ID)
);

-- ============================================================================
-- RETURN CODES TABLE (from RTNCODES.sql)
-- ============================================================================
CREATE TABLE IF NOT EXISTS RTNCODES (
    TIMESTAMP         TIMESTAMP       NOT NULL,
    PROGRAM_ID        CHAR(8)         NOT NULL,
    RETURN_CODE       INTEGER         NOT NULL,
    HIGHEST_CODE      INTEGER         NOT NULL,
    STATUS_CODE       CHAR(1)         NOT NULL,
    MESSAGE_TEXT      VARCHAR(80),
    PRIMARY KEY (TIMESTAMP, PROGRAM_ID)
);

-- ============================================================================
-- AUTHORIZATION TABLE (from SECMGR.cbl)
-- ============================================================================
CREATE TABLE IF NOT EXISTS AUTHFILE (
    USER_ID           CHAR(8)         NOT NULL,
    PASSWORD_HASH     VARCHAR(128)    NOT NULL,
    STATUS            CHAR(1)         NOT NULL DEFAULT 'A',
    RESOURCE          CHAR(8)         NOT NULL,
    ACCESS_TYPE       CHAR(8)         NOT NULL,
    PRIMARY KEY (USER_ID, RESOURCE, ACCESS_TYPE)
);

-- ============================================================================
-- AUDIT LOG TABLE (from SECMGR.cbl / ERRHNDL.cbl)
-- ============================================================================
CREATE TABLE IF NOT EXISTS AUDITLOG (
    AUDIT_TIMESTAMP   TIMESTAMP       NOT NULL,
    USER_ID           CHAR(8)         NOT NULL,
    TERMINAL_ID       CHAR(4),
    TRANS_ID          CHAR(4),
    PROGRAM           CHAR(8)         NOT NULL DEFAULT 'SECMGR',
    ACCESS_TYPE       CHAR(8)         NOT NULL,
    ACTION            VARCHAR(50),
    DETAIL            VARCHAR(200),
    PRIMARY KEY (AUDIT_TIMESTAMP, USER_ID)
);

-- ============================================================================
-- BATCH CONTROL TABLE (from BCHCTL.cpy)
-- ============================================================================
CREATE TABLE IF NOT EXISTS BATCH_CONTROL (
    JOB_NAME          CHAR(8)         NOT NULL,
    PROCESS_DATE      CHAR(8)         NOT NULL,
    SEQUENCE_NO       INTEGER         NOT NULL,
    STATUS            CHAR(1)         NOT NULL,
    RETURN_CODE       INTEGER         DEFAULT 0,
    RESTART_COUNT     INTEGER         DEFAULT 0,
    MAX_RESTARTS      INTEGER         DEFAULT 3,
    START_TIME        CHAR(26),
    END_TIME          CHAR(26),
    ATTEMPT_TS        CHAR(26),
    RECORDS_READ      INTEGER         DEFAULT 0,
    RECORDS_WRITTEN   INTEGER         DEFAULT 0,
    ERROR_COUNT       INTEGER         DEFAULT 0,
    ERROR_DESC        VARCHAR(80),
    PRIMARY KEY (JOB_NAME, PROCESS_DATE, SEQUENCE_NO)
);

-- ============================================================================
-- PROCESS SEQUENCE TABLE (from PRCSEQ.cpy)
-- ============================================================================
CREATE TABLE IF NOT EXISTS PROCESS_SEQUENCE (
    PROCESS_ID        CHAR(8)         NOT NULL,
    SEQUENCE_TYPE     CHAR(3)         NOT NULL,
    FREQUENCY         CHAR(1)         NOT NULL,
    ACTIVE_DAYS       CHAR(7)         DEFAULT 'YYYYYNN',
    RESTARTABLE       CHAR(1)         DEFAULT 'Y',
    DEP_COUNT         INTEGER         DEFAULT 0,
    PRIMARY KEY (PROCESS_ID)
);

-- ============================================================================
-- PROCESS DEPENDENCIES TABLE (from PRCSEQ.cpy PSR-DEP-ENTRY)
-- ============================================================================
CREATE TABLE IF NOT EXISTS PROCESS_DEPENDENCIES (
    PROCESS_ID        CHAR(8)         NOT NULL,
    DEP_INDEX         INTEGER         NOT NULL,
    DEP_ID            CHAR(8)         NOT NULL,
    DEP_TYPE          CHAR(1)         NOT NULL,
    DEP_RC            INTEGER         DEFAULT 0,
    PRIMARY KEY (PROCESS_ID, DEP_INDEX),
    FOREIGN KEY (PROCESS_ID) REFERENCES PROCESS_SEQUENCE(PROCESS_ID)
);

-- ============================================================================
-- INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS IDX_PORT_MASTER_CLIENT ON PORTFOLIO_MASTER (CLIENT_ID, STATUS);
CREATE INDEX IF NOT EXISTS IDX_POSITIONS_DATE ON INVESTMENT_POSITIONS (POSITION_DATE, PORTFOLIO_ID);
CREATE INDEX IF NOT EXISTS IDX_TRANS_HIST_PORT ON TRANSACTION_HISTORY (PORTFOLIO_ID, TRANSACTION_DATE);
CREATE INDEX IF NOT EXISTS IDX_TRANS_HIST_DATE ON TRANSACTION_HISTORY (TRANSACTION_DATE, PORTFOLIO_ID);
CREATE INDEX IF NOT EXISTS IDX_POSHIST_SEC ON POSHIST (SECURITY_ID, TRANS_DATE);
CREATE INDEX IF NOT EXISTS IDX_POSHIST_PROC ON POSHIST (PROCESS_DATE, PROGRAM_ID);
CREATE INDEX IF NOT EXISTS IDX_ERRLOG_DATE ON ERRLOG (PROCESS_DATE, ERROR_SEVERITY);
CREATE INDEX IF NOT EXISTS IDX_RTNCODES_PRG ON RTNCODES (PROGRAM_ID, TIMESTAMP);
CREATE INDEX IF NOT EXISTS IDX_RTNCODES_STS ON RTNCODES (STATUS_CODE, TIMESTAMP);
