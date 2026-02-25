-- =====================================================================
-- PostgreSQL Migration Script: Initial Schema
-- Migrated from DB2 DDL files in src/database/db2/
-- Version: 1.0
-- Date: 2024
-- =====================================================================

-- ====================================================================
-- PORTFOLIO MASTER TABLE
-- Migrated from: db2-definitions.sql (PORTFOLIO_MASTER)
-- Replaces VSAM: PORTMSTR (Key: Portfolio ID + Account Type + Branch ID)
-- ====================================================================
CREATE TABLE IF NOT EXISTS portfolio_master (
    portfolio_id      CHAR(8)         NOT NULL,
    account_type      CHAR(2)         NOT NULL,
    branch_id         CHAR(2)         NOT NULL,
    client_id         CHAR(10)        NOT NULL,
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     CHAR(3)         NOT NULL DEFAULT 'USD',
    risk_level        CHAR(1)         NOT NULL DEFAULT 'M',
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    open_date         DATE            NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    
    CONSTRAINT pk_portfolio_master PRIMARY KEY (portfolio_id)
);

COMMENT ON TABLE portfolio_master IS 'Portfolio Master - Migrated from VSAM PORTMSTR and DB2 PORTFOLIO_MASTER';
COMMENT ON COLUMN portfolio_master.status IS 'Status: A=Active, C=Closed, S=Suspended';
COMMENT ON COLUMN portfolio_master.risk_level IS 'Risk Level: L=Low, M=Medium, H=High';

CREATE INDEX idx_portfolio_master_client ON portfolio_master (client_id, status);
CREATE INDEX idx_portfolio_master_branch ON portfolio_master (branch_id, account_type);

