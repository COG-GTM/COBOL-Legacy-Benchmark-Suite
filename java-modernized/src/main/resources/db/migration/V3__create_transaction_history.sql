--====================================================================
-- TRANSACTION HISTORY TABLE
-- Translated from DB2 (db2-definitions.sql lines 46-62)
--====================================================================
CREATE TABLE transaction_history (
    transaction_id    VARCHAR(20)      NOT NULL,
    portfolio_id      VARCHAR(8)       NOT NULL,
    transaction_date  DATE             NOT NULL,
    transaction_time  TIME             NOT NULL,
    investment_id     VARCHAR(10)      NOT NULL,
    transaction_type  VARCHAR(2)       NOT NULL,
    quantity          NUMERIC(18,4)    NOT NULL,
    price             NUMERIC(18,4)    NOT NULL,
    amount            NUMERIC(18,2)    NOT NULL,
    currency_code     VARCHAR(3)       NOT NULL,
    status            VARCHAR(1)       NOT NULL,
    process_date      TIMESTAMP        NOT NULL,
    process_user      VARCHAR(8)       NOT NULL,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);
