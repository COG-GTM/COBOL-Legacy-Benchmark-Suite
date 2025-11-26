-- V1__Create_portfolio_table.sql
-- Creates the portfolio table to replace VSAM indexed file

CREATE TABLE portfolio (
    portfolio_id VARCHAR(10) PRIMARY KEY,
    account_no VARCHAR(15),
    total_units DECIMAL(15, 4) NOT NULL DEFAULT 0,
    total_cost DECIMAL(15, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_portfolio_account ON portfolio(account_no);
