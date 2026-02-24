-- Migration 001: Create DB2-equivalent tables in PostgreSQL
-- Source: src/database/db2/db2-definitions.sql
-- Converts DB2 DDL to PostgreSQL-compatible DDL

BEGIN;

-- ====================================================================
-- PORTFOLIO MASTER TABLE
-- Source: DB2 PORTFOLIO_MASTER
-- ====================================================================
CREATE TABLE IF NOT EXISTS portfolio_master (
    portfolio_id      VARCHAR(8)          NOT NULL,
    account_type      VARCHAR(2)          NOT NULL,
    branch_id         VARCHAR(2)          NOT NULL,
    client_id         VARCHAR(10)         NOT NULL,
    portfolio_name    VARCHAR(50)         NOT NULL,
    currency_code     VARCHAR(3)          NOT NULL,
    risk_level        VARCHAR(1)          NOT NULL,
    status            VARCHAR(1)          NOT NULL,
    open_date         DATE                NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    last_maint_user   VARCHAR(8)          NOT NULL,
    PRIMARY KEY (portfolio_id)
);

COMMENT ON TABLE portfolio_master IS 'Portfolio master records. Migrated from DB2 PORTFOLIO_MASTER table.';
COMMENT ON COLUMN portfolio_master.status IS 'A=Active, C=Closed, S=Suspended';
COMMENT ON COLUMN portfolio_master.risk_level IS 'Risk classification level';

