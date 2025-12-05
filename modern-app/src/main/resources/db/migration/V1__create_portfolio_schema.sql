-- ============================================================================
-- V1: Create Portfolio Schema
-- Description: Initial schema creation for Portfolio Modernization System
-- Replaces: VSAM PORTMSTR (Portfolio Master File)
-- Source: COBOL Copybook PORTFLIO.cpy
-- ============================================================================

-- Portfolio table (replaces VSAM PORTMSTR)
CREATE TABLE portfolio (
    portfolio_id        VARCHAR(8) PRIMARY KEY,
    account_number      VARCHAR(10) NOT NULL,
    client_id           VARCHAR(10) NOT NULL,
    client_name         VARCHAR(30) NOT NULL,
    client_type         CHAR(1) NOT NULL CHECK (client_type IN ('I', 'C', 'T')),
    portfolio_name      VARCHAR(50),
    currency_code       CHAR(3) NOT NULL DEFAULT 'USD',
    risk_level          CHAR(1),
    status              CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'C', 'S')),
    total_value         DECIMAL(15, 2),
    cash_balance        DECIMAL(15, 2),
    create_date         DATE NOT NULL,
    close_date          DATE,
    last_transaction_date DATE,
    last_maintenance_user VARCHAR(8),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for portfolio table
CREATE INDEX idx_portfolio_client ON portfolio (client_id, status);
CREATE INDEX idx_portfolio_account ON portfolio (account_number);
CREATE INDEX idx_portfolio_status ON portfolio (status, close_date);

-- Comments for documentation
COMMENT ON TABLE portfolio IS 'Portfolio Master - Replaces VSAM PORTMSTR file';
COMMENT ON COLUMN portfolio.client_type IS 'I=Individual, C=Corporate, T=Trust';
COMMENT ON COLUMN portfolio.status IS 'A=Active, C=Closed, S=Suspended';
