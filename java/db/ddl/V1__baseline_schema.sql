-- ====================================================================
-- V1__baseline_schema.sql
-- PostgreSQL baseline schema translated from the VSAM copybooks of the
-- COBOL Legacy Benchmark Suite (investment portfolio management system).
--
-- Source copybooks:
--   PORTFLIO.cpy -> PORTFOLIO_MASTER       (VSAM KSDS, RECORD KEY PORT-KEY)
--   TRNREC.cpy   -> PORTFOLIO_TRANSACTION  (VSAM KSDS, RECORD KEY TRN-KEY / TRAN-KEY)
--   POSREC.cpy   -> PORTFOLIO_POSITION     (VSAM KSDS, RECORD KEY POS-KEY)
--   HISTREC.cpy  -> HISTORY_RECORD         (VSAM KSDS, RECORD KEY HIST-KEY / TH-KEY)
--   ERRHAND.cpy  -> ERROR_LOG              (ERR-MESSAGE structure; see also DBTBLS.cpy ERRLOG-RECORD)
--   AUDITLOG.cpy -> AUDIT_LOG              (AUDIT-RECORD structure)
--
-- Conventions:
--   COMP-3 / PIC S9(n)V9(m) -> NUMERIC(n+m, m)  (exact decimal, maps to java.math.BigDecimal)
--   PIC X(n)                -> VARCHAR(n) / CHAR(n) for fixed codes
--   PIC 9(8) dates          -> DATE
--   PIC X(26) timestamps    -> TIMESTAMP
--   COBOL FILLER fields are not migrated (reserved space only).
-- ====================================================================

-- ====================================================================
-- PORTFOLIO_MASTER (PORTFLIO.cpy, PORT-RECORD)
-- VSAM KSDS primary key: PORT-KEY = PORT-ID + PORT-ACCOUNT-NO
-- ====================================================================
CREATE TABLE PORTFOLIO_MASTER (
    PORTFOLIO_ID      CHAR(8)        NOT NULL,                          -- PORT-ID
    ACCOUNT_NO        CHAR(10)       NOT NULL,                          -- PORT-ACCOUNT-NO
    CLIENT_NAME       VARCHAR(30)    NOT NULL,                          -- PORT-CLIENT-NAME
    CLIENT_TYPE       CHAR(1)        NOT NULL
        CONSTRAINT CK_PORT_CLIENT_TYPE CHECK (CLIENT_TYPE IN ('I','C','T')), -- PORT-CLIENT-TYPE: I=Individual, C=Corporate, T=Trust
    CREATE_DATE       DATE           NOT NULL,                          -- PORT-CREATE-DATE (YYYYMMDD)
    LAST_MAINT_DATE   DATE,                                             -- PORT-LAST-MAINT (YYYYMMDD)
    STATUS            CHAR(1)        NOT NULL
        CONSTRAINT CK_PORT_STATUS CHECK (STATUS IN ('A','C','S')),      -- PORT-STATUS: A=Active, C=Closed, S=Suspended
    TOTAL_VALUE       NUMERIC(15,2)  NOT NULL DEFAULT 0,                -- PORT-TOTAL-VALUE  S9(13)V99 COMP-3
    CASH_BALANCE      NUMERIC(15,2)  NOT NULL DEFAULT 0,                -- PORT-CASH-BALANCE S9(13)V99 COMP-3
    LAST_MAINT_USER   VARCHAR(8),                                       -- PORT-LAST-USER
    LAST_TRANS_NO     BIGINT,                                           -- PORT-LAST-TRANS PIC 9(8)
    CONSTRAINT PK_PORTFOLIO_MASTER PRIMARY KEY (PORTFOLIO_ID, ACCOUNT_NO)
);

-- Alternate access paths observed in online inquiry programs (INQPORT
-- searches by account number) and in the legacy DB2 schema index
-- IDX_PORT_MASTER_CLIENT (client/status filtering).
CREATE INDEX IDX_PORTFOLIO_MASTER_ACCOUNT ON PORTFOLIO_MASTER (ACCOUNT_NO);
CREATE INDEX IDX_PORTFOLIO_MASTER_STATUS  ON PORTFOLIO_MASTER (STATUS, PORTFOLIO_ID);

