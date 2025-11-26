-- V3__Create_audit_record_table.sql
-- Creates the audit record table for transaction audit trail

CREATE TABLE audit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP,
    program VARCHAR(8),
    audit_type VARCHAR(4),
    action VARCHAR(8),
    status VARCHAR(4),
    portfolio_id VARCHAR(10),
    account_no VARCHAR(15),
    before_image VARCHAR(500),
    after_image VARCHAR(500),
    message VARCHAR(200)
);

CREATE INDEX idx_audit_portfolio ON audit_record(portfolio_id);
CREATE INDEX idx_audit_timestamp ON audit_record(timestamp);
