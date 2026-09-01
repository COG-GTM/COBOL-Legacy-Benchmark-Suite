CREATE TABLE transaction_record (
    portfolio_id CHAR(8) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL,
    sequence_no CHAR(6) NOT NULL,
    investment_id CHAR(10) NOT NULL,
    transaction_type CHAR(2) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    price NUMERIC(18,4) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    status CHAR(1) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    process_user VARCHAR(8) NOT NULL,
    PRIMARY KEY (portfolio_id, transaction_date, transaction_time, sequence_no)
);

CREATE TABLE position_record (
    portfolio_id CHAR(8) NOT NULL,
    position_date DATE NOT NULL,
    investment_id CHAR(10) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    cost_basis NUMERIC(18,2) NOT NULL,
    market_value NUMERIC(18,2) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    status CHAR(1) NOT NULL,
    last_maint_at TIMESTAMP NOT NULL,
    last_maint_user VARCHAR(8) NOT NULL,
    PRIMARY KEY (portfolio_id, position_date, investment_id)
);

CREATE TABLE history_record (
    portfolio_id CHAR(8) NOT NULL,
    history_date DATE NOT NULL,
    history_time TIME NOT NULL,
    sequence_no CHAR(4) NOT NULL,
    record_type CHAR(2) NOT NULL,
    action_code CHAR(1) NOT NULL,
    before_image VARCHAR(400),
    after_image VARCHAR(400),
    reason_code CHAR(4),
    processed_at TIMESTAMP NOT NULL,
    process_user VARCHAR(8) NOT NULL,
    PRIMARY KEY (portfolio_id, history_date, history_time, sequence_no)
);

CREATE INDEX idx_transaction_record_portfolio_date
    ON transaction_record (portfolio_id, transaction_date);

CREATE INDEX idx_position_record_date
    ON position_record (position_date, portfolio_id);

CREATE INDEX idx_history_record_portfolio_date
    ON history_record (portfolio_id, history_date);
