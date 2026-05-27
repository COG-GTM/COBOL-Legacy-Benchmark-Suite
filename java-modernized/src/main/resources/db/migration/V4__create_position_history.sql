--====================================================================
-- POSITION HISTORY TABLE
-- Translated from DB2 (POSHIST.sql lines 25-44)
-- Converted DB2-specific syntax to PostgreSQL
--====================================================================
CREATE TABLE position_history (
    account_no        VARCHAR(8)       NOT NULL,
    portfolio_id      VARCHAR(10)      NOT NULL,
    trans_date        DATE             NOT NULL,
    trans_time        TIME             NOT NULL,
    trans_type        VARCHAR(2)       NOT NULL,
    security_id       VARCHAR(12)      NOT NULL,
    quantity          NUMERIC(15,3)    NOT NULL,
    price             NUMERIC(15,3)    NOT NULL,
    amount            NUMERIC(15,2)    NOT NULL,
    fees              NUMERIC(15,2)    NOT NULL DEFAULT 0,
    total_amount      NUMERIC(15,2)    NOT NULL,
    cost_basis        NUMERIC(15,2)    NOT NULL,
    gain_loss         NUMERIC(15,2)    NOT NULL,
    process_date      DATE             NOT NULL,
    process_time      TIME             NOT NULL,
    program_id        VARCHAR(8)       NOT NULL,
    user_id           VARCHAR(8)       NOT NULL,
    audit_timestamp   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);