-- ====================================================================
-- PORTFOLIO_TRANSACTION (TRNREC.cpy, TRANSACTION-RECORD)
-- VSAM KSDS primary key: TRN-KEY = TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO
-- ====================================================================
CREATE TABLE PORTFOLIO_TRANSACTION (
    TRANS_DATE        DATE           NOT NULL,                          -- TRN-DATE (YYYYMMDD)
    TRANS_TIME        TIME           NOT NULL,                          -- TRN-TIME (HHMMSS)
    PORTFOLIO_ID      CHAR(8)        NOT NULL,                          -- TRN-PORTFOLIO-ID
    SEQUENCE_NO       CHAR(6)        NOT NULL,                          -- TRN-SEQUENCE-NO
    INVESTMENT_ID     CHAR(10)       NOT NULL,                          -- TRN-INVESTMENT-ID
    TRANS_TYPE        CHAR(2)        NOT NULL
        CONSTRAINT CK_TRN_TYPE CHECK (TRANS_TYPE IN ('BU','SL','TR','FE')), -- TRN-TYPE: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    QUANTITY          NUMERIC(15,4)  NOT NULL DEFAULT 0,                -- TRN-QUANTITY S9(11)V9(4) COMP-3
    PRICE             NUMERIC(15,4)  NOT NULL DEFAULT 0,                -- TRN-PRICE    S9(11)V9(4) COMP-3
    AMOUNT            NUMERIC(15,2)  NOT NULL DEFAULT 0,                -- TRN-AMOUNT   S9(13)V9(2) COMP-3
    CURRENCY_CODE     CHAR(3)        NOT NULL,                          -- TRN-CURRENCY
    STATUS            CHAR(1)        NOT NULL
        CONSTRAINT CK_TRN_STATUS CHECK (STATUS IN ('P','D','F','R')),   -- TRN-STATUS: P=Pending, D=Done, F=Failed, R=Reversed
    PROCESS_DATE      TIMESTAMP,                                        -- TRN-PROCESS-DATE PIC X(26)
    PROCESS_USER      VARCHAR(8),                                       -- TRN-PROCESS-USER
    CONSTRAINT PK_PORTFOLIO_TRANSACTION
        PRIMARY KEY (TRANS_DATE, TRANS_TIME, PORTFOLIO_ID, SEQUENCE_NO)
);

-- Alternate access paths: PORTTRAN/POSUPDT process transactions by
-- portfolio; legacy DB2 indexes IDX_TRANS_HIST_PORT / IDX_TRANS_HIST_DATE.
CREATE INDEX IDX_PORT_TRANS_PORTFOLIO ON PORTFOLIO_TRANSACTION (PORTFOLIO_ID, TRANS_DATE);
CREATE INDEX IDX_PORT_TRANS_INVESTMENT ON PORTFOLIO_TRANSACTION (INVESTMENT_ID, TRANS_DATE);

-- ====================================================================
-- PORTFOLIO_POSITION (POSREC.cpy, POSITION-RECORD)
-- VSAM KSDS primary key: POS-KEY = POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID
-- ====================================================================
CREATE TABLE PORTFOLIO_POSITION (
    PORTFOLIO_ID      CHAR(8)        NOT NULL,                          -- POS-PORTFOLIO-ID
    POSITION_DATE     DATE           NOT NULL,                          -- POS-DATE (YYYYMMDD)
    INVESTMENT_ID     CHAR(10)       NOT NULL,                          -- POS-INVESTMENT-ID
    QUANTITY          NUMERIC(15,4)  NOT NULL DEFAULT 0,                -- POS-QUANTITY     S9(11)V9(4) COMP-3
    COST_BASIS        NUMERIC(15,2)  NOT NULL DEFAULT 0,                -- POS-COST-BASIS   S9(13)V9(2) COMP-3
    MARKET_VALUE      NUMERIC(15,2)  NOT NULL DEFAULT 0,                -- POS-MARKET-VALUE S9(13)V9(2) COMP-3
    CURRENCY_CODE     CHAR(3)        NOT NULL,                          -- POS-CURRENCY
    STATUS            CHAR(1)        NOT NULL
        CONSTRAINT CK_POS_STATUS CHECK (STATUS IN ('A','C','P')),       -- POS-STATUS: A=Active, C=Closed, P=Pending
    LAST_MAINT_DATE   TIMESTAMP,                                        -- POS-LAST-MAINT-DATE PIC X(26)
    LAST_MAINT_USER   VARCHAR(8),                                       -- POS-LAST-MAINT-USER
    CONSTRAINT PK_PORTFOLIO_POSITION
        PRIMARY KEY (PORTFOLIO_ID, POSITION_DATE, INVESTMENT_ID)
);

