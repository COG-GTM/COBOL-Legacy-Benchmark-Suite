-- Migration 002: Create PostgreSQL tables from VSAM file definitions
-- Source: src/database/vsam/vsam-definitions.txt
-- Target: PostgreSQL 16+
--
-- VSAM Feature Mapping:
--   KSDS (Key-Sequenced Data Set) -> B-tree indexed table with composite PK
--   CI/CA FREESPACE -> fillfactor storage parameter
--   SHAREOPTIONS(2,3) -> MVCC (concurrent read/write)
--   RECOVERY -> WAL-based point-in-time recovery

BEGIN;

-- ====================================================================
-- VSAM PORTFOLIO MASTER (PORTMSTR)
-- Source: VSAM KSDS, Record Length: 400, Key Length: 12
-- Key: Portfolio ID (8) + Account Type (2) + Branch ID (2)
-- Mapped from copybook: PORTFLIO.cpy
-- ====================================================================
CREATE TABLE vsam_portfolio_master (
    portfolio_id      CHAR(8)         NOT NULL,
    account_type      CHAR(2)         NOT NULL,
    branch_id         CHAR(2)         NOT NULL,
    account_no        CHAR(10)        NOT NULL,
    client_name       VARCHAR(30)     NOT NULL,
    client_type       CHAR(1)         NOT NULL,
    create_date       DATE            NOT NULL,
    last_maint_date   DATE,
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    total_value       DECIMAL(15,2)   NOT NULL DEFAULT 0,
    cash_balance      DECIMAL(15,2)   NOT NULL DEFAULT 0,
    last_user         VARCHAR(8),
    last_trans_date   DATE,
    CONSTRAINT pk_vsam_portfolio_master PRIMARY KEY (portfolio_id, account_type, branch_id),
    CONSTRAINT chk_vsam_port_client_type CHECK (client_type IN ('I', 'C', 'T')),
    CONSTRAINT chk_vsam_port_status CHECK (status IN ('A', 'C', 'S'))
) WITH (fillfactor = 80);

COMMENT ON TABLE vsam_portfolio_master IS 'Portfolio Master - migrated from VSAM KSDS PORTMSTR (400-byte records)';
COMMENT ON COLUMN vsam_portfolio_master.client_type IS 'I=Individual, C=Corporate, T=Trust';
COMMENT ON COLUMN vsam_portfolio_master.status IS 'A=Active, C=Closed, S=Suspended';

CREATE INDEX idx_vsam_port_account
    ON vsam_portfolio_master (account_no);

CREATE INDEX idx_vsam_port_status
    ON vsam_portfolio_master (status, portfolio_id);

