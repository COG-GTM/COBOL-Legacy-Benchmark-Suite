-- V4__Create_error_log_table.sql
-- Creates the error log table to replace COBOL ERRPROC functionality

CREATE TABLE error_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program VARCHAR(8),
    error_code VARCHAR(30),
    error_message VARCHAR(500),
    timestamp TIMESTAMP,
    category VARCHAR(10)
);

CREATE INDEX idx_error_log_timestamp ON error_log(timestamp);
CREATE INDEX idx_error_log_program ON error_log(program);
