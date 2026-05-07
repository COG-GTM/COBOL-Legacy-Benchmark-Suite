-- =================================================================
-- POSHIST table schema
-- Mirror of POSHIST-RECORD from src/copybook/db2/DBTBLS.cpy
--
-- COMP-3 PIC clauses are translated to NUMERIC(precision, scale):
--   PIC S9(12)V9(3)  -> NUMERIC(15, 3)
--   PIC S9(13)V9(2)  -> NUMERIC(15, 2)
-- =================================================================

CREATE TABLE IF NOT EXISTS poshist (
    account_no       VARCHAR(8)   NOT NULL,
    portfolio_id     VARCHAR(10)  NOT NULL,
    trans_date       VARCHAR(10)  NOT NULL,
    trans_time       VARCHAR(8)   NOT NULL,
    trans_type       VARCHAR(2)   NOT NULL,
    security_id      VARCHAR(12)  NOT NULL,
    quantity         NUMERIC(15, 3) NOT NULL DEFAULT 0,
    price            NUMERIC(15, 3) NOT NULL DEFAULT 0,
    amount           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    fees             NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_amount     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cost_basis       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    gain_loss        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    process_date     VARCHAR(10)  NOT NULL DEFAULT '',
    process_time     VARCHAR(8)   NOT NULL DEFAULT '',
    program_id       VARCHAR(8)   NOT NULL DEFAULT '',
    user_id          VARCHAR(8)   NOT NULL DEFAULT '',
    audit_timestamp  VARCHAR(26)  NOT NULL DEFAULT '',
    CONSTRAINT pk_poshist PRIMARY KEY (
        account_no,
        portfolio_id,
        trans_date,
        trans_time,
        security_id,
        trans_type
    )
);

CREATE INDEX IF NOT EXISTS ix_poshist_portfolio
    ON poshist (portfolio_id, trans_date);

CREATE INDEX IF NOT EXISTS ix_poshist_account
    ON poshist (account_no, trans_date);
