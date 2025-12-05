-- ============================================================================
-- V2: Create Position Schema
-- Description: Position tracking tables for investment holdings
-- Replaces: VSAM POSHIST (Position History File)
-- Source: COBOL Copybook POSREC.cpy
-- ============================================================================

-- Position table (replaces VSAM position records)
CREATE TABLE position (
    portfolio_id        VARCHAR(8) NOT NULL,
    position_date       DATE NOT NULL,
    investment_id       VARCHAR(10) NOT NULL,
    quantity            DECIMAL(15, 4) NOT NULL,
    cost_basis          DECIMAL(15, 2) NOT NULL,
    market_value        DECIMAL(15, 2) NOT NULL,
    currency_code       CHAR(3) NOT NULL DEFAULT 'USD',
    status              CHAR(1) NOT NULL DEFAULT 'A' CHECK (status IN ('A', 'C', 'P')),
    last_maintenance_user VARCHAR(8),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (portfolio_id, position_date, investment_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id)
);

-- Indexes for position table
CREATE INDEX idx_position_date ON position (position_date, portfolio_id);
CREATE INDEX idx_position_investment ON position (investment_id);
CREATE INDEX idx_position_status ON position (status);

-- Comments for documentation
COMMENT ON TABLE position IS 'Investment Positions - Replaces VSAM POSHIST file';
COMMENT ON COLUMN position.status IS 'A=Active, C=Closed, P=Pending';
