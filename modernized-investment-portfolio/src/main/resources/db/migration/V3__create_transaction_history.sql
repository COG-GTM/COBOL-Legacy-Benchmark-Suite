--====================================================================
-- TRANSACTION HISTORY TABLE (PostgreSQL)
-- Migrated from: src/database/db2/db2-definitions.sql
-- COBOL Copybook: TRNREC.cpy (TRANSACTION-RECORD)
--====================================================================

CREATE TABLE transaction_history (
    transaction_id    CHAR(20)        NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    transaction_type  CHAR(2)         NOT NULL,
    quantity          NUMERIC(18,4)   NOT NULL,
    price             NUMERIC(18,4)   NOT NULL,
    amount            NUMERIC(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    process_date      TIMESTAMP       NOT NULL,
    process_user      VARCHAR(8)      NOT NULL,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

-- Transaction ID format: YYYYMMDDHHMMSS + 6-digit sequence
-- Transaction types: 'BU'=Buy, 'SL'=Sell, 'TR'=Transfer, 'FE'=Fee
-- Status codes: 'P'=Processed, 'F'=Failed, 'R'=Reversed

COMMENT ON TABLE transaction_history IS 'Transaction History (migrated from DB2 TRANSACTION_HISTORY / COBOL TRNREC)';
COMMENT ON COLUMN transaction_history.transaction_id IS 'Transaction ID: YYYYMMDDHHMMSS + 6-digit seq (COBOL: TRN-KEY)';
COMMENT ON COLUMN transaction_history.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee (COBOL: TRN-TYPE)';
COMMENT ON COLUMN transaction_history.status IS 'P=Processed, F=Failed, R=Reversed (COBOL: TRN-STATUS)';

-- Index for portfolio+date lookups (from DB2: IDX_TRANS_HIST_PORT)
CREATE INDEX idx_trans_hist_portfolio
    ON transaction_history (portfolio_id, transaction_date);

-- Date-range index for the partitioned-by-date pattern from COBOL
-- Replaces DB2 date-range partitioning with a BRIN index for efficient date scans
CREATE INDEX idx_trans_hist_date_brin
    ON transaction_history USING BRIN (transaction_date)
    WITH (pages_per_range = 32);

-- Standard B-tree index for date+portfolio lookups (from DB2: IDX_TRANS_HIST_DATE)
CREATE INDEX idx_trans_hist_date
    ON transaction_history (transaction_date, portfolio_id);
