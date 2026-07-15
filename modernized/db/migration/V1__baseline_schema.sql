-- =====================================================================
-- V1: Baseline relational schema for the modernized Investment
--     Portfolio Management System.
--
-- Source-of-truth mappings (see ../README.md for the full mapping doc):
--   * src/database/db2/db2-definitions.sql  -> portfolio_master,
--       investment_positions, transaction_history, views
--   * src/database/db2/POSHIST.sql          -> position_history
--   * src/database/db2/ERRLOG.sql           -> error_log
--   * src/database/db2/RTNCODES.sql         -> return_codes
--   * src/database/vsam/vsam-definitions.txt (PORTMSTR/TRANHIST/POSHIST
--       KSDS keys) -> primary keys / unique indexes below
--
-- Dialect: ANSI SQL, tested with PostgreSQL 14+.
-- =====================================================================

-- =====================================================================
-- PORTFOLIO_MASTER (DB2 PORTFOLIO_MASTER + VSAM PORTMSTR)
-- VSAM KSDS key: portfolio_id(8) + account_type(2) + branch_id(2)
-- =====================================================================
CREATE TABLE portfolio_master (
    portfolio_id      CHAR(8)      NOT NULL,
    account_type      CHAR(2)      NOT NULL,
    branch_id         CHAR(2)      NOT NULL,
    client_id         CHAR(10)     NOT NULL,
    portfolio_name    VARCHAR(50)  NOT NULL,
    currency_code     CHAR(3)      NOT NULL,
    risk_level        CHAR(1)      NOT NULL,
    status            CHAR(1)      NOT NULL,
    open_date         DATE         NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP    NOT NULL,
    last_maint_user   VARCHAR(8)   NOT NULL,
    CONSTRAINT portfolio_master_pk PRIMARY KEY (portfolio_id),
    CONSTRAINT portfolio_master_status_ck
        CHECK (status IN ('A', 'C', 'S'))
);

-- Preserves the full VSAM PORTMSTR KSDS key as a uniqueness guarantee.
CREATE UNIQUE INDEX portfolio_master_vsam_key
    ON portfolio_master (portfolio_id, account_type, branch_id);

CREATE INDEX idx_port_master_client
    ON portfolio_master (client_id, status);

-- =====================================================================
-- INVESTMENT_POSITIONS (DB2 INVESTMENT_POSITIONS)
-- =====================================================================
CREATE TABLE investment_positions (
    portfolio_id      CHAR(8)        NOT NULL,
    investment_id     CHAR(10)       NOT NULL,
    position_date     DATE           NOT NULL,
    quantity          DECIMAL(18,4)  NOT NULL,
    cost_basis        DECIMAL(18,2)  NOT NULL,
    market_value      DECIMAL(18,2)  NOT NULL,
    currency_code     CHAR(3)        NOT NULL,
    last_maint_date   TIMESTAMP      NOT NULL,
    last_maint_user   VARCHAR(8)     NOT NULL,
    CONSTRAINT investment_positions_pk
        PRIMARY KEY (portfolio_id, investment_id, position_date),
    CONSTRAINT investment_positions_portfolio_fk
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master (portfolio_id)
);

CREATE INDEX idx_positions_date
    ON investment_positions (position_date, portfolio_id);

-- =====================================================================
-- TRANSACTION_HISTORY (DB2 TRANSACTION_HISTORY + VSAM TRANHIST)
-- VSAM KSDS key: trans_date(8) + trans_time(6) + portfolio_id(8) + seq(6)
-- is preserved by transaction_id (YYYYMMDDHHMMSS + 6-digit sequence).
-- =====================================================================
CREATE TABLE transaction_history (
    transaction_id    CHAR(20)       NOT NULL,
    portfolio_id      CHAR(8)        NOT NULL,
    transaction_date  DATE           NOT NULL,
    transaction_time  TIME           NOT NULL,
    investment_id     CHAR(10)       NOT NULL,
    transaction_type  CHAR(2)        NOT NULL,
    quantity          DECIMAL(18,4)  NOT NULL,
    price             DECIMAL(18,4)  NOT NULL,
    amount            DECIMAL(18,2)  NOT NULL,
    currency_code     CHAR(3)        NOT NULL,
    status            CHAR(1)        NOT NULL,
    process_date      TIMESTAMP      NOT NULL,
    process_user      VARCHAR(8)     NOT NULL,
    CONSTRAINT transaction_history_pk PRIMARY KEY (transaction_id),
    CONSTRAINT transaction_history_portfolio_fk
        FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master (portfolio_id),
    CONSTRAINT transaction_history_type_ck
        CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT transaction_history_status_ck
        CHECK (status IN ('P', 'F', 'R'))
);

