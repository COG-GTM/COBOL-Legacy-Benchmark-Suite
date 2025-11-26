-- V2__Create_transaction_table.sql
-- Creates the transaction table to store transaction records

CREATE TABLE transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    quantity DECIMAL(15, 4) NOT NULL,
    price DECIMAL(15, 4),
    amount DECIMAL(15, 2),
    status VARCHAR(10) NOT NULL,
    error_message VARCHAR(200),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (portfolio_id) REFERENCES portfolio(portfolio_id)
);

CREATE INDEX idx_transaction_portfolio ON transaction(portfolio_id);
CREATE INDEX idx_transaction_status ON transaction(status);
