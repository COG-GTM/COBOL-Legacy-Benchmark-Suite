--====================================================================
-- POSITION HISTORY TABLE (PostgreSQL)
-- Migrated from: src/database/db2/POSHIST.sql
-- COBOL Copybook: HISTREC.cpy (HISTORY-RECORD) / DBTBLS.cpy (POSHIST-RECORD)
--
-- The original DB2 table was partitioned by TRANS_DATE (quarterly).
-- PostgreSQL equivalent: date-range indexes (BRIN) for efficient
-- sequential scans on date ranges, replacing DB2 range partitioning.
--====================================================================

CREATE TABLE position_history (
    account_no        CHAR(8)         NOT NULL,
    portfolio_id      CHAR(10)        NOT NULL,
    trans_date        DATE            NOT NULL,
    trans_time        TIME            NOT NULL,
    trans_type        CHAR(2)         NOT NULL,
    security_id       CHAR(12)        NOT NULL,
    quantity          NUMERIC(15,3)   NOT NULL,
    price             NUMERIC(15,3)   NOT NULL,
    amount            NUMERIC(15,2)   NOT NULL,
    fees              NUMERIC(15,2)   NOT NULL DEFAULT 0,
    total_amount      NUMERIC(15,2)   NOT NULL,
    cost_basis        NUMERIC(15,2)   NOT NULL,
    gain_loss         NUMERIC(15,2)   NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

-- Transaction types: BU=Buy, SL=Sell, TR=Transfer

COMMENT ON TABLE position_history IS 'Position History - All portfolio transaction history (migrated from DB2 POSHIST)';
COMMENT ON COLUMN position_history.account_no IS 'Account Number';
COMMENT ON COLUMN position_history.portfolio_id IS 'Portfolio Identifier';
COMMENT ON COLUMN position_history.trans_type IS 'Transaction Type (BU=Buy, SL=Sell, TR=Transfer)';
COMMENT ON COLUMN position_history.security_id IS 'Security Identifier';
COMMENT ON COLUMN position_history.total_amount IS 'Total Amount Including Fees';
COMMENT ON COLUMN position_history.cost_basis IS 'Cost Basis Amount';
COMMENT ON COLUMN position_history.gain_loss IS 'Realized Gain/Loss Amount';

-- BRIN index for date-range scans (replaces DB2 quarterly partitioning)
CREATE INDEX idx_poshist_date_brin
    ON position_history USING BRIN (trans_date)
    WITH (pages_per_range = 32);

-- Secondary index: security + date (from DB2: POSHIST_IX1)
CREATE INDEX idx_poshist_security_date
    ON position_history (security_id, trans_date);

-- Secondary index: process date + program (from DB2: POSHIST_IX2)
CREATE INDEX idx_poshist_process
    ON position_history (process_date, program_id);
