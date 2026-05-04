-- Flyway Migration V5: Position History Table
-- Source: src/database/db2/POSHIST.sql
-- COBOL Copybook: DBTBLS.cpy (POSHIST-RECORD)

CREATE TABLE poshist (
    account_no        VARCHAR(8)         NOT NULL,
    portfolio_id      VARCHAR(10)        NOT NULL,
    trans_date        DATE            NOT NULL,
    trans_time        VARCHAR(8)      NOT NULL,
    trans_type        VARCHAR(2)         NOT NULL,
    security_id       VARCHAR(12)        NOT NULL,
    quantity          DECIMAL(15,3)   NOT NULL DEFAULT 0,
    price             DECIMAL(15,3)   NOT NULL DEFAULT 0,
    amount            DECIMAL(15,2)   NOT NULL DEFAULT 0,
    fees              DECIMAL(15,2)   NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)   NOT NULL DEFAULT 0,
    cost_basis        DECIMAL(15,2)   NOT NULL DEFAULT 0,
    gain_loss         DECIMAL(15,2)   NOT NULL DEFAULT 0,
    process_date      DATE            NOT NULL,
    process_time      VARCHAR(8)      NOT NULL,
    program_id        VARCHAR(8)         NOT NULL,
    user_id           VARCHAR(8)         NOT NULL,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

COMMENT ON TABLE poshist IS 'Position History Table - migrated from DB2 POSHIST';
COMMENT ON COLUMN poshist.trans_type IS 'BU=Buy, SL=Sell, TR=Transfer';
