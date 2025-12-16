-- ============================================================================
-- PostgreSQL Schema Migration for Investment Portfolio Management System
-- Phase 1: Database Migration from VSAM/DB2 to PostgreSQL
-- Version: 1.0.0
-- Date: 2024
-- ============================================================================

-- Create schema for portfolio management
CREATE SCHEMA IF NOT EXISTS portfolio;

-- Set search path
SET search_path TO portfolio, public;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

-- Transaction types (mapped from COBOL: BU=Buy, SL=Sell, TR=Transfer, FE=Fee)
CREATE TYPE transaction_type AS ENUM ('BUY', 'SELL', 'TRANSFER', 'FEE');

-- Transaction status (mapped from COBOL: P=Pending, D=Done, F=Failed, R=Reversed)
CREATE TYPE transaction_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED');

-- Position status (mapped from COBOL: A=Active, C=Closed, P=Pending)
CREATE TYPE position_status AS ENUM ('ACTIVE', 'CLOSED', 'PENDING');

-- Portfolio status
CREATE TYPE portfolio_status AS ENUM ('ACTIVE', 'CLOSED', 'SUSPENDED');

-- Error types (mapped from COBOL: S=System, A=Application, D=Data)
CREATE TYPE error_type AS ENUM ('SYSTEM', 'APPLICATION', 'DATA');

-- Error severity levels
CREATE TYPE error_severity AS ENUM ('INFO', 'WARNING', 'ERROR', 'SEVERE');

-- Audit action types (mapped from COBOL AUDITLOG copybook)
CREATE TYPE audit_action AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'INQUIRE', 'LOGIN', 'LOGOUT', 'STARTUP', 'SHUTDOWN');

-- Audit event types
CREATE TYPE audit_event_type AS ENUM ('TRANSACTION', 'USER_ACTION', 'SYSTEM_EVENT');

-- Audit status
CREATE TYPE audit_status AS ENUM ('SUCCESS', 'FAILURE', 'WARNING');

-- ============================================================================
-- TABLE: users (migrated from AUTHFILE DB2 table)
-- Source: SECMGR.cbl - AUTHFILE table for user authorization
-- ============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(8) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    department VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER DEFAULT 0,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(8),
    updated_by VARCHAR(8)
);

-- User authorization table (maps to AUTHFILE structure in SECMGR.cbl)
CREATE TABLE user_authorizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(8) NOT NULL REFERENCES users(user_id),
    resource VARCHAR(8) NOT NULL,
    access_type VARCHAR(8) NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by VARCHAR(8),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(user_id, resource, access_type)
);

-- ============================================================================
-- TABLE: portfolios (migrated from PORTMSTR VSAM file)
-- Source: VSAM PORTMSTR - Portfolio Master File
-- Key: Portfolio ID (8 bytes) + Account Type (2 bytes) + Branch ID (2 bytes)
-- ============================================================================
CREATE TABLE portfolios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id VARCHAR(8) NOT NULL,
    account_type VARCHAR(2) NOT NULL,
    branch_id VARCHAR(2) NOT NULL,
    client_id VARCHAR(10) NOT NULL,
    portfolio_name VARCHAR(50) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    risk_level CHAR(1) NOT NULL DEFAULT 'M',
    status portfolio_status NOT NULL DEFAULT 'ACTIVE',
    open_date DATE NOT NULL,
    close_date DATE,
    total_value DECIMAL(18, 2) DEFAULT 0,
    total_cost_basis DECIMAL(18, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(8),
    updated_by VARCHAR(8),
    UNIQUE(portfolio_id, account_type, branch_id)
);

