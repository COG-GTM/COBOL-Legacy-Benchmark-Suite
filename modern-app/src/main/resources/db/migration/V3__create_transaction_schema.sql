-- ============================================================================
-- V3: Create Transaction Schema
-- Description: Transaction tracking tables for portfolio activities
-- Replaces: VSAM TRANHIST (Transaction History File)
-- Source: COBOL Copybook TRNREC.cpy
-- ============================================================================

-- Transaction table (replaces VSAM TRANHIST)
CREATE TABLE transaction (
    transaction_id      VARCHAR(28) PRIMARY KEY,
    portfolio_id        VARCHAR(8) NOT NULL,
    transaction_date    DATE NOT NULL,
    transaction_time    TIME NOT NULL,
    sequence_number     VARCHAR(6),
    investment_id       VARCHAR(10) NOT NULL,
    transaction_type    CHAR(2) NOT NULL CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    quantity            DECIMAL(15, 4) NOT NULL,
    price               DECIMAL(15, 4) NOT NULL,
    amount              DECIMAL(15, 2) NOT NULL,
    currency_code       CHAR(3) NOT NULL DEFAULT 'USD',
    status              CHAR(1) NOT NULL DEFAULT 'P' CHECK (status IN ('P', 'D', 'F', 'R')),
    process_user        VARCHAR(8),
    process_timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id)
);

-- Indexes for transaction table
CREATE INDEX idx_transaction_portfolio ON transaction (portfolio_id, transaction_date);
CREATE INDEX idx_transaction_date ON transaction (transaction_date, portfolio_id);
CREATE INDEX idx_transaction_investment ON transaction (investment_id);
CREATE INDEX idx_transaction_status ON transaction (status);
CREATE INDEX idx_transaction_type ON transaction (transaction_type);

-- Comments for documentation
COMMENT ON TABLE transaction IS 'Transaction History - Replaces VSAM TRANHIST file';
COMMENT ON COLUMN transaction.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN transaction.status IS 'P=Pending, D=Done, F=Failed, R=Reversed';
