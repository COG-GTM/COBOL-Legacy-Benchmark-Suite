
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id VARCHAR(10) NOT NULL,
    position_date DATE NOT NULL,
    investment_id VARCHAR(10) NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    cost_basis DECIMAL(15, 2) NOT NULL,
    market_value DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(1) NOT NULL,
    last_maintenance_date TIMESTAMP,
    last_maintenance_user VARCHAR(8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pos_status CHECK (status IN ('A', 'C', 'P')),
    CONSTRAINT uq_pos_portfolio_date_investment UNIQUE (portfolio_id, position_date, investment_id)
);

CREATE INDEX idx_pos_portfolio_id ON positions(portfolio_id);
CREATE INDEX idx_pos_investment_id ON positions(investment_id);
CREATE INDEX idx_pos_status ON positions(status);
CREATE INDEX idx_pos_position_date ON positions(position_date);

CREATE INDEX idx_pos_portfolio_date ON positions(portfolio_id, position_date);

COMMENT ON TABLE positions IS 'Position records migrated from COBOL POSREC copybook';
COMMENT ON COLUMN positions.status IS 'A=Active, C=Closed, P=Pending';
COMMENT ON COLUMN positions.quantity IS 'Current holding quantity';
COMMENT ON COLUMN positions.cost_basis IS 'Total cost basis of position';
COMMENT ON COLUMN positions.market_value IS 'Current market value of position';
