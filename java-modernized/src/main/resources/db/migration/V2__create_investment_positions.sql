--====================================================================
-- INVESTMENT POSITIONS TABLE
-- Translated from DB2 (db2-definitions.sql) + POSREC.cpy status field
--====================================================================
CREATE TABLE investment_positions (
    portfolio_id      VARCHAR(8)       NOT NULL,
    investment_id     VARCHAR(10)      NOT NULL,
    position_date     DATE             NOT NULL,
    quantity          NUMERIC(18,4)    NOT NULL,
    cost_basis        NUMERIC(18,2)    NOT NULL,
    market_value      NUMERIC(18,2)    NOT NULL,
    currency_code     VARCHAR(3)       NOT NULL,
    status            VARCHAR(1),
    last_maint_date   TIMESTAMP        NOT NULL,
    last_maint_user   VARCHAR(8)       NOT NULL,
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);