-- ============================================================================
-- TABLE: positions (migrated from POSFILE VSAM file)
-- Source: VSAM POSFILE - Portfolio Position File (PORTFOLIO.POSITION.VSAM)
-- Key: Portfolio ID (8 bytes) + Position Date (8 bytes) + Investment ID (10 bytes)
-- Mapped from POSREC.cpy copybook structure
-- ============================================================================
CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id VARCHAR(8) NOT NULL,
    position_date DATE NOT NULL,
    investment_id VARCHAR(10) NOT NULL,
    cusip VARCHAR(9),
    quantity DECIMAL(15, 4) NOT NULL DEFAULT 0,
    cost_basis DECIMAL(15, 2) NOT NULL DEFAULT 0,
    market_value DECIMAL(15, 2) NOT NULL DEFAULT 0,
    average_cost DECIMAL(15, 4) DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    status position_status NOT NULL DEFAULT 'ACTIVE',
    last_transaction_id VARCHAR(20),
    last_transaction_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(8),
    updated_by VARCHAR(8),
    UNIQUE(portfolio_id, position_date, investment_id)
);

-- ============================================================================
-- TABLE: transactions (migrated from TRANHIST VSAM file)
-- Source: VSAM TRANHIST - Transaction History File
-- Key: Transaction Date (8 bytes) + Time (6 bytes) + Portfolio ID (8 bytes) + Sequence (6 bytes)
-- Mapped from TRNREC.cpy copybook structure
-- ============================================================================
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(20) NOT NULL UNIQUE,
    portfolio_id VARCHAR(8) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL,
    sequence_no VARCHAR(6) NOT NULL,
    investment_id VARCHAR(10) NOT NULL,
    transaction_type transaction_type NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    price DECIMAL(15, 4) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    fees DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    status transaction_status NOT NULL DEFAULT 'PENDING',
    before_balance DECIMAL(15, 4),
    after_balance DECIMAL(15, 4),
    result_code VARCHAR(4),
    process_date TIMESTAMP WITH TIME ZONE,
    process_user VARCHAR(8),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- TABLE: position_history (migrated from POSHIST DB2 table)
-- Source: DB2 POSHIST table - Position History for reporting
-- Mapped from POSHIST.sql and DBTBLS.cpy
-- ============================================================================
CREATE TABLE position_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_no VARCHAR(8) NOT NULL,
    portfolio_id VARCHAR(10) NOT NULL,
    trans_date DATE NOT NULL,
    trans_time TIME NOT NULL,
    trans_type VARCHAR(2) NOT NULL,
    security_id VARCHAR(12) NOT NULL,
    quantity DECIMAL(15, 3) NOT NULL,
    price DECIMAL(15, 3) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    fees DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    cost_basis DECIMAL(15, 2) NOT NULL,
    gain_loss DECIMAL(15, 2) NOT NULL,
    process_date DATE NOT NULL,
    process_time TIME NOT NULL,
    program_id VARCHAR(8) NOT NULL,
    user_id VARCHAR(8) NOT NULL,
    audit_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE(account_no, portfolio_id, trans_date, trans_time)
);

-- ============================================================================
-- TABLE: error_log (migrated from ERRLOG DB2 table)
-- Source: DB2 ERRLOG table - Error Logging
-- Mapped from ERRLOG.sql and DBTBLS.cpy
-- ============================================================================
CREATE TABLE error_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    error_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    program_id VARCHAR(8) NOT NULL,
    error_type error_type NOT NULL,
    error_severity error_severity NOT NULL,
    error_code VARCHAR(8) NOT NULL,
    error_message VARCHAR(200) NOT NULL,
    process_date DATE NOT NULL,
    process_time TIME NOT NULL,
    user_id VARCHAR(8) NOT NULL,
    additional_info TEXT,
    stack_trace TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(8),
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(error_timestamp, program_id)
);