-- ====================================================================
-- VSAM TRANSACTION HISTORY (TRANHIST)
-- Source: VSAM KSDS, Record Length: 300, Key Length: 20
-- Key: Date (8) + Time (6) + Portfolio ID (8) + Sequence (6) [adjusted from definition: 20 bytes total mapped to logical fields]
-- Mapped from copybook: TRNREC.cpy
-- ====================================================================
CREATE TABLE vsam_transaction_history (
    trans_date        CHAR(8)         NOT NULL,
    trans_time        CHAR(6)         NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    sequence_no       CHAR(6)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    trans_type        CHAR(2)         NOT NULL,
    quantity          DECIMAL(15,4)   NOT NULL,
    price             DECIMAL(15,4)   NOT NULL,
    amount            DECIMAL(15,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL DEFAULT 'USD',
    status            CHAR(1)         NOT NULL DEFAULT 'P',
    process_timestamp VARCHAR(26),
    process_user      VARCHAR(8),
    CONSTRAINT pk_vsam_transaction_history PRIMARY KEY (trans_date, trans_time, portfolio_id, sequence_no),
    CONSTRAINT chk_vsam_trn_type CHECK (trans_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT chk_vsam_trn_status CHECK (status IN ('P', 'D', 'F', 'R'))
) WITH (fillfactor = 90);

COMMENT ON TABLE vsam_transaction_history IS 'Transaction History - migrated from VSAM KSDS TRANHIST (300-byte records)';
COMMENT ON COLUMN vsam_transaction_history.trans_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN vsam_transaction_history.status IS 'P=Pending, D=Done, F=Failed, R=Reversed';

CREATE INDEX idx_vsam_trn_portfolio
    ON vsam_transaction_history (portfolio_id, trans_date);

CREATE INDEX idx_vsam_trn_investment
    ON vsam_transaction_history (investment_id, trans_date);

-- ====================================================================
-- VSAM POSITION HISTORY (POSHIST)
-- Source: VSAM KSDS, Record Length: 350, Key Length: 18
-- Key: Portfolio ID (8) + Position Date (8) + Investment ID (10) [adjusted: 18 bytes mapped]
-- Mapped from copybook: POSREC.cpy
-- ====================================================================
CREATE TABLE vsam_position_history (
    portfolio_id      CHAR(8)         NOT NULL,
    position_date     CHAR(8)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    quantity          DECIMAL(15,4)   NOT NULL,
    cost_basis        DECIMAL(15,2)   NOT NULL,
    market_value      DECIMAL(15,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL DEFAULT 'USD',
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    last_maint_date   VARCHAR(26),
    last_maint_user   VARCHAR(8),
    CONSTRAINT pk_vsam_position_history PRIMARY KEY (portfolio_id, position_date, investment_id),
    CONSTRAINT chk_vsam_pos_status CHECK (status IN ('A', 'C', 'P'))
) WITH (fillfactor = 90);

COMMENT ON TABLE vsam_position_history IS 'Position History - migrated from VSAM KSDS POSHIST (350-byte records)';
COMMENT ON COLUMN vsam_position_history.status IS 'A=Active, C=Closed, P=Pending';

CREATE INDEX idx_vsam_pos_date
    ON vsam_position_history (position_date, portfolio_id);

CREATE INDEX idx_vsam_pos_investment
    ON vsam_position_history (investment_id, position_date);

-- ====================================================================
-- VSAM BATCH CONTROL (derived from BCHCTL copybook)
-- Not in vsam-definitions.txt but referenced as VSAM KSDS in data dictionary
-- Key: Process Date (8) + Process ID (8)
-- ====================================================================
CREATE TABLE vsam_batch_control (
    job_name          CHAR(8)         NOT NULL,
    process_date      CHAR(8)         NOT NULL,
    sequence_no       INTEGER         NOT NULL,
    status            CHAR(1)         NOT NULL DEFAULT 'R',
    step_name         CHAR(8),
    program_name      CHAR(8),
    start_time        CHAR(8),
    end_time          CHAR(8),
    prereq_count      SMALLINT        NOT NULL DEFAULT 0,
    return_code       SMALLINT,
    error_desc        VARCHAR(80),
    restart_count     SMALLINT        NOT NULL DEFAULT 0,
    attempt_timestamp VARCHAR(26),
    complete_timestamp VARCHAR(26),
    CONSTRAINT pk_vsam_batch_control PRIMARY KEY (job_name, process_date, sequence_no),
    CONSTRAINT chk_vsam_bch_status CHECK (status IN ('R', 'A', 'W', 'D', 'E'))
);

COMMENT ON TABLE vsam_batch_control IS 'Batch Control - migrated from VSAM KSDS BCHCTL';
COMMENT ON COLUMN vsam_batch_control.status IS 'R=Ready, A=Active, W=Waiting, D=Done, E=Error';

-- Prerequisite tracking for batch jobs
CREATE TABLE vsam_batch_prerequisites (
    job_name          CHAR(8)         NOT NULL,
    process_date      CHAR(8)         NOT NULL,
    sequence_no       INTEGER         NOT NULL,
    prereq_index      SMALLINT        NOT NULL,
    prereq_name       CHAR(8)         NOT NULL,
    prereq_seq        INTEGER         NOT NULL,
    prereq_rc         SMALLINT,
    CONSTRAINT pk_vsam_batch_prereqs PRIMARY KEY (job_name, process_date, sequence_no, prereq_index),
    CONSTRAINT fk_batch_prereqs FOREIGN KEY (job_name, process_date, sequence_no)
        REFERENCES vsam_batch_control(job_name, process_date, sequence_no)
);

-- ====================================================================
-- AUDIT/HISTORY CHANGE LOG (derived from HISTREC copybook)
-- Tracks before/after images of record changes
-- ====================================================================
CREATE TABLE vsam_change_history (
    portfolio_id      CHAR(8)         NOT NULL,
    history_date      CHAR(8)         NOT NULL,
    history_time      CHAR(6)         NOT NULL,
    sequence_no       CHAR(4)         NOT NULL,
    record_type       CHAR(2)         NOT NULL,
    action_code       CHAR(1)         NOT NULL,
    before_image      TEXT,
    after_image       TEXT,
    reason_code       CHAR(4),
    process_timestamp VARCHAR(26),
    process_user      VARCHAR(8),
    CONSTRAINT pk_vsam_change_history PRIMARY KEY (portfolio_id, history_date, history_time, sequence_no),
    CONSTRAINT chk_vsam_hist_rec_type CHECK (record_type IN ('PT', 'PS', 'TR')),
    CONSTRAINT chk_vsam_hist_action CHECK (action_code IN ('A', 'C', 'D'))
);

COMMENT ON TABLE vsam_change_history IS 'Change History - migrated from VSAM, tracks record changes with before/after images';
COMMENT ON COLUMN vsam_change_history.record_type IS 'PT=Portfolio, PS=Position, TR=Transaction';
COMMENT ON COLUMN vsam_change_history.action_code IS 'A=Add, C=Change, D=Delete';

COMMIT;