-- Alternate access path: POSUPDT/UTLVAL00 scan positions by date
-- (legacy DB2 index IDX_POSITIONS_DATE).
CREATE INDEX IDX_PORT_POSITION_DATE ON PORTFOLIO_POSITION (POSITION_DATE, PORTFOLIO_ID);

-- ====================================================================
-- HISTORY_RECORD (HISTREC.cpy, HISTORY-RECORD)
-- VSAM KSDS primary key: HIST-KEY = HIST-PORTFOLIO-ID + HIST-DATE + HIST-TIME + HIST-SEQ-NO
-- ====================================================================
CREATE TABLE HISTORY_RECORD (
    PORTFOLIO_ID      CHAR(8)        NOT NULL,                          -- HIST-PORTFOLIO-ID
    HIST_DATE         DATE           NOT NULL,                          -- HIST-DATE (YYYYMMDD)
    HIST_TIME         TIME           NOT NULL,                          -- HIST-TIME (HHMMSS)
    SEQ_NO            CHAR(4)        NOT NULL,                          -- HIST-SEQ-NO
    RECORD_TYPE       CHAR(2)        NOT NULL
        CONSTRAINT CK_HIST_RECORD_TYPE CHECK (RECORD_TYPE IN ('PT','PS','TR')), -- HIST-RECORD-TYPE: PT=Portfolio, PS=Position, TR=Transaction
    ACTION_CODE       CHAR(1)        NOT NULL
        CONSTRAINT CK_HIST_ACTION_CODE CHECK (ACTION_CODE IN ('A','C','D')),    -- HIST-ACTION-CODE: A=Add, C=Change, D=Delete
    BEFORE_IMAGE      VARCHAR(400),                                     -- HIST-BEFORE-IMAGE
    AFTER_IMAGE       VARCHAR(400),                                     -- HIST-AFTER-IMAGE
    REASON_CODE       CHAR(4),                                          -- HIST-REASON-CODE
    PROCESS_DATE      TIMESTAMP,                                        -- HIST-PROCESS-DATE PIC X(26)
    PROCESS_USER      VARCHAR(8),                                       -- HIST-PROCESS-USER
    CONSTRAINT PK_HISTORY_RECORD
        PRIMARY KEY (PORTFOLIO_ID, HIST_DATE, HIST_TIME, SEQ_NO)
);

-- Alternate access path: history inquiry (INQHIST) reads by date range.
CREATE INDEX IDX_HISTORY_RECORD_DATE ON HISTORY_RECORD (HIST_DATE, PORTFOLIO_ID);

-- ====================================================================
-- ERROR_LOG (ERRHAND.cpy ERR-MESSAGE; consistent with DBTBLS.cpy
-- ERRLOG-RECORD / src/database/db2/ERRLOG.sql)
-- Log-style table (VSAM ESDS / DB2 insert-only): surrogate identity key.
-- ====================================================================
CREATE TABLE ERROR_LOG (
    ERROR_LOG_ID      BIGINT         GENERATED ALWAYS AS IDENTITY,      -- surrogate key (log table)
    ERROR_DATE        DATE           NOT NULL,                          -- ERR-DATE PIC X(10)
    ERROR_TIME        TIME           NOT NULL,                          -- ERR-TIME PIC X(8)
    PROGRAM_ID        VARCHAR(8)     NOT NULL,                          -- ERR-PROGRAM
    ERROR_CATEGORY    CHAR(2)        NOT NULL
        CONSTRAINT CK_ERR_CATEGORY CHECK (ERROR_CATEGORY IN ('VS','VL','PR','SY')), -- ERR-CATEGORY: VS=VSAM, VL=Validation, PR=Processing, SY=System
    ERROR_CODE        CHAR(4)        NOT NULL,                          -- ERR-CODE
    ERROR_SEVERITY    SMALLINT       NOT NULL
        CONSTRAINT CK_ERR_SEVERITY CHECK (ERROR_SEVERITY IN (0,4,8,12,16)),     -- ERR-SEVERITY S9(4) COMP per ERR-RETURN-CODES
    ERROR_TEXT        VARCHAR(80),                                      -- ERR-TEXT
    ERROR_DETAILS     VARCHAR(256),                                     -- ERR-DETAILS
    CONSTRAINT PK_ERROR_LOG PRIMARY KEY (ERROR_LOG_ID)
);

