-- ============================================================================
-- Seed Data for Investment Portfolio Management System
-- Phase 1: Initial Data Setup
-- Version: 1.0.0
-- ============================================================================

SET search_path TO portfolio, public;

-- ============================================================================
-- SEED: Default Users (System and Admin accounts)
-- ============================================================================

INSERT INTO users (user_id, username, password_hash, email, first_name, last_name, department, role, is_active)
VALUES
    ('SYSTEM', 'system', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.qQQQQQQQQQQQQQ', 'system@portfolio.local', 'System', 'Account', 'IT', 'SYSTEM', true),
    ('ADMIN', 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.qQQQQQQQQQQQQQ', 'admin@portfolio.local', 'Admin', 'User', 'IT', 'ADMIN', true),
    ('BATCH', 'batch', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.qQQQQQQQQQQQQQ', 'batch@portfolio.local', 'Batch', 'Process', 'Operations', 'SERVICE', true),
    ('REPORT', 'report', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.qQQQQQQQQQQQQQ', 'report@portfolio.local', 'Report', 'Service', 'Operations', 'SERVICE', true);

-- ============================================================================
-- SEED: Default User Authorizations
-- ============================================================================

INSERT INTO user_authorizations (user_id, resource, access_type, granted_by)
VALUES
    ('SYSTEM', '*', '*', 'SYSTEM'),
    ('ADMIN', '*', '*', 'SYSTEM'),
    ('BATCH', 'POSFILE', 'UPDATE', 'ADMIN'),
    ('BATCH', 'TRANHIST', 'INSERT', 'ADMIN'),
    ('BATCH', 'POSHIST', 'INSERT', 'ADMIN'),
    ('BATCH', 'ERRLOG', 'INSERT', 'ADMIN'),
    ('REPORT', 'POSFILE', 'READ', 'ADMIN'),
    ('REPORT', 'TRANHIST', 'READ', 'ADMIN'),
    ('REPORT', 'POSHIST', 'READ', 'ADMIN');

-- ============================================================================
-- SEED: Process Control Configuration
-- Mapped from data-dictionary.md Process IDs
-- ============================================================================

INSERT INTO process_control (process_date, sequence_no, program_id, program_desc, required_return_code, dependency, is_restartable)
VALUES
    (CURRENT_DATE, 1, 'TRNVAL00', 'Transaction Validation', 4, NULL, true),
    (CURRENT_DATE, 2, 'POSUPD00', 'Position Update', 4, 'TRNVAL00', true),
    (CURRENT_DATE, 3, 'HISTLD00', 'History Load to DB2', 4, 'POSUPD00', true),
    (CURRENT_DATE, 4, 'RPTPOS00', 'Position Report Generation', 4, 'POSUPD00', false),
    (CURRENT_DATE, 5, 'RPTAUD00', 'Audit Report Generation', 4, 'HISTLD00', false),
    (CURRENT_DATE, 6, 'RPTSTA00', 'Statistics Report Generation', 4, 'HISTLD00', false);

-- ============================================================================
-- SEED: Sample Portfolios (for testing)
-- ============================================================================

INSERT INTO portfolios (portfolio_id, account_type, branch_id, client_id, portfolio_name, currency_code, risk_level, status, open_date)
VALUES
    ('PORT0001', 'IN', '01', 'CLIENT0001', 'Growth Portfolio', 'USD', 'H', 'ACTIVE', '2024-01-15'),
    ('PORT0002', 'IN', '01', 'CLIENT0001', 'Income Portfolio', 'USD', 'L', 'ACTIVE', '2024-01-15'),
    ('PORT0003', 'RE', '02', 'CLIENT0002', 'Retirement Fund', 'USD', 'M', 'ACTIVE', '2024-02-01'),
    ('PORT0004', 'IN', '01', 'CLIENT0003', 'Aggressive Growth', 'USD', 'H', 'ACTIVE', '2024-03-01'),
    ('PORT0005', 'TR', '03', 'CLIENT0004', 'Trust Account', 'USD', 'L', 'ACTIVE', '2024-03-15');

-- ============================================================================
-- SEED: Sample Positions
-- ============================================================================

INSERT INTO positions (portfolio_id, position_date, investment_id, cusip, quantity, cost_basis, market_value, average_cost, currency_code, status)
VALUES
    ('PORT0001', CURRENT_DATE, 'AAPL', '037833100', 100.0000, 15000.00, 17500.00, 150.0000, 'USD', 'ACTIVE'),
    ('PORT0001', CURRENT_DATE, 'GOOGL', '02079K305', 50.0000, 7000.00, 7500.00, 140.0000, 'USD', 'ACTIVE'),
    ('PORT0001', CURRENT_DATE, 'MSFT', '594918104', 75.0000, 22500.00, 28125.00, 300.0000, 'USD', 'ACTIVE'),
    ('PORT0002', CURRENT_DATE, 'VTI', '922908363', 200.0000, 40000.00, 44000.00, 200.0000, 'USD', 'ACTIVE'),
    ('PORT0002', CURRENT_DATE, 'BND', '921937835', 300.0000, 24000.00, 23400.00, 80.0000, 'USD', 'ACTIVE'),
    ('PORT0003', CURRENT_DATE, 'SPY', '78462F103', 150.0000, 60000.00, 67500.00, 400.0000, 'USD', 'ACTIVE'),
    ('PORT0003', CURRENT_DATE, 'QQQ', '46090E103', 100.0000, 35000.00, 38000.00, 350.0000, 'USD', 'ACTIVE');

-- ============================================================================
-- SEED: Sample Transactions
-- ============================================================================

INSERT INTO transactions (transaction_id, portfolio_id, transaction_date, transaction_time, sequence_no, investment_id, transaction_type, quantity, price, amount, fees, total_amount, currency_code, status, process_date, process_user)
VALUES
    ('20240115093000000001', 'PORT0001', '2024-01-15', '09:30:00', '000001', 'AAPL', 'BUY', 100.0000, 150.0000, 15000.00, 9.99, 15009.99, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH'),
    ('20240115093500000002', 'PORT0001', '2024-01-15', '09:35:00', '000002', 'GOOGL', 'BUY', 50.0000, 140.0000, 7000.00, 9.99, 7009.99, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH'),
    ('20240115094000000003', 'PORT0001', '2024-01-15', '09:40:00', '000003', 'MSFT', 'BUY', 75.0000, 300.0000, 22500.00, 9.99, 22509.99, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH'),
    ('20240201100000000001', 'PORT0002', '2024-02-01', '10:00:00', '000001', 'VTI', 'BUY', 200.0000, 200.0000, 40000.00, 0.00, 40000.00, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH'),
    ('20240201100500000002', 'PORT0002', '2024-02-01', '10:05:00', '000002', 'BND', 'BUY', 300.0000, 80.0000, 24000.00, 0.00, 24000.00, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH'),
    ('20240301110000000001', 'PORT0003', '2024-03-01', '11:00:00', '000001', 'SPY', 'BUY', 150.0000, 400.0000, 60000.00, 0.00, 60000.00, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH'),
    ('20240301110500000002', 'PORT0003', '2024-03-01', '11:05:00', '000002', 'QQQ', 'BUY', 100.0000, 350.0000, 35000.00, 0.00, 35000.00, 'USD', 'COMPLETED', CURRENT_TIMESTAMP, 'BATCH');

-- ============================================================================
-- SEED: Initial Audit Log Entry
-- ============================================================================

INSERT INTO audit_log (system_id, user_id, program_id, event_type, action, status, message)
VALUES
    ('PORTMGMT', 'SYSTEM', 'MIGRATE', 'SYSTEM_EVENT', 'STARTUP', 'SUCCESS', 'Phase 1 migration schema initialized');