-- ====================================================================
-- INVESTMENT POSITIONS TABLE
-- Migrated from: db2-definitions.sql (INVESTMENT_POSITIONS)
-- Replaces VSAM: POSHIST (Key: Portfolio ID + Position Date + Investment ID)
-- ====================================================================
CREATE TABLE IF NOT EXISTS investment_positions (
    portfolio_id      CHAR(8)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    cost_basis        DECIMAL(18,2)   NOT NULL,
    market_value      DECIMAL(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL DEFAULT 'USD',
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    
    CONSTRAINT pk_investment_positions PRIMARY KEY (portfolio_id, investment_id, position_date),
    CONSTRAINT fk_positions_portfolio FOREIGN KEY (portfolio_id) 
        REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE investment_positions IS 'Investment Positions - Migrated from VSAM POSHIST and DB2 INVESTMENT_POSITIONS';
COMMENT ON COLUMN investment_positions.status IS 'Status: A=Active, C=Closed, P=Pending';

CREATE INDEX idx_positions_date ON investment_positions (position_date, portfolio_id);
CREATE INDEX idx_positions_investment ON investment_positions (investment_id, position_date);

-- ====================================================================
-- TRANSACTION HISTORY TABLE
-- Migrated from: db2-definitions.sql (TRANSACTION_HISTORY)
-- Replaces VSAM: TRANHIST (Key: Date + Time + Portfolio ID + Sequence)
-- ====================================================================
CREATE TABLE IF NOT EXISTS transaction_history (
    transaction_id    CHAR(20)        NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    transaction_type  CHAR(2)         NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    price             DECIMAL(18,4)   NOT NULL,
    amount            DECIMAL(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL DEFAULT 'USD',
    status            CHAR(1)         NOT NULL DEFAULT 'P',
    process_date      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_user      VARCHAR(8)      NOT NULL,
    
    CONSTRAINT pk_transaction_history PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id) 
        REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE transaction_history IS 'Transaction History - Migrated from VSAM TRANHIST and DB2 TRANSACTION_HISTORY';
COMMENT ON COLUMN transaction_history.transaction_type IS 'Type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN transaction_history.status IS 'Status: P=Pending, D=Done, F=Failed, R=Reversed';

CREATE INDEX idx_trans_hist_portfolio ON transaction_history (portfolio_id, transaction_date);
CREATE INDEX idx_trans_hist_date ON transaction_history (transaction_date, portfolio_id);
CREATE INDEX idx_trans_hist_investment ON transaction_history (investment_id, transaction_date);

-- ====================================================================
-- POSITION HISTORY TABLE (POSHIST)
-- Migrated from: POSHIST.sql
-- Partitioned by transaction date (quarterly)
-- ====================================================================
CREATE TABLE IF NOT EXISTS position_history (
    account_no        CHAR(8)         NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    trans_date        DATE            NOT NULL,
    trans_time        TIME            NOT NULL,
    trans_type        CHAR(2)         NOT NULL,
    security_id       CHAR(12)        NOT NULL,
    quantity          DECIMAL(15,3)   NOT NULL,
    price             DECIMAL(15,3)   NOT NULL,
    amount            DECIMAL(15,2)   NOT NULL,
    fees              DECIMAL(15,2)   NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)   NOT NULL,
    cost_basis        DECIMAL(15,2)   NOT NULL,
    gain_loss         DECIMAL(15,2)   NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_position_history PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
) PARTITION BY RANGE (trans_date);

COMMENT ON TABLE position_history IS 'Position History - Migrated from DB2 POSHIST table';
COMMENT ON COLUMN position_history.trans_type IS 'Transaction Type: BU=Buy, SL=Sell, TR=Transfer';

-- Create quarterly partitions for 2024
CREATE TABLE position_history_2024_q1 PARTITION OF position_history
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE position_history_2024_q2 PARTITION OF position_history
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
CREATE TABLE position_history_2024_q3 PARTITION OF position_history
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');
CREATE TABLE position_history_2024_q4 PARTITION OF position_history
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

-- Create partitions for 2025
CREATE TABLE position_history_2025_q1 PARTITION OF position_history
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE position_history_2025_q2 PARTITION OF position_history
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE position_history_2025_q3 PARTITION OF position_history
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE position_history_2025_q4 PARTITION OF position_history
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');

-- Create partitions for 2026
CREATE TABLE position_history_2026_q1 PARTITION OF position_history
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE position_history_2026_q2 PARTITION OF position_history
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE position_history_2026_q3 PARTITION OF position_history
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE position_history_2026_q4 PARTITION OF position_history
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');

CREATE INDEX idx_poshist_security ON position_history (security_id, trans_date);
CREATE INDEX idx_poshist_process ON position_history (process_date, program_id);

-- ====================================================================
-- ERROR LOG TABLE
-- Migrated from: ERRLOG.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS error_log (
    id                SERIAL          PRIMARY KEY,
    error_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    program_id        CHAR(8)         NOT NULL,
    error_type        CHAR(1)         NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        CHAR(8)         NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    process_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
    process_time      TIME            NOT NULL DEFAULT CURRENT_TIME,
    user_id           CHAR(8)         NOT NULL,
    additional_info   VARCHAR(500)
);

COMMENT ON TABLE error_log IS 'Error Log - Migrated from DB2 ERRLOG table';
COMMENT ON COLUMN error_log.error_type IS 'Error Type: S=System, A=Application, D=Data';
COMMENT ON COLUMN error_log.error_severity IS 'Severity: 1=Info, 2=Warning, 3=Error, 4=Severe';

CREATE INDEX idx_errlog_timestamp ON error_log (error_timestamp, program_id);
CREATE INDEX idx_errlog_severity ON error_log (process_date, error_severity DESC);

-- ====================================================================
-- AUTHENTICATION FILE TABLE
-- Migrated from: AUTHFILE (referenced in documentation)
-- ====================================================================
CREATE TABLE IF NOT EXISTS auth_permissions (
    user_id           VARCHAR(8)      NOT NULL,
    resource_type     CHAR(2)         NOT NULL,
    resource_id       VARCHAR(20)     NOT NULL,
    permission_level  CHAR(1)         NOT NULL,
    granted_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
    granted_by        VARCHAR(8)      NOT NULL,
    expiry_date       DATE,
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_auth_permissions PRIMARY KEY (user_id, resource_type, resource_id)
);

COMMENT ON TABLE auth_permissions IS 'Authentication Permissions - Replaces AUTHFILE';
COMMENT ON COLUMN auth_permissions.resource_type IS 'Resource Type: PT=Portfolio, RP=Report, SY=System';
COMMENT ON COLUMN auth_permissions.permission_level IS 'Permission: R=Read, W=Write, A=Admin';
COMMENT ON COLUMN auth_permissions.status IS 'Status: A=Active, I=Inactive, R=Revoked';

CREATE INDEX idx_auth_resource ON auth_permissions (resource_type, resource_id);
CREATE INDEX idx_auth_status ON auth_permissions (status, expiry_date);

-- ====================================================================
-- AUDIT LOG TABLE
-- Migrated from: AUDITLOG (referenced in documentation)
-- ====================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id          SERIAL          PRIMARY KEY,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id           VARCHAR(8)      NOT NULL,
    action_type       CHAR(2)         NOT NULL,
    resource_type     CHAR(2)         NOT NULL,
    resource_id       VARCHAR(20)     NOT NULL,
    action_detail     VARCHAR(500),
    ip_address        VARCHAR(45),
    session_id        VARCHAR(50),
    status            CHAR(1)         NOT NULL DEFAULT 'S',
    error_message     VARCHAR(200)
);

COMMENT ON TABLE audit_log IS 'Audit Log - Replaces AUDITLOG for security audit trail';
COMMENT ON COLUMN audit_log.action_type IS 'Action: LI=Login, LO=Logout, RD=Read, WR=Write, DL=Delete';
COMMENT ON COLUMN audit_log.status IS 'Status: S=Success, F=Failure';

CREATE INDEX idx_audit_timestamp ON audit_log (audit_timestamp);
CREATE INDEX idx_audit_user ON audit_log (user_id, audit_timestamp);
CREATE INDEX idx_audit_resource ON audit_log (resource_type, resource_id, audit_timestamp);

-- ====================================================================
-- VIEWS
-- Migrated from: db2-definitions.sql
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

-- ====================================================================
-- STORED PROCEDURES
-- Migrated from: ERRLOG.sql
-- ====================================================================
CREATE OR REPLACE FUNCTION errlog_cleanup(retention_days INTEGER)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM error_log
    WHERE process_date < CURRENT_DATE - retention_days;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION errlog_cleanup IS 'Cleanup old error log entries based on retention period';
