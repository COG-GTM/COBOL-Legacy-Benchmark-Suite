
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL,
    portfolio_id VARCHAR(10) NOT NULL,
    sequence_number VARCHAR(6) NOT NULL,
    investment_id VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(4) NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    price DECIMAL(15, 4) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(1) NOT NULL,
    process_date TIMESTAMP,
    process_user VARCHAR(8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_trn_type CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT chk_trn_status CHECK (status IN ('P', 'D', 'F', 'R'))
);

CREATE INDEX idx_trn_portfolio_id ON transactions(portfolio_id);
CREATE INDEX idx_trn_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_trn_investment_id ON transactions(investment_id);
CREATE INDEX idx_trn_status ON transactions(status);

CREATE INDEX idx_trn_portfolio_date ON transactions(portfolio_id, transaction_date);

COMMENT ON TABLE transactions IS 'Transaction records migrated from COBOL TRNREC copybook';
COMMENT ON COLUMN transactions.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN transactions.status IS 'P=Pending, D=Done, F=Failed, R=Reversed';
