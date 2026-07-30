-- Representative transaction records for the `seed` profile.
--
-- The legacy repository contains NO ASCII extract of the TRANHIST VSAM file, so these rows are not
-- a conversion of production data. They are derived from:
--   * the record layout in src/copybook/common/TRNREC.cpy (field widths and 88-level values), and
--   * the generation conventions of the COBOL test data generators:
--       - PORTTEST.cbl  2100-GENERATE-KEY: portfolio ids are 'PORT' + record counter
--       - TSTGEN00.cbl  2300-GEN-TRANSACTION: one record per configured volume, cycling types
--
-- Coverage: one record per TRN-TYPE (BU/SL/TR/FE) and per TRN-STATUS (P/D/F/R), sequential
-- TRN-SEQUENCE-NO values within a date/portfolio (BR-20), and amounts that satisfy
-- amount = quantity x price truncated to two decimals (BR-22).

INSERT INTO portfolio_transaction
    (trn_date, trn_time, trn_portfolio_id, trn_sequence_no, trn_investment_id, trn_type,
     trn_quantity, trn_price, trn_amount, trn_currency, trn_status, trn_process_date, trn_process_user)
VALUES
    ('20240320', '093015', 'PORT0001', '000001', 'AAPL000001', 'BU',
     150.0000, 187.4500, 28117.50, 'USD', 'D', '2024-03-20-09.30.15.123456', 'BATCH001'),
    ('20240320', '101122', 'PORT0001', '000002', 'AAPL000001', 'SL',
     50.0000, 191.2000, 9560.00, 'USD', 'D', '2024-03-20-10.11.22.000000', 'BATCH001'),
    ('20240320', '104500', 'PORT0001', '000003', 'MGMTFEE001', 'FE',
     1.0000, 125.0000, 125.00, 'USD', 'P', '2024-03-20-10.45.00.000000', 'BATCH001'),
    ('20240320', '110000', 'PORT0002', '000001', 'MSFT000001', 'BU',
     200.0000, 415.3300, 83066.00, 'USD', 'P', '2024-03-20-11.00.00.000000', 'ONLINE01'),
    ('20240320', '113000', 'PORT0002', '000002', 'MSFT000001', 'TR',
     25.0000, 0.0000, 0.00, 'USD', 'F', '2024-03-20-11.30.00.000000', 'ONLINE01'),
    ('20240321', '090500', 'PORT0002', '000001', 'BND000ABCD', 'BU',
     1000.0000, 98.7500, 98750.00, 'EUR', 'D', '2024-03-21-09.05.00.000000', 'BATCH001'),
    ('20240321', '093000', 'PORT0003', '000001', 'ETF000GBLX', 'BU',
     75.5000, 52.1000, 3933.55, 'GBP', 'R', '2024-03-21-09.30.00.000000', 'BATCH001'),
    ('20240321', '160000', 'PORT0003', '000002', 'MMF000CASH', 'SL',
     500.0000, 1.0000, 500.00, 'USD', 'P', '2024-03-21-16.00.00.000000', 'ONLINE02');
