-- ============================================================
-- Seed Data - Based on documentation/operations/test-data-specs.md
-- ============================================================

-- Users are created by DataInitializer with properly encoded passwords

-- Portfolio Master records (from test-data-specs.md sample records)
INSERT INTO portfolio_master (portfolio_id, account_type, branch_id, client_id, portfolio_name, currency_code, risk_level, status, open_date, last_maint_user)
VALUES ('PORT0001', 'GR', '01', 'CLIENT0001', 'Growth Portfolio', 'USD', 'H', 'A', '2024-03-20', 'SYSTEM');

INSERT INTO portfolio_master (portfolio_id, account_type, branch_id, client_id, portfolio_name, currency_code, risk_level, status, open_date, last_maint_user)
VALUES ('PORT0002', 'IN', '01', 'CLIENT0002', 'Income Portfolio', 'USD', 'M', 'A', '2024-03-20', 'SYSTEM');

INSERT INTO portfolio_master (portfolio_id, account_type, branch_id, client_id, portfolio_name, currency_code, risk_level, status, open_date, last_maint_user)
VALUES ('PORT0003', 'BA', '02', 'CLIENT0003', 'Balanced Portfolio', 'USD', 'L', 'A', '2024-03-20', 'SYSTEM');

-- Investment Positions
INSERT INTO investment_positions (portfolio_id, investment_id, position_date, quantity, cost_basis, market_value, currency_code, status, last_maint_user)
VALUES ('PORT0001', 'IBM0000001', '2024-03-20', 100.0000, 12500.00, 13200.00, 'USD', 'A', 'SYSTEM');

INSERT INTO investment_positions (portfolio_id, investment_id, position_date, quantity, cost_basis, market_value, currency_code, status, last_maint_user)
VALUES ('PORT0001', 'AAPL000001', '2024-03-20', 50.0000, 8750.00, 9500.00, 'USD', 'A', 'SYSTEM');

INSERT INTO investment_positions (portfolio_id, investment_id, position_date, quantity, cost_basis, market_value, currency_code, status, last_maint_user)
VALUES ('PORT0002', 'MSFT000001', '2024-03-20', 200.0000, 30000.00, 32500.00, 'USD', 'A', 'SYSTEM');

INSERT INTO investment_positions (portfolio_id, investment_id, position_date, quantity, cost_basis, market_value, currency_code, status, last_maint_user)
VALUES ('PORT0003', 'AAPL000001', '2024-03-20', 75.0000, 13125.00, 14250.00, 'USD', 'A', 'SYSTEM');

-- Transaction History (from test-data-specs.md sample records)
INSERT INTO transaction_history (transaction_id, portfolio_id, transaction_date, transaction_time, investment_id, transaction_type, quantity, price, amount, currency_code, status, process_user)
VALUES ('20240320153045000001', 'PORT0001', '2024-03-20', '15:30:45', 'IBM0000001', 'BU', 100.0000, 125.0000, 12500.00, 'USD', 'P', 'SYSTEM');

INSERT INTO transaction_history (transaction_id, portfolio_id, transaction_date, transaction_time, investment_id, transaction_type, quantity, price, amount, currency_code, status, process_user)
VALUES ('20240320153112000002', 'PORT0002', '2024-03-20', '15:31:12', 'MSFT000001', 'SL', 50.0000, 100.0000, 5000.00, 'USD', 'P', 'SYSTEM');

INSERT INTO transaction_history (transaction_id, portfolio_id, transaction_date, transaction_time, investment_id, transaction_type, quantity, price, amount, currency_code, status, process_user)
VALUES ('20240320153201000003', 'PORT0003', '2024-03-20', '15:32:01', 'AAPL000001', 'BU', 75.0000, 175.0000, 13125.00, 'USD', 'P', 'SYSTEM');

INSERT INTO transaction_history (transaction_id, portfolio_id, transaction_date, transaction_time, investment_id, transaction_type, quantity, price, amount, currency_code, status, process_user)
VALUES ('20240321100000000004', 'PORT0001', '2024-03-21', '10:00:00', 'AAPL000001', 'BU', 50.0000, 175.0000, 8750.00, 'USD', 'P', 'SYSTEM');

INSERT INTO transaction_history (transaction_id, portfolio_id, transaction_date, transaction_time, investment_id, transaction_type, quantity, price, amount, currency_code, status, process_user)
VALUES ('20240321110000000005', 'PORT0002', '2024-03-21', '11:00:00', 'MSFT000001', 'BU', 200.0000, 150.0000, 30000.00, 'USD', 'P', 'SYSTEM');

INSERT INTO transaction_history (transaction_id, portfolio_id, transaction_date, transaction_time, investment_id, transaction_type, quantity, price, amount, currency_code, status, process_user)
VALUES ('20240322090000000006', 'PORT0001', '2024-03-22', '09:00:00', 'IBM0000001', 'FE', 0.0000, 0.0000, 25.00, 'USD', 'P', 'SYSTEM');
