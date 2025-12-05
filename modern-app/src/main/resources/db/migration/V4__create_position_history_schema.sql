-- ============================================================================
-- V4: Create Position History Schema
-- Description: Position history tracking for audit and reporting
-- Replaces: DB2 POSHIST table
-- Source: DB2 POSHIST.sql
-- ============================================================================

-- Position History table (replaces DB2 POSHIST)
CREATE TABLE position_history (
    account_number      VARCHAR(8) NOT NULL,
    portfolio_id        VARCHAR(10) NOT NULL,
    transaction_date    DATE NOT NULL,
    transaction_time    TIME NOT NULL,
    transaction_type    CHAR(2) NOT NULL,
    security_id         VARCHAR(12) NOT NULL,
    quantity            DECIMAL(15, 3) NOT NULL,
    price               DECIMAL(15, 3) NOT NULL,
    amount              DECIMAL(15, 2) NOT NULL,
    fees                DECIMAL(15, 2) DEFAULT 0,
    total_amount        DECIMAL(15, 2) NOT NULL,
    cost_basis          DECIMAL(15, 2) NOT NULL,
    gain_loss           DECIMAL(15, 2) NOT NULL,
    process_date        DATE NOT NULL,
    process_time        TIME NOT NULL,
    program_id          VARCHAR(8) NOT NULL,
    user_id             VARCHAR(8) NOT NULL,
    audit_timestamp     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_number, portfolio_id, transaction_date, transaction_time)
);

-- Indexes for position_history table
CREATE INDEX idx_poshist_security ON position_history (security_id, transaction_date);
CREATE INDEX idx_poshist_process ON position_history (process_date, program_id);
CREATE INDEX idx_poshist_portfolio ON position_history (portfolio_id, transaction_date);

-- Partitioning by quarter (PostgreSQL native partitioning)
-- Note: In production, consider using table partitioning for large datasets

-- Comments for documentation
COMMENT ON TABLE position_history IS 'Position History - Replaces DB2 POSHIST table';
COMMENT ON COLUMN position_history.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer';
