--====================================================================
-- PORTFOLIO MASTER TABLE
-- Translated from DB2 (db2-definitions.sql) + VSAM fields (PORTFLIO.cpy)
--====================================================================
CREATE TABLE portfolio_master (
    portfolio_id      VARCHAR(8)       NOT NULL,
    account_type      VARCHAR(2)       NOT NULL,
    branch_id         VARCHAR(2)       NOT NULL,
    client_id         VARCHAR(10)      NOT NULL,
    portfolio_name    VARCHAR(50)      NOT NULL,
    currency_code     VARCHAR(3)       NOT NULL,
    risk_level        VARCHAR(1)       NOT NULL,
    status            VARCHAR(1)       NOT NULL,
    open_date         DATE             NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP        NOT NULL,
    last_maint_user   VARCHAR(8)       NOT NULL,
    client_name       VARCHAR(30),
    client_type       VARCHAR(1),
    total_value       NUMERIC(15,2),
    cash_balance      NUMERIC(15,2),
    PRIMARY KEY (portfolio_id)
);
