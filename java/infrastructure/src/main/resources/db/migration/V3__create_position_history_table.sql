
CREATE TABLE position_history (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id VARCHAR(10) NOT NULL,
    history_date DATE NOT NULL,
    history_time TIME NOT NULL,
    sequence_number VARCHAR(4) NOT NULL,
    record_type VARCHAR(2) NOT NULL,
    action_code VARCHAR(1) NOT NULL,
    before_image TEXT,
    after_image TEXT,
    reason_code VARCHAR(4),
    process_date TIMESTAMP,
    process_user VARCHAR(8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hist_record_type CHECK (record_type IN ('PT', 'PS', 'TR')),
    CONSTRAINT chk_hist_action_code CHECK (action_code IN ('A', 'C', 'D'))
);

CREATE INDEX idx_hist_portfolio_id ON position_history(portfolio_id);
CREATE INDEX idx_hist_date ON position_history(history_date);
CREATE INDEX idx_hist_record_type ON position_history(record_type);

CREATE INDEX idx_hist_portfolio_date ON position_history(portfolio_id, history_date);

COMMENT ON TABLE position_history IS 'History records migrated from COBOL HISTREC copybook';
COMMENT ON COLUMN position_history.record_type IS 'PT=Portfolio, PS=Position, TR=Transaction';
COMMENT ON COLUMN position_history.action_code IS 'A=Add, C=Change, D=Delete';
COMMENT ON COLUMN position_history.before_image IS 'Record state before change';
COMMENT ON COLUMN position_history.after_image IS 'Record state after change';
