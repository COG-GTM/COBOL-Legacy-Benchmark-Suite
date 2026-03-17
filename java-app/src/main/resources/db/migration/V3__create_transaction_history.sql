-- ============================================================================
-- V3: Transaction History Table
-- Migrated from: DB2 TRANSACTION_HISTORY (db2-definitions.sql lines 46-62)
-- Also replaces: VSAM TRANHIST file (300-byte records,
--                composite key = date 8 + time 6 + portfolio 8 + seq 6)
-- Copybook: TRNREC.cpy
-- Transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
-- Transaction statuses: P=Processed, F=Failed, R=Reversed
-- ============================================================================

CREATE TABLE transaction_history (
    transaction_id    CHAR(20)        NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    transaction_type  CHAR(2)         NOT NULL,
    quantity          NUMERIC(18,4)   NOT NULL DEFAULT 0,
    price             NUMERIC(18,4)   NOT NULL DEFAULT 0,
    amount            NUMERIC(18,2)   NOT NULL DEFAULT 0,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL DEFAULT 'P',
    process_date      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_user      VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_transaction_history PRIMARY KEY (transaction_id),
    CONSTRAINT fk_trans_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id),
    CONSTRAINT chk_trans_type CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT chk_trans_status CHECK (status IN ('P', 'F', 'R'))
);

-- Index matching DB2 IDX_TRANS_HIST_PORT (db2-definitions.sql line 73-74)
CREATE INDEX idx_trans_hist_port ON transaction_history (portfolio_id, transaction_date);

-- Index matching DB2 IDX_TRANS_HIST_DATE (db2-definitions.sql line 76-77)
CREATE INDEX idx_trans_hist_date ON transaction_history (transaction_date, portfolio_id);
