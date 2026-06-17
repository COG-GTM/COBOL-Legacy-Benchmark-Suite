-- =====================================================================
-- V2: Representative sample data
-- Provides existing holdings (position_master) and pending transactions
-- (txn) so the REST API returns data and the position-update batch job has
-- work to process. This mirrors what TSTGEN00 (test data generator) produced
-- for the COBOL suite. Amounts use the copybook decimal scales.
-- =====================================================================

-- Existing holdings (as-of 2024-06-17)
INSERT INTO position_master
    (portfolio_id, position_date, investment_id, quantity, cost_basis, market_value, currency, status)
VALUES
    ('PORT0001', '20240617', 'SEC0000001', 1000.0000, 50000.00, 52000.00, 'USD', 'A'),
    ('PORT0001', '20240617', 'SEC0000002',  500.0000, 25000.00, 24000.00, 'USD', 'A'),
    ('PORT0002', '20240617', 'SEC0000001',  200.0000, 12000.00, 12500.00, 'USD', 'A');

-- Pending transactions to be applied by POSUPD00 (status 'P')
INSERT INTO txn
    (trn_date, trn_time, portfolio_id, sequence_no, investment_id, trn_type,
     quantity, price, amount, currency, status, process_user)
VALUES
    ('20240617', '090000', 'PORT0001', '000001', 'SEC0000001', 'BU',
     100.0000, 55.0000, 5500.00, 'USD', 'P', 'BATCH'),
    ('20240617', '093000', 'PORT0001', '000002', 'SEC0000002', 'SL',
     100.0000, 48.0000, 4800.00, 'USD', 'P', 'BATCH'),
    ('20240617', '100000', 'PORT0001', '000003', 'SEC0000001', 'FE',
       1.0000,  1.0000,   25.00, 'USD', 'P', 'BATCH'),
    ('20240617', '103000', 'PORT0002', '000004', 'SEC0000003', 'BU',
     300.0000, 20.0000, 6000.00, 'USD', 'P', 'BATCH');