CREATE INDEX IDX_ERROR_LOG_DATE    ON ERROR_LOG (ERROR_DATE, ERROR_TIME);
CREATE INDEX IDX_ERROR_LOG_PROGRAM ON ERROR_LOG (PROGRAM_ID, ERROR_DATE);

-- ====================================================================
-- AUDIT_LOG (AUDITLOG.cpy, AUDIT-RECORD)
-- Log-style table (insert-only audit trail): surrogate identity key.
-- ====================================================================
CREATE TABLE AUDIT_LOG (
    AUDIT_LOG_ID      BIGINT         GENERATED ALWAYS AS IDENTITY,      -- surrogate key (log table)
    AUDIT_TIMESTAMP   TIMESTAMP      NOT NULL,                          -- AUD-TIMESTAMP PIC X(26)
    SYSTEM_ID         VARCHAR(8)     NOT NULL,                          -- AUD-SYSTEM-ID
    USER_ID           VARCHAR(8)     NOT NULL,                          -- AUD-USER-ID
    PROGRAM_ID        VARCHAR(8)     NOT NULL,                          -- AUD-PROGRAM
    TERMINAL_ID       VARCHAR(8),                                       -- AUD-TERMINAL
    AUDIT_TYPE        CHAR(4)        NOT NULL
        CONSTRAINT CK_AUD_TYPE CHECK (AUDIT_TYPE IN ('TRAN','USER','SYST')), -- AUD-TYPE
    AUDIT_ACTION      CHAR(8)        NOT NULL
        CONSTRAINT CK_AUD_ACTION CHECK (RTRIM(AUDIT_ACTION) IN
            ('CREATE','UPDATE','DELETE','INQUIRE','LOGIN','LOGOUT','STARTUP','SHUTDOWN')), -- AUD-ACTION
    AUDIT_STATUS      CHAR(4)        NOT NULL
        CONSTRAINT CK_AUD_STATUS CHECK (AUDIT_STATUS IN ('SUCC','FAIL','WARN')), -- AUD-STATUS
    PORTFOLIO_ID      CHAR(8),                                          -- AUD-PORTFOLIO-ID
    ACCOUNT_NO        CHAR(10),                                         -- AUD-ACCOUNT-NO
    BEFORE_IMAGE      VARCHAR(100),                                     -- AUD-BEFORE-IMAGE
    AFTER_IMAGE       VARCHAR(100),                                     -- AUD-AFTER-IMAGE
    AUDIT_MESSAGE     VARCHAR(100),                                     -- AUD-MESSAGE
    CONSTRAINT PK_AUDIT_LOG PRIMARY KEY (AUDIT_LOG_ID)
);

CREATE INDEX IDX_AUDIT_LOG_TIMESTAMP ON AUDIT_LOG (AUDIT_TIMESTAMP);
CREATE INDEX IDX_AUDIT_LOG_PORTFOLIO ON AUDIT_LOG (PORTFOLIO_ID, ACCOUNT_NO);
CREATE INDEX IDX_AUDIT_LOG_USER      ON AUDIT_LOG (USER_ID, AUDIT_TIMESTAMP);

-- ====================================================================
-- Foreign keys
-- PORTFOLIO_MASTER's VSAM key is (PORTFOLIO_ID, ACCOUNT_NO) while child
-- records reference PORTFOLIO_ID alone, so a UNIQUE index on
-- PORTFOLIO_ID backs the single-column foreign keys.
-- ====================================================================
ALTER TABLE PORTFOLIO_MASTER
    ADD CONSTRAINT UQ_PORTFOLIO_MASTER_ID UNIQUE (PORTFOLIO_ID);

ALTER TABLE PORTFOLIO_TRANSACTION
    ADD CONSTRAINT FK_TRANS_PORTFOLIO
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER (PORTFOLIO_ID);

ALTER TABLE PORTFOLIO_POSITION
    ADD CONSTRAINT FK_POSITION_PORTFOLIO
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER (PORTFOLIO_ID);

ALTER TABLE HISTORY_RECORD
    ADD CONSTRAINT FK_HISTORY_PORTFOLIO
    FOREIGN KEY (PORTFOLIO_ID) REFERENCES PORTFOLIO_MASTER (PORTFOLIO_ID);
