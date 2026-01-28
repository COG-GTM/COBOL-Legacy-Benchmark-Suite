-- Migration: 001_initial_schema.sql
-- Description: Initial PostgreSQL schema migrated from DB2 for z/OS
-- Source: src/database/db2/*.sql
-- Date: 2026-01-28

-- ============================================================================
-- POSITION HISTORY TABLE (from POSHIST.sql)
-- Stores historical position and transaction data for reporting
-- ============================================================================

CREATE TABLE IF NOT EXISTS poshist (
    account_no          CHAR(8)         NOT NULL,
    portfolio_id        CHAR(10)        NOT NULL,
    trans_date          DATE            NOT NULL,
    trans_time          TIME            NOT NULL,
    trans_type          CHAR(2)         NOT NULL,
    security_id         CHAR(12)        NOT NULL,
    quantity            NUMERIC(15,3)   NOT NULL,
    price               NUMERIC(15,3)   NOT NULL,
    amount              NUMERIC(15,2)   NOT NULL,
    fees                NUMERIC(15,2)   NOT NULL DEFAULT 0,
    total_amount        NUMERIC(15,2)   NOT NULL,
    cost_basis          NUMERIC(15,2)   NOT NULL,
    gain_loss           NUMERIC(15,2)   NOT NULL,
    process_date        DATE            NOT NULL,
    process_time        TIME            NOT NULL,
    program_id          CHAR(8)         NOT NULL,
    user_id             CHAR(8)         NOT NULL,
    audit_timestamp     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_poshist PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

-- Indexes for POSHIST (equivalent to DB2 secondary indexes)
CREATE INDEX idx_poshist_security ON poshist (security_id, trans_date);
CREATE INDEX idx_poshist_process ON poshist (process_date, process_time);

-- Partitioning by trans_date (PostgreSQL native partitioning)
-- Note: In production, create partitioned table with quarterly partitions
COMMENT ON TABLE poshist IS 'Position history table - migrated from DB2 POSHIST';

-- ============================================================================
-- ERROR LOG TABLE (from ERRLOG.sql)
-- Stores application error information for troubleshooting
-- ============================================================================

CREATE TABLE IF NOT EXISTS error_log (
    error_timestamp     TIMESTAMP       NOT NULL,
    program_id          CHAR(8)         NOT NULL,
    error_type          CHAR(1)         NOT NULL,
    error_severity      INTEGER         NOT NULL,
    error_code          CHAR(8)         NOT NULL,
    error_message       VARCHAR(200)    NOT NULL,
    process_date        DATE            NOT NULL,
    process_time        TIME            NOT NULL,
    user_id             CHAR(8)         NOT NULL,
    additional_info     VARCHAR(500),
    
    CONSTRAINT pk_error_log PRIMARY KEY (error_timestamp, program_id)
);

-- Index for error log queries
CREATE INDEX idx_error_log_severity ON error_log (process_date, error_severity);

COMMENT ON TABLE error_log IS 'Error logging table - migrated from DB2 ERRLOG';

-- ============================================================================
-- AUTHORIZATION FILE TABLE (from db2-definitions.sql)
-- Stores user authorization and access control information
-- ============================================================================

CREATE TABLE IF NOT EXISTS auth_file (
    user_id             CHAR(8)         NOT NULL,
    resource_type       CHAR(8)         NOT NULL,
    resource_id         VARCHAR(50)     NOT NULL,
    access_level        CHAR(1)         NOT NULL,
    granted_by          CHAR(8)         NOT NULL,
    granted_date        DATE            NOT NULL,
    expiry_date         DATE,
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    last_access_date    DATE,
    last_access_time    TIME,
    
    CONSTRAINT pk_auth_file PRIMARY KEY (user_id, resource_type, resource_id)
);

-- Index for authorization lookups
CREATE INDEX idx_auth_file_resource ON auth_file (resource_type, resource_id);

COMMENT ON TABLE auth_file IS 'User authorization table - migrated from DB2 AUTHFILE';

-- ============================================================================
-- AUDIT LOG TABLE (from db2-definitions.sql)
-- Stores security audit trail for compliance
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_log (
    audit_timestamp     TIMESTAMP       NOT NULL,
    user_id             CHAR(8)         NOT NULL,
    action_type         CHAR(4)         NOT NULL,
    resource_type       CHAR(8)         NOT NULL,
    resource_id         VARCHAR(50)     NOT NULL,
    action_result       CHAR(1)         NOT NULL,
    client_info         VARCHAR(100),
    session_id          CHAR(16),
    details             VARCHAR(500),
    
    CONSTRAINT pk_audit_log PRIMARY KEY (audit_timestamp, user_id)
);

-- Index for audit queries
CREATE INDEX idx_audit_log_action ON audit_log (action_type, audit_timestamp);
CREATE INDEX idx_audit_log_resource ON audit_log (resource_type, resource_id);

COMMENT ON TABLE audit_log IS 'Security audit log - migrated from DB2 AUDITLOG';

-- ============================================================================
-- PORTFOLIO MASTER TABLE (from db2-definitions.sql)
-- Stores portfolio definitions and metadata
-- ============================================================================

CREATE TABLE IF NOT EXISTS portfolio_master (
    portfolio_id        CHAR(10)        NOT NULL,
    account_no          CHAR(8)         NOT NULL,
    account_type        CHAR(2)         NOT NULL,
    branch_id           CHAR(4)         NOT NULL,
    portfolio_name      VARCHAR(50)     NOT NULL,
    portfolio_type      CHAR(2)         NOT NULL,
    currency_code       CHAR(3)         NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    open_date           DATE            NOT NULL,
    close_date          DATE,
    manager_id          CHAR(8),
    risk_profile        CHAR(1),
    benchmark_id        CHAR(10),
    last_maint_date     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user     CHAR(8)         NOT NULL,
    
    CONSTRAINT pk_portfolio_master PRIMARY KEY (portfolio_id)
);

-- Indexes for portfolio lookups
CREATE INDEX idx_portfolio_account ON portfolio_master (account_no, account_type);
CREATE INDEX idx_portfolio_branch ON portfolio_master (branch_id);
CREATE INDEX idx_portfolio_manager ON portfolio_master (manager_id);

COMMENT ON TABLE portfolio_master IS 'Portfolio master table - migrated from DB2 PORTFOLIO_MASTER and VSAM PORTMSTR';

-- ============================================================================
-- INVESTMENT POSITIONS TABLE (from db2-definitions.sql)
-- Stores current investment positions for each portfolio
-- ============================================================================

CREATE TABLE IF NOT EXISTS investment_positions (
    portfolio_id        CHAR(10)        NOT NULL,
    investment_id       CHAR(10)        NOT NULL,
    position_date       DATE            NOT NULL,
    quantity            NUMERIC(15,4)   NOT NULL,
    cost_basis          NUMERIC(15,2)   NOT NULL,
    market_value        NUMERIC(15,2)   NOT NULL,
    currency_code       CHAR(3)         NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    acquisition_date    DATE,
    last_price          NUMERIC(15,4),
    last_price_date     DATE,
    unrealized_gain     NUMERIC(15,2),
    last_maint_date     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user     CHAR(8)         NOT NULL,
    
    CONSTRAINT pk_investment_positions PRIMARY KEY (portfolio_id, investment_id, position_date)
);

-- Indexes for position queries
CREATE INDEX idx_positions_investment ON investment_positions (investment_id, position_date);
CREATE INDEX idx_positions_status ON investment_positions (status, position_date);

COMMENT ON TABLE investment_positions IS 'Investment positions table - migrated from DB2 INVESTMENT_POSITIONS';

-- ============================================================================
-- TRANSACTION HISTORY TABLE (from db2-definitions.sql)
-- Stores all financial transactions
-- ============================================================================

CREATE TABLE IF NOT EXISTS transaction_history (
    transaction_id      SERIAL          NOT NULL,
    portfolio_id        CHAR(10)        NOT NULL,
    trans_date          DATE            NOT NULL,
    trans_time          TIME            NOT NULL,
    sequence_no         CHAR(6)         NOT NULL,
    investment_id       CHAR(10)        NOT NULL,
    trans_type          CHAR(2)         NOT NULL,
    quantity            NUMERIC(15,4)   NOT NULL,
    price               NUMERIC(15,4)   NOT NULL,
    amount              NUMERIC(15,2)   NOT NULL,
    currency_code       CHAR(3)         NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'P',
    process_date        TIMESTAMP,
    process_user        CHAR(8),
    
    CONSTRAINT pk_transaction_history PRIMARY KEY (transaction_id)
);

-- Unique constraint matching VSAM key structure
CREATE UNIQUE INDEX idx_transaction_key ON transaction_history 
    (trans_date, trans_time, portfolio_id, sequence_no);

-- Indexes for transaction queries
CREATE INDEX idx_transaction_portfolio ON transaction_history (portfolio_id, trans_date);
CREATE INDEX idx_transaction_investment ON transaction_history (investment_id, trans_date);
CREATE INDEX idx_transaction_status ON transaction_history (status, trans_date);

COMMENT ON TABLE transaction_history IS 'Transaction history table - migrated from DB2 TRANSACTION_HISTORY and VSAM TRANHIST';

-- ============================================================================
-- LOOKUP TABLES
-- Reference data for codes and descriptions
-- ============================================================================

-- Transaction Type Codes
CREATE TABLE IF NOT EXISTS trans_type_codes (
    trans_type          CHAR(2)         NOT NULL,
    description         VARCHAR(50)     NOT NULL,
    debit_credit        CHAR(1)         NOT NULL,
    
    CONSTRAINT pk_trans_type PRIMARY KEY (trans_type)
);

INSERT INTO trans_type_codes (trans_type, description, debit_credit) VALUES
    ('BU', 'Buy', 'D'),
    ('SL', 'Sell', 'C'),
    ('TR', 'Transfer', 'N'),
    ('FE', 'Fee', 'D'),
    ('DV', 'Dividend', 'C'),
    ('IN', 'Interest', 'C'),
    ('DP', 'Deposit', 'C'),
    ('WD', 'Withdrawal', 'D');

-- Status Codes
CREATE TABLE IF NOT EXISTS status_codes (
    status_type         CHAR(8)         NOT NULL,
    status_code         CHAR(1)         NOT NULL,
    description         VARCHAR(50)     NOT NULL,
    
    CONSTRAINT pk_status PRIMARY KEY (status_type, status_code)
);

INSERT INTO status_codes (status_type, status_code, description) VALUES
    ('TRANS', 'P', 'Pending'),
    ('TRANS', 'D', 'Done/Completed'),
    ('TRANS', 'F', 'Failed'),
    ('TRANS', 'R', 'Reversed'),
    ('POSITION', 'A', 'Active'),
    ('POSITION', 'C', 'Closed'),
    ('POSITION', 'P', 'Pending'),
    ('PORTFOLIO', 'A', 'Active'),
    ('PORTFOLIO', 'C', 'Closed'),
    ('PORTFOLIO', 'S', 'Suspended'),
    ('AUTH', 'A', 'Active'),
    ('AUTH', 'I', 'Inactive'),
    ('AUTH', 'E', 'Expired');

-- Error Severity Codes
CREATE TABLE IF NOT EXISTS error_severity_codes (
    severity_code       INTEGER         NOT NULL,
    severity_name       VARCHAR(20)     NOT NULL,
    description         VARCHAR(100)    NOT NULL,
    
    CONSTRAINT pk_error_severity PRIMARY KEY (severity_code)
);

INSERT INTO error_severity_codes (severity_code, severity_name, description) VALUES
    (0, 'SUCCESS', 'Operation completed successfully'),
    (4, 'WARNING', 'Operation completed with warnings'),
    (8, 'ERROR', 'Operation failed with recoverable error'),
    (12, 'SEVERE', 'Operation failed with severe error'),
    (16, 'TERMINAL', 'Operation failed with terminal error - immediate abort');

COMMENT ON TABLE trans_type_codes IS 'Transaction type reference data';
COMMENT ON TABLE status_codes IS 'Status code reference data';
COMMENT ON TABLE error_severity_codes IS 'Error severity reference data';