CREATE INDEX idx_trans_hist_port
    ON transaction_history (portfolio_id, transaction_date);

CREATE INDEX idx_trans_hist_date
    ON transaction_history (transaction_date, portfolio_id);

-- =====================================================================
-- POSITION_HISTORY (DB2 POSHIST)
-- DB2 range-partitioning by trans_date is an operational optimization;
-- use native partitioning in the target DB if volumes require it.
-- =====================================================================
CREATE TABLE position_history (
    account_no        CHAR(8)        NOT NULL,
    portfolio_id      CHAR(10)       NOT NULL,
    trans_date        DATE           NOT NULL,
    trans_time        TIME           NOT NULL,
    trans_type        CHAR(2)        NOT NULL,
    security_id       CHAR(12)       NOT NULL,
    quantity          DECIMAL(15,3)  NOT NULL,
    price             DECIMAL(15,3)  NOT NULL,
    amount            DECIMAL(15,2)  NOT NULL,
    fees              DECIMAL(15,2)  NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)  NOT NULL,
    cost_basis        DECIMAL(15,2)  NOT NULL,
    gain_loss         DECIMAL(15,2)  NOT NULL,
    process_date      DATE           NOT NULL,
    process_time      TIME           NOT NULL,
    program_id        CHAR(8)        NOT NULL,
    user_id           CHAR(8)        NOT NULL,
    audit_timestamp   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT position_history_pk
        PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

COMMENT ON TABLE position_history IS
    'Position History - stores all portfolio transaction history (DB2 POSHIST)';
COMMENT ON COLUMN position_history.trans_type IS
    'Transaction Type (BU=Buy, SL=Sell, TR=Transfer)';
COMMENT ON COLUMN position_history.gain_loss IS
    'Realized Gain/Loss Amount';

CREATE INDEX position_history_ix1
    ON position_history (security_id, trans_date);

CREATE INDEX position_history_ix2
    ON position_history (process_date, program_id);

-- =====================================================================
-- ERROR_LOG (DB2 ERRLOG)
-- =====================================================================
CREATE TABLE error_log (
    error_timestamp   TIMESTAMP     NOT NULL,
    program_id        CHAR(8)       NOT NULL,
    error_type        CHAR(1)       NOT NULL,
    error_severity    INTEGER       NOT NULL,
    error_code        CHAR(8)       NOT NULL,
    error_message     VARCHAR(200)  NOT NULL,
    process_date      DATE          NOT NULL,
    process_time      TIME          NOT NULL,
    user_id           CHAR(8)       NOT NULL,
    additional_info   VARCHAR(500),
    CONSTRAINT error_log_pk PRIMARY KEY (error_timestamp, program_id),
    CONSTRAINT error_log_type_ck
        CHECK (error_type IN ('S', 'A', 'D')),
    CONSTRAINT error_log_severity_ck
        CHECK (error_severity BETWEEN 1 AND 4)
);

COMMENT ON TABLE error_log IS
    'Error Logging - application errors and warnings (DB2 ERRLOG)';
COMMENT ON COLUMN error_log.error_type IS
    'Error Type (S=System, A=Application, D=Data)';
COMMENT ON COLUMN error_log.error_severity IS
    'Error Severity (1=Info, 2=Warning, 3=Error, 4=Severe)';

CREATE INDEX error_log_ix1
    ON error_log (process_date, error_severity DESC);

-- =====================================================================
-- RETURN_CODES (DB2 RTNCODES)
-- Column TIMESTAMP renamed to logged_at (reserved word in most dialects).
-- =====================================================================
CREATE TABLE return_codes (
    logged_at         TIMESTAMP    NOT NULL,
    program_id        CHAR(8)      NOT NULL,
    return_code       INTEGER      NOT NULL,
    highest_code      INTEGER      NOT NULL,
    status_code       CHAR(1)      NOT NULL,
    message_text      VARCHAR(80),
    CONSTRAINT return_codes_pk PRIMARY KEY (logged_at, program_id)
);

CREATE INDEX return_codes_prg_idx
    ON return_codes (program_id, logged_at);

CREATE INDEX return_codes_sts_idx
    ON return_codes (status_code, logged_at);

-- =====================================================================
-- VIEWS (DB2 db2-definitions.sql)
-- =====================================================================
CREATE VIEW active_portfolios AS
    SELECT *
    FROM portfolio_master
    WHERE status = 'A'
      AND (close_date IS NULL OR close_date > CURRENT_DATE);

CREATE VIEW current_positions AS
    SELECT p.*, pm.portfolio_name, pm.client_id
    FROM investment_positions p
    JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day';
