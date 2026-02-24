-- Migration 002: Create VSAM-equivalent tables in PostgreSQL
-- Source: src/database/vsam/vsam-definitions.txt
-- Converts VSAM KSDS file definitions to PostgreSQL relational tables

BEGIN;

-- ====================================================================
-- VSAM PORTFOLIO MASTER (PORTMSTR)
-- Source: VSAM KSDS, Record Length: 400, Key Length: 12
-- Key: Portfolio ID (8) + Account Type (2) + Branch ID (2)
-- Copybook: PORTFLIO
-- ====================================================================
CREATE TABLE IF NOT EXISTS vsam_portfolio_master (
    -- Key fields (VSAM key: 12 bytes)
    portfolio_id      VARCHAR(8)          NOT NULL,
    account_type      VARCHAR(2)          NOT NULL,
    branch_id         VARCHAR(2)          NOT NULL,

    -- Portfolio data fields (from PORTFLIO copybook)
    account_no        VARCHAR(10)         NOT NULL,
    client_name       VARCHAR(30)         NOT NULL,
    client_type       VARCHAR(1)          NOT NULL,
    create_date       DATE                NOT NULL DEFAULT CURRENT_DATE,
    last_maint_date   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    status            VARCHAR(1)          NOT NULL DEFAULT 'A',
    total_value       NUMERIC(15,2)       NOT NULL DEFAULT 0,
    cash_balance      NUMERIC(15,2)       NOT NULL DEFAULT 0,
    last_user         VARCHAR(8),
    last_trans_id     VARCHAR(20),

    PRIMARY KEY (portfolio_id, account_type, branch_id)
);

COMMENT ON TABLE vsam_portfolio_master IS 'Portfolio master records. Migrated from VSAM KSDS PORTMSTR (400-byte records).';
COMMENT ON COLUMN vsam_portfolio_master.client_type IS 'I=Individual, C=Corporate, T=Trust';
COMMENT ON COLUMN vsam_portfolio_master.status IS 'A=Active, C=Closed, S=Suspended';

CREATE INDEX IF NOT EXISTS idx_vsam_portmstr_account
    ON vsam_portfolio_master (account_no);

CREATE INDEX IF NOT EXISTS idx_vsam_portmstr_client
    ON vsam_portfolio_master (client_name);

CREATE INDEX IF NOT EXISTS idx_vsam_portmstr_status
    ON vsam_portfolio_master (status);

-- ====================================================================
-- VSAM TRANSACTION HISTORY (TRANHIST)
-- Source: VSAM KSDS, Record Length: 300, Key Length: 20
-- Key: Trans Date (8) + Trans Time (6) + Portfolio ID (8) + Seq No (6)
-- Copybook: TRNREC
-- ====================================================================
CREATE TABLE IF NOT EXISTS vsam_transaction_history (
    -- Key fields (VSAM key: 20 bytes total, but split logically)
    transaction_date  VARCHAR(8)          NOT NULL,
    transaction_time  VARCHAR(6)          NOT NULL,
    portfolio_id      VARCHAR(8)          NOT NULL,
    sequence_no       VARCHAR(6)          NOT NULL,

    -- Transaction data fields (from TRNREC copybook)
    investment_id     VARCHAR(10)         NOT NULL,
    transaction_type  VARCHAR(2)          NOT NULL,
    quantity          NUMERIC(15,4)       NOT NULL,
    price             NUMERIC(15,4)       NOT NULL,
    amount            NUMERIC(15,2)       NOT NULL,
    currency_code     VARCHAR(3)          NOT NULL DEFAULT 'USD',
    status            VARCHAR(1)          NOT NULL DEFAULT 'P',
    process_date      VARCHAR(8),
    process_user      VARCHAR(8),

    PRIMARY KEY (transaction_date, transaction_time, portfolio_id, sequence_no)
);

