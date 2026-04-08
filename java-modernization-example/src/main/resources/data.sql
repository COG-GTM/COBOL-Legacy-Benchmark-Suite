-- ============================================================================
-- Sample Seed Data for Portfolio Inquiry API
-- Mirrors the data patterns from the COBOL test data generator (TSTGEN00.cbl)
-- ============================================================================

-- Portfolio records (maps to PORTFLIO.cpy / VSAM KSDS)
-- PORT-ID PIC X(8), PORT-ACCOUNT-NO PIC X(10)
INSERT INTO portfolio (portfolio_id, account_no, client_name, client_type, create_date, last_maint_date, status, total_value, cash_balance, last_user, last_trans_date)
VALUES ('PORT0001', 'ACCT000001', 'SMITH JOHN R', 'I', '2024-01-15', '2024-03-20', 'A', 1250000.50, 75000.00, 'ADMIN01', '2024-03-20');

INSERT INTO portfolio (portfolio_id, account_no, client_name, client_type, create_date, last_maint_date, status, total_value, cash_balance, last_user, last_trans_date)
VALUES ('PORT0002', 'ACCT000002', 'ACME CORPORATION', 'C', '2024-02-01', '2024-03-18', 'A', 5400000.00, 250000.75, 'ADMIN02', '2024-03-18');

INSERT INTO portfolio (portfolio_id, account_no, client_name, client_type, create_date, last_maint_date, status, total_value, cash_balance, last_user, last_trans_date)
VALUES ('PORT0003', 'ACCT000003', 'JOHNSON FAMILY TRUST', 'T', '2023-06-10', '2024-03-15', 'A', 3200000.25, 180000.00, 'ADMIN01', '2024-03-15');

INSERT INTO portfolio (portfolio_id, account_no, client_name, client_type, create_date, last_maint_date, status, total_value, cash_balance, last_user, last_trans_date)
VALUES ('PORT0004', 'ACCT000004', 'WILLIAMS SARAH M', 'I', '2023-11-20', '2024-02-28', 'S', 890000.00, 45000.50, 'ADMIN03', '2024-02-28');

INSERT INTO portfolio (portfolio_id, account_no, client_name, client_type, create_date, last_maint_date, status, total_value, cash_balance, last_user, last_trans_date)
VALUES ('PORT0005', 'ACCT000005', 'LEGACY HOLDINGS INC', 'C', '2022-03-01', '2023-12-31', 'C', 0.00, 0.00, 'ADMIN02', '2023-12-31');

-- Transaction records (maps to TRNREC.cpy)
INSERT INTO transaction_record (trans_date, trans_time, portfolio_id, sequence_no, investment_id, trans_type, quantity, price, amount, currency, trans_status, process_timestamp, process_user)
VALUES ('2024-03-20', '10:30:00', 'PORT0001', '000001', 'AAPL', 'BU', 100.0000, 175.5000, 17550.00, 'USD', 'D', '2024-03-20T10:30:15Z', 'BATCH01');

INSERT INTO transaction_record (trans_date, trans_time, portfolio_id, sequence_no, investment_id, trans_type, quantity, price, amount, currency, trans_status, process_timestamp, process_user)
VALUES ('2024-03-19', '14:15:00', 'PORT0001', '000002', 'MSFT', 'SL', 50.0000, 420.2500, 21012.50, 'USD', 'D', '2024-03-19T14:15:30Z', 'BATCH01');

INSERT INTO transaction_record (trans_date, trans_time, portfolio_id, sequence_no, investment_id, trans_type, quantity, price, amount, currency, trans_status, process_timestamp, process_user)
VALUES ('2024-03-18', '09:00:00', 'PORT0002', '000001', 'GOOGL', 'BU', 200.0000, 155.7500, 31150.00, 'USD', 'D', '2024-03-18T09:00:45Z', 'BATCH01');

INSERT INTO transaction_record (trans_date, trans_time, portfolio_id, sequence_no, investment_id, trans_type, quantity, price, amount, currency, trans_status, process_timestamp, process_user)
VALUES ('2024-03-20', '11:00:00', 'PORT0002', '000002', 'AMZN', 'TR', 75.0000, 180.0000, 13500.00, 'USD', 'P', '2024-03-20T11:00:00Z', 'ONLINE1');

INSERT INTO transaction_record (trans_date, trans_time, portfolio_id, sequence_no, investment_id, trans_type, quantity, price, amount, currency, trans_status, process_timestamp, process_user)
VALUES ('2024-03-15', '16:30:00', 'PORT0003', '000001', 'BND', 'FE', 0.0000, 0.0000, 125.00, 'USD', 'D', '2024-03-15T16:30:10Z', 'BATCH01');

-- Position History records (maps to POSHIST DB2 table)
INSERT INTO position_history (account_no, portfolio_id, trans_date, trans_time, trans_type, security_id, quantity, price, amount, fees, total_amount, cost_basis, gain_loss, process_date, process_time, program_id, user_id, audit_timestamp)
VALUES ('ACCT0001', 'PORT0001', '2024-03-20', '10:30:00', 'BU', 'AAPL', 100.000, 175.500, 17550.00, 9.99, 17559.99, 17559.99, 0.00, '2024-03-20', '10:31:00', 'HISTLD00', 'BATCH01', '2024-03-20T10:31:00Z');

INSERT INTO position_history (account_no, portfolio_id, trans_date, trans_time, trans_type, security_id, quantity, price, amount, fees, total_amount, cost_basis, gain_loss, process_date, process_time, program_id, user_id, audit_timestamp)
VALUES ('ACCT0001', 'PORT0001', '2024-03-19', '14:15:00', 'SL', 'MSFT', 50.000, 420.250, 21012.50, 12.50, 21000.00, 18500.00, 2500.00, '2024-03-19', '14:16:00', 'HISTLD00', 'BATCH01', '2024-03-19T14:16:00Z');

INSERT INTO position_history (account_no, portfolio_id, trans_date, trans_time, trans_type, security_id, quantity, price, amount, fees, total_amount, cost_basis, gain_loss, process_date, process_time, program_id, user_id, audit_timestamp)
VALUES ('ACCT0002', 'PORT0002', '2024-03-18', '09:00:00', 'BU', 'GOOGL', 200.000, 155.750, 31150.00, 15.00, 31165.00, 31165.00, 0.00, '2024-03-18', '09:01:00', 'HISTLD00', 'BATCH01', '2024-03-18T09:01:00Z');

INSERT INTO position_history (account_no, portfolio_id, trans_date, trans_time, trans_type, security_id, quantity, price, amount, fees, total_amount, cost_basis, gain_loss, process_date, process_time, program_id, user_id, audit_timestamp)
VALUES ('ACCT0003', 'PORT0003', '2024-03-15', '16:30:00', 'FE', 'BND', 0.000, 0.000, 125.00, 0.00, 125.00, 0.00, 0.00, '2024-03-15', '16:31:00', 'HISTLD00', 'BATCH01', '2024-03-15T16:31:00Z');

INSERT INTO position_history (account_no, portfolio_id, trans_date, trans_time, trans_type, security_id, quantity, price, amount, fees, total_amount, cost_basis, gain_loss, process_date, process_time, program_id, user_id, audit_timestamp)
VALUES ('ACCT0001', 'PORT0001', '2024-03-10', '11:00:00', 'BU', 'MSFT', 50.000, 400.000, 20000.00, 10.00, 20010.00, 20010.00, 0.00, '2024-03-10', '11:01:00', 'HISTLD00', 'BATCH01', '2024-03-10T11:01:00Z');
