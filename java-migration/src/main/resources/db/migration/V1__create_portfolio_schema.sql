-- =====================================================================
-- Portfolio Management System - PostgreSQL Schema Migration
-- Version: 1.0
-- Migrated from: COBOL VSAM/DB2 to PostgreSQL
-- =====================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================================
-- PORTFOLIO MASTER TABLE
-- Migrated from: VSAM PORTMSTR (KSDS, 400 bytes)
-- Key: Portfolio ID + Account Type + Branch ID
-- =====================================================================
CREATE TABLE portfolio_master (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id VARCHAR(8) NOT NULL,
    account_type VARCHAR(2) NOT NULL,
    branch_id VARCHAR(2) NOT NULL,
    account_no VARCHAR(10) NOT NULL,
    client_id VARCHAR(10) NOT NULL,
    client_name VARCHAR(30) NOT NULL,
    client_type CHAR(1) NOT NULL CHECK (client_type IN ('I', 'C', 'T')),
    portfolio_name VARCHAR(50),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    risk_level CHAR(1) CHECK (risk_level IN ('L', 'M', 'H')),
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'C', 'S')),
    total_value DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    cash_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    open_date DATE NOT NULL,
    close_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_portfolio_master_key UNIQUE (portfolio_id, account_type, branch_id)
);

COMMENT ON TABLE portfolio_master IS 'Portfolio Master - Migrated from VSAM PORTMSTR';
COMMENT ON COLUMN portfolio_master.client_type IS 'I=Individual, C=Corporate, T=Trust';
COMMENT ON COLUMN portfolio_master.status IS 'A=Active, C=Closed, S=Suspended';
COMMENT ON COLUMN portfolio_master.total_value IS 'Total portfolio value - COMP-3 S9(13)V99';
COMMENT ON COLUMN portfolio_master.cash_balance IS 'Cash balance - COMP-3 S9(13)V99';

-- Indexes for common query patterns
CREATE INDEX idx_portfolio_master_client ON portfolio_master (client_id, status);
CREATE INDEX idx_portfolio_master_status ON portfolio_master (status, open_date);
CREATE INDEX idx_portfolio_master_account ON portfolio_master (account_no);

-- =====================================================================
-- INVESTMENT POSITIONS TABLE
-- Migrated from: VSAM POSHIST (KSDS, 350 bytes)
-- Key: Portfolio ID + Position Date + Investment ID
-- =====================================================================
CREATE TABLE investment_positions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id VARCHAR(8) NOT NULL,
    investment_id VARCHAR(10) NOT NULL,
    position_date DATE NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL DEFAULT 0.0000,
    cost_basis DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    market_value DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    average_cost DECIMAL(15, 4) NOT NULL DEFAULT 0.0000,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    status CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'C', 'P')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_investment_positions_key UNIQUE (portfolio_id, investment_id, position_date)
);

COMMENT ON TABLE investment_positions IS 'Investment Positions - Migrated from VSAM POSFILE';
COMMENT ON COLUMN investment_positions.status IS 'A=Active, C=Closed, P=Pending';
COMMENT ON COLUMN investment_positions.quantity IS 'Holding quantity - COMP-3 S9(11)V9(4)';
COMMENT ON COLUMN investment_positions.cost_basis IS 'Total cost basis - COMP-3 S9(13)V9(2)';
COMMENT ON COLUMN investment_positions.market_value IS 'Current market value - COMP-3 S9(13)V9(2)';

-- Indexes for common query patterns
CREATE INDEX idx_positions_portfolio ON investment_positions (portfolio_id, position_date);
CREATE INDEX idx_positions_date ON investment_positions (position_date, portfolio_id);
CREATE INDEX idx_positions_investment ON investment_positions (investment_id, position_date);