COMMENT ON TABLE vsam_transaction_history IS 'Transaction history records. Migrated from VSAM KSDS TRANHIST (300-byte records).';
COMMENT ON COLUMN vsam_transaction_history.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN vsam_transaction_history.status IS 'P=Pending, D=Done, F=Failed, R=Reversed';
COMMENT ON COLUMN vsam_transaction_history.transaction_date IS 'Format: YYYYMMDD';
COMMENT ON COLUMN vsam_transaction_history.transaction_time IS 'Format: HHMMSS';

CREATE INDEX IF NOT EXISTS idx_vsam_tranhist_portfolio
    ON vsam_transaction_history (portfolio_id, transaction_date);

CREATE INDEX IF NOT EXISTS idx_vsam_tranhist_type
    ON vsam_transaction_history (transaction_type, transaction_date);

CREATE INDEX IF NOT EXISTS idx_vsam_tranhist_status
    ON vsam_transaction_history (status);

-- ====================================================================
-- VSAM POSITION HISTORY (POSHIST)
-- Source: VSAM KSDS, Record Length: 350, Key Length: 18
-- Key: Portfolio ID (8) + Position Date (8) + Investment ID (10)
-- Note: This is the VSAM-side position history, distinct from the
--       DB2 POSHIST table which stores loaded/processed records
-- ====================================================================
CREATE TABLE IF NOT EXISTS vsam_position_history (
    -- Key fields (VSAM key: 18 bytes, but investment_id is 10)
    portfolio_id      VARCHAR(8)          NOT NULL,
    position_date     VARCHAR(8)          NOT NULL,
    investment_id     VARCHAR(10)         NOT NULL,

    -- Position data fields (from POSREC copybook)
    quantity          NUMERIC(15,4)       NOT NULL DEFAULT 0,
    cost_basis        NUMERIC(15,2)       NOT NULL DEFAULT 0,
    market_value      NUMERIC(15,2)       NOT NULL DEFAULT 0,
    currency_code     VARCHAR(3)          NOT NULL DEFAULT 'USD',
    status            VARCHAR(1)          NOT NULL DEFAULT 'A',
    last_maint_date   VARCHAR(8),
    last_maint_user   VARCHAR(8),

    PRIMARY KEY (portfolio_id, position_date, investment_id)
);

COMMENT ON TABLE vsam_position_history IS 'Position history records. Migrated from VSAM KSDS POSHIST (350-byte records).';
COMMENT ON COLUMN vsam_position_history.status IS 'A=Active, C=Closed, P=Pending';
COMMENT ON COLUMN vsam_position_history.position_date IS 'Format: YYYYMMDD';

CREATE INDEX IF NOT EXISTS idx_vsam_poshist_date
    ON vsam_position_history (position_date, portfolio_id);

-- ====================================================================
-- BATCH CONTROL TABLE
-- Source: VSAM KSDS used by BCHCTL00 program
-- ====================================================================
CREATE TABLE IF NOT EXISTS batch_control (
    batch_key         VARCHAR(20)         NOT NULL,
    job_name          VARCHAR(8)          NOT NULL,
    step_name         VARCHAR(8),
    status            VARCHAR(1)          NOT NULL DEFAULT 'P',
    start_timestamp   TIMESTAMPTZ,
    end_timestamp     TIMESTAMPTZ,
    records_read      INTEGER             NOT NULL DEFAULT 0,
    records_written   INTEGER             NOT NULL DEFAULT 0,
    records_error     INTEGER             NOT NULL DEFAULT 0,
    return_code       INTEGER             NOT NULL DEFAULT 0,
    last_checkpoint   TIMESTAMPTZ,
    restart_info      VARCHAR(200),
    PRIMARY KEY (batch_key)
);

COMMENT ON TABLE batch_control IS 'Batch job control records. Migrated from VSAM BATCH-CONTROL-FILE.';
COMMENT ON COLUMN batch_control.status IS 'P=Pending, R=Running, C=Complete, F=Failed, A=Aborted';

COMMIT;