-- ============================================================================
-- TABLE: audit_log (migrated from AUDITLOG DB2 table)
-- Source: SECMGR.cbl - AUDITLOG table insert
-- Mapped from AUDITLOG.cpy copybook structure
-- ============================================================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audit_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    system_id VARCHAR(8),
    user_id VARCHAR(8) NOT NULL,
    program_id VARCHAR(8),
    terminal_id VARCHAR(8),
    transaction_id VARCHAR(4),
    event_type audit_event_type NOT NULL,
    action audit_action NOT NULL,
    status audit_status NOT NULL,
    portfolio_id VARCHAR(8),
    account_no VARCHAR(10),
    resource_name VARCHAR(50),
    access_type VARCHAR(8),
    before_image TEXT,
    after_image TEXT,
    message TEXT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- TABLE: batch_control (migrated from BCHCTL VSAM file)
-- Source: VSAM BCHCTL - Batch Control File
-- Mapped from BCHCTL.cpy copybook structure
-- ============================================================================
CREATE TABLE batch_control (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    process_date DATE NOT NULL,
    process_id VARCHAR(8) NOT NULL,
    status VARCHAR(1) NOT NULL DEFAULT 'W',
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    record_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    last_position INTEGER DEFAULT 0,
    return_code INTEGER DEFAULT 0,
    message VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE(process_date, process_id)
);

-- ============================================================================
-- TABLE: checkpoints (for checkpoint/restart functionality)
-- Source: Checkpoint/Restart Record from data-dictionary.md
-- ============================================================================
CREATE TABLE checkpoints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    process_date DATE NOT NULL,
    process_id VARCHAR(8) NOT NULL,
    last_transaction_id VARCHAR(12),
    last_account VARCHAR(9),
    last_fund VARCHAR(6),
    records_processed INTEGER DEFAULT 0,
    checkpoint_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(process_date, process_id, checkpoint_timestamp)
);

-- ============================================================================
-- TABLE: process_control (migrated from PRCCTL sequential file)
-- Source: PRCCTL - Process Control File
-- Mapped from data-dictionary.md
-- ============================================================================
CREATE TABLE process_control (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    process_date DATE NOT NULL,
    sequence_no INTEGER NOT NULL,
    program_id VARCHAR(8) NOT NULL,
    program_desc VARCHAR(30),
    required_return_code INTEGER DEFAULT 0,
    dependency VARCHAR(8),
    is_restartable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(process_date, sequence_no)
);