-- =====================================================================
-- TRANSACTION RECORDS TABLE
-- Migrated from: VSAM TRANHIST (KSDS, 300 bytes)
-- Key: Transaction Date + Time + Portfolio ID + Sequence Number
-- =====================================================================
CREATE TABLE transaction_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(20) NOT NULL,
    portfolio_id VARCHAR(8) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL,
    sequence_no VARCHAR(6) NOT NULL,
    investment_id VARCHAR(10) NOT NULL,
    transaction_type CHAR(2) NOT NULL CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    quantity DECIMAL(15, 4) NOT NULL DEFAULT 0.0000,
    price DECIMAL(15, 4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    status CHAR(1) NOT NULL DEFAULT 'P' CHECK (status IN ('P', 'D', 'F', 'R')),
    before_balance DECIMAL(15, 4),
    after_balance DECIMAL(15, 4),
    result_code VARCHAR(4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_transaction_records_key UNIQUE (transaction_date, transaction_time, portfolio_id, sequence_no)
);

COMMENT ON TABLE transaction_records IS 'Transaction Records - Migrated from VSAM TRANFILE';
COMMENT ON COLUMN transaction_records.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN transaction_records.status IS 'P=Pending, D=Done, F=Failed, R=Reversed';
COMMENT ON COLUMN transaction_records.quantity IS 'Transaction quantity - COMP-3 S9(11)V9(4)';
COMMENT ON COLUMN transaction_records.price IS 'Transaction price - COMP-3 S9(11)V9(4)';
COMMENT ON COLUMN transaction_records.amount IS 'Transaction amount - COMP-3 S9(13)V9(2)';

-- Indexes for common query patterns
CREATE INDEX idx_transactions_portfolio ON transaction_records (portfolio_id, transaction_date);
CREATE INDEX idx_transactions_date ON transaction_records (transaction_date, portfolio_id);
CREATE INDEX idx_transactions_status ON transaction_records (status, transaction_date);
CREATE INDEX idx_transactions_investment ON transaction_records (investment_id, transaction_date);

-- =====================================================================
-- POSITION HISTORY TABLE (Partitioned)
-- Migrated from: DB2 POSHIST with quarterly partitioning
-- =====================================================================
CREATE TABLE position_history (
    id UUID DEFAULT uuid_generate_v4(),
    account_no VARCHAR(8) NOT NULL,
    portfolio_id VARCHAR(10) NOT NULL,
    trans_date DATE NOT NULL,
    trans_time TIME NOT NULL,
    trans_type CHAR(2) NOT NULL CHECK (trans_type IN ('BU', 'SL', 'TR', 'FE')),
    security_id VARCHAR(12) NOT NULL,
    quantity DECIMAL(15, 3) NOT NULL DEFAULT 0.000,
    price DECIMAL(15, 3) NOT NULL DEFAULT 0.000,
    amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    fees DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    cost_basis DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    gain_loss DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    process_date DATE NOT NULL,
    process_time TIME NOT NULL,
    program_id VARCHAR(8) NOT NULL,
    user_id VARCHAR(8) NOT NULL,
    audit_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, trans_date)
) PARTITION BY RANGE (trans_date);

COMMENT ON TABLE position_history IS 'Position History - Migrated from DB2 POSHIST with quarterly partitioning';
COMMENT ON COLUMN position_history.trans_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';

-- Create quarterly partitions for 2024-2025
CREATE TABLE position_history_2024_q1 PARTITION OF position_history
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE position_history_2024_q2 PARTITION OF position_history
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
CREATE TABLE position_history_2024_q3 PARTITION OF position_history
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');
CREATE TABLE position_history_2024_q4 PARTITION OF position_history
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
CREATE TABLE position_history_2025_q1 PARTITION OF position_history
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE position_history_2025_q2 PARTITION OF position_history
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE position_history_2025_q3 PARTITION OF position_history
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE position_history_2025_q4 PARTITION OF position_history
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');

-- Indexes for position history
CREATE INDEX idx_poshist_account ON position_history (account_no, portfolio_id, trans_date);
CREATE INDEX idx_poshist_security ON position_history (security_id, trans_date);
CREATE INDEX idx_poshist_process ON position_history (process_date, program_id);

-- =====================================================================
-- ERROR LOG TABLE
-- Migrated from: DB2 ERRLOG
-- =====================================================================
CREATE TABLE error_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    error_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    program_id VARCHAR(8) NOT NULL,
    error_type CHAR(1) NOT NULL CHECK (error_type IN ('S', 'A', 'D')),
    error_severity INTEGER NOT NULL CHECK (error_severity BETWEEN 1 AND 4),
    error_code VARCHAR(8) NOT NULL,
    error_message VARCHAR(200) NOT NULL,
    process_date DATE NOT NULL,
    process_time TIME NOT NULL,
    user_id VARCHAR(8) NOT NULL,
    additional_info VARCHAR(500),
    CONSTRAINT uk_error_log_key UNIQUE (error_timestamp, program_id)
);

COMMENT ON TABLE error_log IS 'Error Log - Migrated from DB2 ERRLOG';
COMMENT ON COLUMN error_log.error_type IS 'S=System, A=Application, D=Data';
COMMENT ON COLUMN error_log.error_severity IS '1=Info, 2=Warning, 3=Error, 4=Severe';

-- Indexes for error log
CREATE INDEX idx_error_log_date ON error_log (process_date, error_severity DESC);
CREATE INDEX idx_error_log_program ON error_log (program_id, error_timestamp);

-- =====================================================================
-- RETURN CODES TABLE
-- Migrated from: DB2 RTNCODES
-- =====================================================================
CREATE TABLE return_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    log_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    program_id VARCHAR(8) NOT NULL,
    return_code INTEGER NOT NULL,
    highest_code INTEGER NOT NULL,
    status_code CHAR(1) NOT NULL CHECK (status_code IN ('S', 'W', 'E', 'F')),
    message_text VARCHAR(80),
    CONSTRAINT uk_return_codes_key UNIQUE (log_timestamp, program_id)
);