-- ====================================================================
-- INVESTMENT POSITIONS TABLE
-- Source: DB2 INVESTMENT_POSITIONS
-- ====================================================================
CREATE TABLE IF NOT EXISTS investment_positions (
    portfolio_id      VARCHAR(8)          NOT NULL,
    investment_id     VARCHAR(10)         NOT NULL,
    position_date     DATE                NOT NULL,
    quantity          NUMERIC(18,4)       NOT NULL,
    cost_basis        NUMERIC(18,2)       NOT NULL,
    market_value      NUMERIC(18,2)       NOT NULL,
    currency_code     VARCHAR(3)          NOT NULL,
    last_maint_date   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    last_maint_user   VARCHAR(8)          NOT NULL,
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE investment_positions IS 'Investment position records. Migrated from DB2 INVESTMENT_POSITIONS table.';

-- ====================================================================
-- TRANSACTION HISTORY TABLE
-- Source: DB2 TRANSACTION_HISTORY
-- ====================================================================
CREATE TABLE IF NOT EXISTS transaction_history (
    transaction_id    VARCHAR(20)         NOT NULL,
    portfolio_id      VARCHAR(8)          NOT NULL,
    transaction_date  DATE                NOT NULL,
    transaction_time  TIME                NOT NULL,
    investment_id     VARCHAR(10)         NOT NULL,
    transaction_type  VARCHAR(2)          NOT NULL,
    quantity          NUMERIC(18,4)       NOT NULL,
    price             NUMERIC(18,4)       NOT NULL,
    amount            NUMERIC(18,2)       NOT NULL,
    currency_code     VARCHAR(3)          NOT NULL,
    status            VARCHAR(1)          NOT NULL,
    process_date      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    process_user      VARCHAR(8)          NOT NULL,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE transaction_history IS 'Transaction history records. Migrated from DB2 TRANSACTION_HISTORY table.';
COMMENT ON COLUMN transaction_history.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN transaction_history.status IS 'P=Processed, F=Failed, R=Reversed';
COMMENT ON COLUMN transaction_history.transaction_id IS 'Format: YYYYMMDDHHMMSS + 6-digit sequence';

-- ====================================================================
-- POSITION HISTORY TABLE (POSHIST)
-- Source: DB2 POSHIST from DBTBLS copybook
-- ====================================================================
CREATE TABLE IF NOT EXISTS position_history (
    account_no        VARCHAR(8)          NOT NULL,
    portfolio_id      VARCHAR(10)         NOT NULL,
    trans_date        DATE                NOT NULL,
    trans_time        TIME                NOT NULL,
    trans_type        VARCHAR(2)          NOT NULL,
    security_id       VARCHAR(12)         NOT NULL,
    quantity          NUMERIC(15,3)       NOT NULL,
    price             NUMERIC(15,3)       NOT NULL,
    amount            NUMERIC(18,2)       NOT NULL,
    fees              NUMERIC(18,2)       NOT NULL DEFAULT 0,
    total_amount      NUMERIC(18,2)       NOT NULL,
    cost_basis        NUMERIC(18,2)       NOT NULL DEFAULT 0,
    gain_loss         NUMERIC(18,2)       NOT NULL DEFAULT 0,
    process_date      DATE                NOT NULL DEFAULT CURRENT_DATE,
    process_time      TIME                NOT NULL DEFAULT CURRENT_TIME,
    program_id        VARCHAR(8)          NOT NULL,
    user_id           VARCHAR(8)          NOT NULL,
    audit_timestamp   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

COMMENT ON TABLE position_history IS 'Position history records. Migrated from DB2 POSHIST table (DBTBLS copybook).';
COMMENT ON COLUMN position_history.trans_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';

-- ====================================================================
-- ERROR LOG TABLE (ERRLOG)
-- Source: DB2 ERRLOG from DBTBLS copybook
-- ====================================================================
CREATE TABLE IF NOT EXISTS error_log (
    error_timestamp   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    program_id        VARCHAR(8)          NOT NULL,
    error_type        VARCHAR(1)          NOT NULL,
    error_severity    INTEGER             NOT NULL,
    error_code        VARCHAR(8)          NOT NULL,
    error_message     VARCHAR(200)        NOT NULL,
    process_date      DATE                NOT NULL DEFAULT CURRENT_DATE,
    process_time      TIME                NOT NULL DEFAULT CURRENT_TIME,
    user_id           VARCHAR(8)          NOT NULL,
    additional_info   VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);

COMMENT ON TABLE error_log IS 'Application error log. Migrated from DB2 ERRLOG table (DBTBLS copybook).';
COMMENT ON COLUMN error_log.error_type IS 'S=System, A=Application, D=Data';
COMMENT ON COLUMN error_log.error_severity IS '1=Info, 2=Warning, 3=Error, 4=Fatal';

-- ====================================================================
-- AUTHORIZATION FILE TABLE (AUTHFILE)
-- Source: Inferred from SECMGR program DB2 SQL
-- ====================================================================
CREATE TABLE IF NOT EXISTS auth_file (
    user_id           VARCHAR(8)          NOT NULL,
    resource_name     VARCHAR(50)         NOT NULL,
    access_type       VARCHAR(10)         NOT NULL,
    granted_date      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    granted_by        VARCHAR(8)          NOT NULL,
    expiry_date       DATE,
    status            VARCHAR(1)          NOT NULL DEFAULT 'A',
    PRIMARY KEY (user_id, resource_name)
);

COMMENT ON TABLE auth_file IS 'User authorization records. Migrated from DB2 AUTHFILE (inferred from SECMGR).';
COMMENT ON COLUMN auth_file.status IS 'A=Active, I=Inactive';

-- ====================================================================
-- AUDIT LOG TABLE (AUDITLOG)
-- Source: Inferred from SECMGR program DB2 SQL + AUDITLOG copybook
-- ====================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    audit_timestamp   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    system_id         VARCHAR(8)          NOT NULL,
    user_id           VARCHAR(8)          NOT NULL,
    terminal_id       VARCHAR(8),
    program_id        VARCHAR(8)          NOT NULL,
    audit_type        VARCHAR(4)          NOT NULL,
    action_code       VARCHAR(8)          NOT NULL,
    status            VARCHAR(4)          NOT NULL,
    portfolio_id      VARCHAR(8),
    account_no        VARCHAR(10),
    before_image      TEXT,
    after_image       TEXT,
    message           VARCHAR(200),
    PRIMARY KEY (audit_timestamp, user_id, program_id)
);

COMMENT ON TABLE audit_log IS 'Security and operational audit trail. Migrated from DB2 AUDITLOG.';
COMMENT ON COLUMN audit_log.audit_type IS 'TRAN=Transaction, USER=User, SYST=System';
COMMENT ON COLUMN audit_log.action_code IS 'CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN';
COMMENT ON COLUMN audit_log.status IS 'SUCC=Success, FAIL=Failure, WARN=Warning';

-- ====================================================================
-- RETURN CODES TABLE
-- Source: src/database/db2/RTNCODES.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS return_codes (
    logged_at         TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    program_id        VARCHAR(8)          NOT NULL,
    return_code       INTEGER             NOT NULL,
    highest_code      INTEGER             NOT NULL,
    status_code       VARCHAR(1)          NOT NULL,
    message_text      VARCHAR(80),
    PRIMARY KEY (logged_at, program_id)
);

COMMENT ON TABLE return_codes IS 'Return code logging. Migrated from DB2 RTNCODES table.';
COMMENT ON COLUMN return_codes.status_code IS 'S=Success, W=Warning, E=Error, F=Fatal';

-- ====================================================================
-- INDEXES
-- Source: DB2 index definitions
-- ====================================================================
CREATE INDEX IF NOT EXISTS idx_port_master_client
    ON portfolio_master (client_id, status);

CREATE INDEX IF NOT EXISTS idx_positions_date
    ON investment_positions (position_date, portfolio_id);

CREATE INDEX IF NOT EXISTS idx_trans_hist_port
    ON transaction_history (portfolio_id, transaction_date);

CREATE INDEX IF NOT EXISTS idx_trans_hist_date
    ON transaction_history (transaction_date, portfolio_id);

CREATE INDEX IF NOT EXISTS idx_poshist_portfolio
    ON position_history (portfolio_id, trans_date);

CREATE INDEX IF NOT EXISTS idx_errlog_program
    ON error_log (program_id, error_timestamp);

CREATE INDEX IF NOT EXISTS idx_errlog_severity
    ON error_log (error_severity, error_timestamp);

CREATE INDEX IF NOT EXISTS idx_rtncodes_program
    ON return_codes (program_id, logged_at);

CREATE INDEX IF NOT EXISTS idx_rtncodes_status
    ON return_codes (status_code, logged_at);

CREATE INDEX IF NOT EXISTS idx_audit_user
    ON audit_log (user_id, audit_timestamp);

-- ====================================================================
-- VIEWS
-- Source: DB2 view definitions
-- ====================================================================
CREATE OR REPLACE VIEW active_portfolios AS
    SELECT *
    FROM portfolio_master
    WHERE status = 'A'
    AND (close_date IS NULL OR close_date > CURRENT_DATE);

CREATE OR REPLACE VIEW current_positions AS
    SELECT p.*, pm.portfolio_name, pm.client_id
    FROM investment_positions p
    JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day';

COMMIT;
