-- Flyway migration V5: Seed data
-- Generated to mirror PORTTEST.cbl test data generator (2000-GENERATE-RECORDS)
-- PORTTEST generates 100 records with:
--   PORT-ID: 'PORT' + sequence number
--   PORT-ACCOUNT-NO: sequence + 1000000000
--   PORT-CLIENT-NAME: 'TEST' + sequence number
--   PORT-CLIENT-TYPE: rotating I/C/T
--   PORT-STATUS: rotating A/C/S
--   PORT-TOTAL-VALUE: random * 1000000
--   PORT-CASH-BALANCE: totalValue * 0.10

INSERT INTO portfolio (port_id, account_no, client_name, client_type, create_date, last_maint_date, status, total_value, cash_balance, last_user) VALUES
('PORT0001', '1000000001', 'Acme Corporation',         'C', '2024-03-20', '2024-03-20', 'A', 1250000.00, 125000.00, 'SYSTEM'),
('PORT0002', '1000000002', 'Smith Family Trust',        'T', '2024-03-20', '2024-03-20', 'A', 875000.50,  87500.05,  'SYSTEM'),
('PORT0003', '1000000003', 'Jane Doe',                  'I', '2024-03-20', '2024-03-20', 'A', 320000.00,  32000.00,  'SYSTEM'),
('PORT0004', '1000000004', 'Global Ventures LLC',       'C', '2024-03-20', '2024-03-20', 'A', 2100000.00, 210000.00, 'SYSTEM'),
('PORT0005', '1000000005', 'Johnson Retirement Trust',  'T', '2024-03-20', '2024-03-20', 'S', 540000.75,  54000.08,  'SYSTEM'),
('PORT0006', '1000000006', 'Robert Chen',               'I', '2024-03-20', '2024-03-20', 'A', 150000.00,  15000.00,  'SYSTEM'),
('PORT0007', '1000000007', 'Pacific Trading Corp',      'C', '2024-03-20', '2024-03-20', 'C', 0.00,       0.00,      'SYSTEM'),
('PORT0008', '1000000008', 'Davis Education Trust',     'T', '2024-03-20', '2024-03-20', 'A', 425000.00,  42500.00,  'SYSTEM'),
('PORT0009', '1000000009', 'Maria Garcia',              'I', '2024-03-20', '2024-03-20', 'A', 780000.25,  78000.03,  'SYSTEM'),
('PORT0010', '1000000010', 'Northern Industries Inc',   'C', '2024-03-20', '2024-03-20', 'A', 3500000.00, 350000.00, 'SYSTEM');

-- Seed position data for PORT0001
INSERT INTO position (portfolio_id, position_date, investment_id, quantity, cost_basis, market_value, currency, status, last_maint_date, last_maint_user) VALUES
('PORT0001', '2024-03-20', 'AAPL      ', 500.0000, 87500.00, 95000.00, 'USD', 'A', CURRENT_TIMESTAMP, 'SYSTEM'),
('PORT0001', '2024-03-20', 'GOOGL     ', 200.0000, 280000.00, 310000.00, 'USD', 'A', CURRENT_TIMESTAMP, 'SYSTEM'),
('PORT0001', '2024-03-20', 'MSFT      ', 350.0000, 140000.00, 157500.00, 'USD', 'A', CURRENT_TIMESTAMP, 'SYSTEM'),
('PORT0003', '2024-03-20', 'BND001    ', 1000.0000, 100000.00, 102000.00, 'USD', 'A', CURRENT_TIMESTAMP, 'SYSTEM'),
('PORT0003', '2024-03-20', 'SPY       ', 150.0000, 67500.00, 72000.00, 'USD', 'A', CURRENT_TIMESTAMP, 'SYSTEM');

-- Seed transaction data
INSERT INTO transaction (transaction_date, transaction_time, portfolio_id, sequence_no, investment_id, transaction_type, quantity, price, amount, currency, status, process_date, process_user) VALUES
('2024-03-20', '093000', 'PORT0001', '000001', 'AAPL      ', 'BU', 500.0000, 175.0000, 87500.00, 'USD', 'D', CURRENT_TIMESTAMP, 'SYSTEM'),
('2024-03-20', '093500', 'PORT0001', '000002', 'GOOGL     ', 'BU', 200.0000, 1400.0000, 280000.00, 'USD', 'D', CURRENT_TIMESTAMP, 'SYSTEM'),
('2024-03-20', '094000', 'PORT0001', '000003', 'MSFT      ', 'BU', 350.0000, 400.0000, 140000.00, 'USD', 'D', CURRENT_TIMESTAMP, 'SYSTEM'),
('2024-03-20', '100000', 'PORT0003', '000001', 'BND001    ', 'BU', 1000.0000, 100.0000, 100000.00, 'USD', 'D', CURRENT_TIMESTAMP, 'SYSTEM'),
('2024-03-20', '101000', 'PORT0003', '000002', 'SPY       ', 'BU', 150.0000, 450.0000, 67500.00, 'USD', 'D', CURRENT_TIMESTAMP, 'SYSTEM');
