-- Flyway Migration V3: Transaction History Table
-- Source: src/database/db2/db2-definitions.sql (TRANSACTION_HISTORY)
-- COBOL Copybook: TRNREC.cpy

CREATE TABLE transaction_history (
    transaction_id    VARCHAR(28)     NOT NULL,
    portfolio_id      VARCHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  VARCHAR(6)      NOT NULL,
    investment_id     VARCHAR(10)        NOT NULL,
    transaction_type  VARCHAR(2)         NOT NULL,
    sequence_no       VARCHAR(6),
    quantity          DECIMAL(15,4)   NOT NULL DEFAULT 0,
    price             DECIMAL(15,4)   NOT NULL DEFAULT 0,
    amount            DECIMAL(15,2)   NOT NULL DEFAULT 0,
    currency_code     VARCHAR(3)         NOT NULL DEFAULT 'USD',
    status            VARCHAR(1)         NOT NULL DEFAULT 'P',
    process_date      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_user      VARCHAR(8)      NOT NULL,
    filler            VARCHAR(50),
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE transaction_history IS 'Transaction History - migrated from COBOL TRNREC.cpy + DB2 TRANSACTION_HISTORY';
COMMENT ON COLUMN transaction_history.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee (level-88 TRN-TYPE-BUY/SELL/TRANS/FEE)';
COMMENT ON COLUMN transaction_history.status IS 'P=Pending, D=Done, F=Failed, R=Reversed (level-88 TRN-STATUS-PEND/DONE/FAIL/REV)';