-- ============================================================================
-- TABLE: history_records (migrated from HISTREC structure)
-- Source: HISTREC.cpy - History Record Structure for change tracking
-- ============================================================================
CREATE TABLE history_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id VARCHAR(8) NOT NULL,
    history_date DATE NOT NULL,
    history_time TIME NOT NULL,
    sequence_no VARCHAR(4) NOT NULL,
    record_type VARCHAR(2) NOT NULL,
    action_code CHAR(1) NOT NULL,
    before_image TEXT,
    after_image TEXT,
    reason_code VARCHAR(4),
    process_date TIMESTAMP WITH TIME ZONE NOT NULL,
    process_user VARCHAR(8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(portfolio_id, history_date, history_time, sequence_no)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Users indexes
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_user_auth_user_id ON user_authorizations(user_id);
CREATE INDEX idx_user_auth_resource ON user_authorizations(resource);

-- Portfolios indexes
CREATE INDEX idx_portfolios_client_id ON portfolios(client_id);
CREATE INDEX idx_portfolios_status ON portfolios(status);
CREATE INDEX idx_portfolios_branch ON portfolios(branch_id);

-- Positions indexes
CREATE INDEX idx_positions_portfolio_id ON positions(portfolio_id);
CREATE INDEX idx_positions_date ON positions(position_date);
CREATE INDEX idx_positions_investment ON positions(investment_id);
CREATE INDEX idx_positions_status ON positions(status);
CREATE INDEX idx_positions_portfolio_date ON positions(portfolio_id, position_date);

-- Transactions indexes
CREATE INDEX idx_transactions_portfolio_id ON transactions(portfolio_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_investment ON transactions(investment_id);
CREATE INDEX idx_transactions_portfolio_date ON transactions(portfolio_id, transaction_date);

-- Position history indexes
CREATE INDEX idx_poshist_account ON position_history(account_no);
CREATE INDEX idx_poshist_portfolio ON position_history(portfolio_id);
CREATE INDEX idx_poshist_date ON position_history(trans_date);
CREATE INDEX idx_poshist_security ON position_history(security_id);
CREATE INDEX idx_poshist_process_date ON position_history(process_date);

-- Error log indexes
CREATE INDEX idx_errlog_timestamp ON error_log(error_timestamp);
CREATE INDEX idx_errlog_program ON error_log(program_id);
CREATE INDEX idx_errlog_severity ON error_log(error_severity);
CREATE INDEX idx_errlog_date ON error_log(process_date);
CREATE INDEX idx_errlog_code ON error_log(error_code);

-- Audit log indexes
CREATE INDEX idx_auditlog_timestamp ON audit_log(audit_timestamp);
CREATE INDEX idx_auditlog_user ON audit_log(user_id);
CREATE INDEX idx_auditlog_action ON audit_log(action);
CREATE INDEX idx_auditlog_portfolio ON audit_log(portfolio_id);
CREATE INDEX idx_auditlog_event_type ON audit_log(event_type);

-- Batch control indexes
CREATE INDEX idx_batchctl_date ON batch_control(process_date);
CREATE INDEX idx_batchctl_status ON batch_control(status);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_portfolios_updated_at
    BEFORE UPDATE ON portfolios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_batch_control_updated_at
    BEFORE UPDATE ON batch_control
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_process_control_updated_at
    BEFORE UPDATE ON process_control
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS
-- ============================================================================

-- Active portfolios view
CREATE VIEW active_portfolios AS
SELECT *
FROM portfolios
WHERE status = 'ACTIVE'
AND (close_date IS NULL OR close_date > CURRENT_DATE);

-- Current positions view (latest position for each portfolio/investment)
CREATE VIEW current_positions AS
SELECT DISTINCT ON (portfolio_id, investment_id)
    p.*,
    pf.portfolio_name,
    pf.client_id
FROM positions p
JOIN portfolios pf ON p.portfolio_id = pf.portfolio_id
WHERE p.status = 'ACTIVE'
ORDER BY portfolio_id, investment_id, position_date DESC;

-- Daily transaction summary view
CREATE VIEW daily_transaction_summary AS
SELECT
    transaction_date,
    transaction_type,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    SUM(fees) as total_fees,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_count,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count
FROM transactions
GROUP BY transaction_date, transaction_type
ORDER BY transaction_date DESC, transaction_type;

-- Error summary view
CREATE VIEW error_summary AS
SELECT
    process_date,
    program_id,
    error_severity,
    COUNT(*) as error_count
FROM error_log
WHERE process_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY process_date, program_id, error_severity
ORDER BY process_date DESC, error_count DESC;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE users IS 'User accounts migrated from AUTHFILE DB2 table';
COMMENT ON TABLE user_authorizations IS 'User resource authorizations from AUTHFILE';
COMMENT ON TABLE portfolios IS 'Portfolio master records migrated from PORTMSTR VSAM file';
COMMENT ON TABLE positions IS 'Portfolio positions migrated from POSFILE VSAM file';
COMMENT ON TABLE transactions IS 'Transaction records migrated from TRANHIST VSAM file';
COMMENT ON TABLE position_history IS 'Position history migrated from POSHIST DB2 table';
COMMENT ON TABLE error_log IS 'Error log migrated from ERRLOG DB2 table';
COMMENT ON TABLE audit_log IS 'Audit trail migrated from AUDITLOG DB2 table';
COMMENT ON TABLE batch_control IS 'Batch control records migrated from BCHCTL VSAM file';
COMMENT ON TABLE checkpoints IS 'Checkpoint/restart records for batch processing';
COMMENT ON TABLE process_control IS 'Process control records migrated from PRCCTL file';
COMMENT ON TABLE history_records IS 'Change history records from HISTREC structure';