COMMENT ON TABLE return_codes IS 'Return Codes - Migrated from DB2 RTNCODES';
COMMENT ON COLUMN return_codes.status_code IS 'S=Success, W=Warning, E=Error, F=Severe';

-- Indexes for return codes
CREATE INDEX idx_return_codes_program ON return_codes (program_id, log_timestamp);
CREATE INDEX idx_return_codes_status ON return_codes (status_code, log_timestamp);

-- =====================================================================
-- AUDIT LOG TABLE
-- Migrated from: COBOL AUDITLOG copybook
-- =====================================================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    audit_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    system_id VARCHAR(8) NOT NULL,
    user_id VARCHAR(8) NOT NULL,
    program_id VARCHAR(8) NOT NULL,
    terminal_id VARCHAR(8),
    audit_type VARCHAR(4) NOT NULL CHECK (audit_type IN ('TRAN', 'USER', 'SYST')),
    action VARCHAR(8) NOT NULL,
    status VARCHAR(4) NOT NULL CHECK (status IN ('SUCC', 'FAIL', 'WARN')),
    portfolio_id VARCHAR(8),
    account_no VARCHAR(10),
    before_image TEXT,
    after_image TEXT,
    message VARCHAR(100)
);

COMMENT ON TABLE audit_log IS 'Audit Log - Migrated from COBOL AUDITLOG copybook';
COMMENT ON COLUMN audit_log.audit_type IS 'TRAN=Transaction, USER=User Action, SYST=System Event';
COMMENT ON COLUMN audit_log.action IS 'CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN';
COMMENT ON COLUMN audit_log.status IS 'SUCC=Success, FAIL=Failure, WARN=Warning';

-- Indexes for audit log
CREATE INDEX idx_audit_log_timestamp ON audit_log (audit_timestamp);
CREATE INDEX idx_audit_log_user ON audit_log (user_id, audit_timestamp);
CREATE INDEX idx_audit_log_portfolio ON audit_log (portfolio_id, audit_timestamp);
CREATE INDEX idx_audit_log_type ON audit_log (audit_type, action, audit_timestamp);

-- =====================================================================
-- BATCH CONTROL TABLE
-- Migrated from: VSAM BCHCTL
-- =====================================================================
CREATE TABLE batch_control (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    process_date DATE NOT NULL,
    process_id VARCHAR(8) NOT NULL,
    status CHAR(1) NOT NULL DEFAULT 'W' CHECK (status IN ('W', 'P', 'C', 'E')),
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    record_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    last_position BIGINT NOT NULL DEFAULT 0,
    return_code INTEGER NOT NULL DEFAULT 0,
    message VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_control_key UNIQUE (process_date, process_id)
);

COMMENT ON TABLE batch_control IS 'Batch Control - Migrated from VSAM BCHCTL';
COMMENT ON COLUMN batch_control.status IS 'W=Waiting, P=In Process, C=Complete, E=Error';

-- Index for batch control
CREATE INDEX idx_batch_control_status ON batch_control (status, process_date);

-- =====================================================================
-- CHECKPOINT TABLE
-- For Spring Batch checkpoint/restart support
-- =====================================================================
CREATE TABLE checkpoint_record (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    process_date DATE NOT NULL,
    process_id VARCHAR(8) NOT NULL,
    last_trans_id VARCHAR(12),
    last_account VARCHAR(9),
    last_fund VARCHAR(6),
    records_processed BIGINT NOT NULL DEFAULT 0,
    checkpoint_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkpoint_key UNIQUE (process_date, process_id)
);

COMMENT ON TABLE checkpoint_record IS 'Checkpoint Record - For batch restart support';

-- =====================================================================
-- VIEWS
-- =====================================================================

-- Active Portfolios View
CREATE VIEW active_portfolios AS
SELECT *
FROM portfolio_master
WHERE status = 'A'
  AND (close_date IS NULL OR close_date > CURRENT_DATE);

-- Current Positions View
CREATE VIEW current_positions AS
SELECT p.*, pm.portfolio_name, pm.client_id, pm.client_name
FROM investment_positions p
         JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day'
  AND p.status = 'A';

-- Recent Transactions View
CREATE VIEW recent_transactions AS
SELECT *
FROM transaction_records
WHERE transaction_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY transaction_date DESC, transaction_time DESC;

-- =====================================================================
-- FUNCTIONS
-- =====================================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_portfolio_master_updated_at
    BEFORE UPDATE ON portfolio_master
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_investment_positions_updated_at
    BEFORE UPDATE ON investment_positions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transaction_records_updated_at
    BEFORE UPDATE ON transaction_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_batch_control_updated_at
    BEFORE UPDATE ON batch_control
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
