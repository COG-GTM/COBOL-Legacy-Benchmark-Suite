-- ============================================================================
-- 001_initial_schema.sql
-- PostgreSQL migration translated from DB2/VSAM/COBOL definitions.
--
-- Sources:
--   src/database/db2/db2-definitions.sql   — PORTFOLIO_MASTER, INVESTMENT_POSITIONS, TRANSACTION_HISTORY
--   src/database/db2/POSHIST.sql           — Position History (POSHIST) table
--   src/database/db2/ERRLOG.sql            — Error Logging table
--   src/database/db2/RTNCODES.sql          — Return Codes table
--   src/database/vsam/vsam-definitions.txt — PORTMSTR, TRANHIST, POSHIST VSAM clusters
--   src/copybook/db2/DBTBLS.cpy            — COBOL record layouts for DB2 tables
--   src/copybook/common/PORTFLIO.cpy       — Portfolio Master record (VSAM)
--   src/copybook/common/TRNREC.cpy         — Transaction record
--   src/copybook/common/POSREC.cpy         — Position record
--   src/copybook/common/AUDITLOG.cpy       — Audit trail record
--   src/copybook/common/RTNCODE.cpy        — Return code record
--   src/copybook/batch/BCHCTL.cpy          — Batch control record
-- ============================================================================

-- ============================================================================
-- portfolios
-- Source: DB2 PORTFOLIO_MASTER + VSAM PORTMSTR (PORTFLIO.cpy)
-- Merges DB2 relational columns with VSAM portfolio-master fields.
-- ============================================================================
CREATE TABLE portfolios (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Original COBOL key: PORT-ID PIC X(8)
    portfolio_id      VARCHAR(8)  NOT NULL UNIQUE,
    -- PORT-ACCOUNT-NO PIC X(10)
    account_number    VARCHAR(10) NOT NULL,
    -- DB2: ACCOUNT_TYPE CHAR(2), BRANCH_ID CHAR(2)
    account_type      VARCHAR(2)  NOT NULL,
    branch_id         VARCHAR(2)  NOT NULL,
    -- DB2: CLIENT_ID CHAR(10)
    client_id         VARCHAR(10) NOT NULL,
    -- PORT-CLIENT-NAME PIC X(30)
    client_name       VARCHAR(30),
    -- PORT-CLIENT-TYPE PIC X(1) — level-88: I/C/T
    client_type       VARCHAR(1),
    -- DB2: PORTFOLIO_NAME VARCHAR(50)
    portfolio_name    VARCHAR(50) NOT NULL,
    -- DB2: CURRENCY_CODE CHAR(3)
    currency_code     VARCHAR(3)  NOT NULL,
    -- DB2: RISK_LEVEL CHAR(1)
    risk_level        VARCHAR(1)  NOT NULL,
    -- DB2: STATUS CHAR(1) — level-88: A/C/S
    status            VARCHAR(1)  NOT NULL DEFAULT 'A',
    -- PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3
    total_value       DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- PORT-CASH-BALANCE PIC S9(13)V99 COMP-3
    cash_balance      DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- DB2: OPEN_DATE / PORT-CREATE-DATE
    open_date         DATE        NOT NULL DEFAULT CURRENT_DATE,
    close_date        DATE,
    last_maint_date   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_maint_user   VARCHAR(8)  NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- CHECK constraints from COBOL level-88 definitions
    -- PORTFLIO.cpy: PORT-STATUS 88 values A/C/S
    CONSTRAINT chk_portfolios_status
        CHECK (status IN ('A', 'C', 'S')),
    -- PORTFLIO.cpy: PORT-CLIENT-TYPE 88 values I/C/T
    CONSTRAINT chk_portfolios_client_type
        CHECK (client_type IS NULL OR client_type IN ('I', 'C', 'T'))
);

COMMENT ON TABLE portfolios IS
    'Portfolio master — translated from DB2 PORTFOLIO_MASTER + VSAM PORTMSTR (PORTFLIO.cpy)';

-- DB2 index: IDX_PORT_MASTER_CLIENT ON PORTFOLIO_MASTER (CLIENT_ID, STATUS)
CREATE INDEX idx_portfolios_client
    ON portfolios (client_id, status);


-- ============================================================================
-- positions
-- Source: DB2 INVESTMENT_POSITIONS + VSAM POSHIST key + POSREC.cpy
-- ============================================================================
CREATE TABLE positions (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id      UUID          NOT NULL REFERENCES portfolios(id),
    -- DB2: INVESTMENT_ID CHAR(10) / POS-INVESTMENT-ID PIC X(10)
    investment_id     VARCHAR(10)   NOT NULL,
    -- DB2: POSITION_DATE
    position_date     DATE          NOT NULL,
    -- DB2: QUANTITY DECIMAL(18,4) / POS-QUANTITY PIC S9(11)V9(4)
    quantity          DECIMAL(18,4) NOT NULL,
    -- DB2: COST_BASIS DECIMAL(18,2) / POS-COST-BASIS PIC S9(13)V9(2)
    cost_basis        DECIMAL(15,2) NOT NULL,
    -- DB2: MARKET_VALUE DECIMAL(18,2) / POS-MARKET-VALUE PIC S9(13)V9(2)
    market_value      DECIMAL(15,2) NOT NULL,
    -- DB2: CURRENCY_CODE CHAR(3) / POS-CURRENCY PIC X(3)
    currency_code     VARCHAR(3)    NOT NULL,
    -- POS-STATUS PIC X(1) — level-88: A/C/P
    status            VARCHAR(1)    NOT NULL DEFAULT 'A',
    last_maint_date   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_maint_user   VARCHAR(8)    NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- POSREC.cpy: POS-STATUS 88 values A/C/P
    CONSTRAINT chk_positions_status
        CHECK (status IN ('A', 'C', 'P')),
    -- Natural composite uniqueness from DB2 PK
    CONSTRAINT uq_positions_natural_key
        UNIQUE (portfolio_id, investment_id, position_date)
);

COMMENT ON TABLE positions IS
    'Investment positions — translated from DB2 INVESTMENT_POSITIONS + POSREC.cpy';

-- DB2 index: IDX_POSITIONS_DATE ON INVESTMENT_POSITIONS (POSITION_DATE, PORTFOLIO_ID)
CREATE INDEX idx_positions_date
    ON positions (position_date, portfolio_id);


-- ============================================================================
-- transactions
-- Source: DB2 TRANSACTION_HISTORY + VSAM TRANHIST + TRNREC.cpy
-- ============================================================================
CREATE TABLE transactions (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    -- DB2: TRANSACTION_ID CHAR(20) — format YYYYMMDDHHMMSS + 6-digit seq
    transaction_id      VARCHAR(20)   NOT NULL UNIQUE,
    portfolio_id        UUID          NOT NULL REFERENCES portfolios(id),
    -- DB2: TRANSACTION_DATE + TRANSACTION_TIME  (merged into timestamptz)
    transaction_date    DATE          NOT NULL,
    transaction_time    TIME          NOT NULL,
    -- DB2: INVESTMENT_ID CHAR(10) / TRN-INVESTMENT-ID PIC X(10)
    investment_id       VARCHAR(10)   NOT NULL,
    -- DB2: TRANSACTION_TYPE CHAR(2) — level-88: BU/SL/TR/FE
    transaction_type    VARCHAR(2)    NOT NULL,
    -- DB2: QUANTITY DECIMAL(18,4) / TRN-QUANTITY PIC S9(11)V9(4)
    quantity            DECIMAL(18,4) NOT NULL,
    -- DB2: PRICE DECIMAL(18,4) / TRN-PRICE PIC S9(11)V9(4)
    price               DECIMAL(18,4) NOT NULL,
    -- DB2: AMOUNT DECIMAL(18,2) / TRN-AMOUNT PIC S9(13)V9(2)
    amount              DECIMAL(15,2) NOT NULL,
    -- DB2: CURRENCY_CODE CHAR(3) / TRN-CURRENCY PIC X(3)
    currency_code       VARCHAR(3)    NOT NULL,
    -- DB2: STATUS CHAR(1) — level-88: P/D/F/R
    status              VARCHAR(1)    NOT NULL DEFAULT 'P',
    process_date        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    process_user        VARCHAR(8)    NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- db2-definitions.sql note: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    -- TRNREC.cpy: TRN-STATUS 88 values P/D/F/R
    CONSTRAINT chk_transactions_status
        CHECK (status IN ('P', 'D', 'F', 'R'))
);

COMMENT ON TABLE transactions IS
    'Transactions — translated from DB2 TRANSACTION_HISTORY + VSAM TRANHIST (TRNREC.cpy)';

-- DB2 index: IDX_TRANS_HIST_PORT ON TRANSACTION_HISTORY (PORTFOLIO_ID, TRANSACTION_DATE)
CREATE INDEX idx_transactions_portfolio
    ON transactions (portfolio_id, transaction_date);

-- DB2 index: IDX_TRANS_HIST_DATE ON TRANSACTION_HISTORY (TRANSACTION_DATE, PORTFOLIO_ID)
CREATE INDEX idx_transactions_date
    ON transactions (transaction_date, portfolio_id);


-- ============================================================================
-- transaction_history
-- Source: DB2 POSHIST table (POSHIST.sql) + DBTBLS.cpy POSHIST-RECORD
-- Stores detailed position-change history per transaction.
-- ============================================================================
CREATE TABLE transaction_history (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    -- POSHIST: ACCOUNT_NO CHAR(8)
    account_number    VARCHAR(8)    NOT NULL,
    -- POSHIST: PORTFOLIO_ID CHAR(10)
    portfolio_id      VARCHAR(10)   NOT NULL,
    -- POSHIST: TRANS_DATE / TRANS_TIME
    trans_date        DATE          NOT NULL,
    trans_time        TIME          NOT NULL,
    -- POSHIST: TRANS_TYPE CHAR(2) — BU/SL/TR
    trans_type        VARCHAR(2)    NOT NULL,
    -- POSHIST: SECURITY_ID CHAR(12)
    security_id       VARCHAR(12)   NOT NULL,
    -- POSHIST: QUANTITY DECIMAL(15,3)
    quantity          DECIMAL(15,3) NOT NULL,
    -- POSHIST: PRICE DECIMAL(15,3)
    price             DECIMAL(15,3) NOT NULL,
    -- POSHIST: AMOUNT DECIMAL(15,2)
    amount            DECIMAL(15,2) NOT NULL,
    -- POSHIST: FEES DECIMAL(15,2) DEFAULT 0
    fees              DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- POSHIST: TOTAL_AMOUNT DECIMAL(15,2)
    total_amount      DECIMAL(15,2) NOT NULL,
    -- POSHIST: COST_BASIS DECIMAL(15,2)
    cost_basis        DECIMAL(15,2) NOT NULL,
    -- POSHIST: GAIN_LOSS DECIMAL(15,2)
    gain_loss         DECIMAL(15,2) NOT NULL,
    process_date      DATE          NOT NULL,
    process_time      TIME          NOT NULL,
    program_id        VARCHAR(8)    NOT NULL,
    user_id           VARCHAR(8)    NOT NULL,
    audit_timestamp   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- POSHIST.sql comment: TRANS_TYPE BU/SL/TR
    CONSTRAINT chk_txn_history_trans_type
        CHECK (trans_type IN ('BU', 'SL', 'TR')),
    -- Natural composite key from DB2 PK (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
    CONSTRAINT uq_txn_history_natural_key
        UNIQUE (account_number, portfolio_id, trans_date, trans_time)
);

COMMENT ON TABLE transaction_history IS
    'Position-change history — translated from DB2 POSHIST (POSHIST.sql, DBTBLS.cpy)';

-- POSHIST_IX1 ON POSHIST (SECURITY_ID, TRANS_DATE)
CREATE INDEX idx_txn_history_security
    ON transaction_history (security_id, trans_date);

-- POSHIST_IX2 ON POSHIST (PROCESS_DATE, PROGRAM_ID)
CREATE INDEX idx_txn_history_process
    ON transaction_history (process_date, program_id);


-- ============================================================================
-- batch_control
-- Source: BCHCTL.cpy — Batch Control Record
-- Tracks batch job execution, sequencing, and dependencies.
-- ============================================================================
CREATE TABLE batch_control (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- BCT-JOB-NAME PIC X(8)
    job_id              VARCHAR(8)  NOT NULL,
    -- BCT-STATUS PIC X(1) — level-88: R/A/W/D/E
    status              VARCHAR(1)  NOT NULL DEFAULT 'R',
    -- BCT-STEP-NAME PIC X(8)
    step_name           VARCHAR(8),
    -- BCT-PROGRAM-NAME PIC X(8)
    program_name        VARCHAR(8),
    -- BCT-PROCESS-DATE PIC X(8)
    process_date        DATE        NOT NULL DEFAULT CURRENT_DATE,
    -- BCT-SEQUENCE-NO PIC 9(4)
    sequence_number     INTEGER     NOT NULL DEFAULT 0,
    start_time          TIMESTAMPTZ,
    end_time            TIMESTAMPTZ,
    records_processed   BIGINT      NOT NULL DEFAULT 0,
    -- BCT-RETURN-CODE PIC S9(4) COMP
    return_code         INTEGER,
    -- BCT-ERROR-DESC PIC X(80)
    error_description   VARCHAR(80),
    -- BCT-RESTART-COUNT PIC 9(2) COMP
    restart_count       INTEGER     NOT NULL DEFAULT 0,
    -- Serialised dependency / checkpoint data (replaces BCT-PREREQ-JOBS array)
    checkpoint_data     JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- BCHCTL.cpy: BCT-STATUS 88 values R/A/W/D/E
    CONSTRAINT chk_batch_status
        CHECK (status IN ('R', 'A', 'W', 'D', 'E')),
    -- Natural composite key from COBOL BCT-KEY
    CONSTRAINT uq_batch_control_key
        UNIQUE (job_id, process_date, sequence_number)
);

COMMENT ON TABLE batch_control IS
    'Batch job control — translated from BCHCTL.cpy (BCT-KEY: job + date + seq)';

CREATE INDEX idx_batch_control_status
    ON batch_control (status, process_date);


-- ============================================================================
-- error_log
-- Source: DB2 ERRLOG (ERRLOG.sql) + DBTBLS.cpy ERRLOG-RECORD
-- ============================================================================
CREATE TABLE error_log (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    -- ERRLOG: ERROR_TIMESTAMP
    error_timestamp     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- ERRLOG: PROGRAM_ID CHAR(8)
    program_id          VARCHAR(8)    NOT NULL,
    -- ERRLOG: ERROR_TYPE CHAR(1) — level-88: S/A/D
    error_type          VARCHAR(1)    NOT NULL,
    -- ERRLOG: ERROR_SEVERITY INTEGER — level-88: 1/2/3/4
    error_severity      INTEGER       NOT NULL,
    -- ERRLOG: ERROR_CODE CHAR(8)
    error_code          VARCHAR(8)    NOT NULL,
    -- ERRLOG: ERROR_MESSAGE VARCHAR(200)
    error_message       VARCHAR(200)  NOT NULL,
    process_date        DATE          NOT NULL DEFAULT CURRENT_DATE,
    process_time        TIME          NOT NULL DEFAULT CURRENT_TIME,
    -- ERRLOG: USER_ID CHAR(8)
    user_id             VARCHAR(8)    NOT NULL,
    -- ERRLOG: ADDITIONAL_INFO VARCHAR(500)
    additional_info     VARCHAR(500),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- DBTBLS.cpy: EL-ERROR-TYPE 88 values S/A/D
    CONSTRAINT chk_error_log_type
        CHECK (error_type IN ('S', 'A', 'D')),
    -- DBTBLS.cpy: EL-ERROR-SEVERITY 88 values 1..4
    CONSTRAINT chk_error_log_severity
        CHECK (error_severity BETWEEN 1 AND 4)
);

COMMENT ON TABLE error_log IS
    'Application error log — translated from DB2 ERRLOG (ERRLOG.sql, DBTBLS.cpy)';

-- ERRLOG_IX1 ON ERRLOG (PROCESS_DATE, ERROR_SEVERITY DESC)
CREATE INDEX idx_error_log_date_severity
    ON error_log (process_date, error_severity DESC);

-- ERRLOG_PK index equivalent (for time-based lookups)
CREATE INDEX idx_error_log_timestamp
    ON error_log (error_timestamp, program_id);


-- ============================================================================
-- audit_trail
-- Source: AUDITLOG.cpy — Audit Trail Record
-- Uses JSONB for before/after images (replacing fixed PIC X(100) fields).
-- ============================================================================
CREATE TABLE audit_trail (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- AUD-TIMESTAMP PIC X(26)
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- AUD-USER-ID PIC X(8)
    user_id         VARCHAR(8)  NOT NULL,
    -- AUD-ACTION PIC X(8) — level-88 values
    action          VARCHAR(8)  NOT NULL,
    -- Generalised entity tracking (replaces AUD-KEY-INFO)
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       VARCHAR(50) NOT NULL,
    -- JSONB replaces AUD-BEFORE-IMAGE / AUD-AFTER-IMAGE PIC X(100)
    old_value       JSONB,
    new_value       JSONB,
    -- AUD-TYPE PIC X(4) — level-88: TRAN/USER/SYST
    audit_type      VARCHAR(4),
    -- AUD-STATUS PIC X(4) — level-88: SUCC/FAIL/WARN
    audit_status    VARCHAR(4),
    -- AUD-SYSTEM-ID PIC X(8)
    system_id       VARCHAR(8),
    -- AUD-PROGRAM PIC X(8)
    program_id      VARCHAR(8),
    -- AUD-TERMINAL PIC X(8)
    terminal_id     VARCHAR(8),
    -- AUD-MESSAGE PIC X(100)
    message         VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- AUDITLOG.cpy: AUD-ACTION 88 values
    CONSTRAINT chk_audit_action
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'INQUIRE', 'LOGIN', 'LOGOUT', 'STARTUP', 'SHUTDOWN')),
    -- AUDITLOG.cpy: AUD-TYPE 88 values
    CONSTRAINT chk_audit_type
        CHECK (audit_type IS NULL OR audit_type IN ('TRAN', 'USER', 'SYST')),
    -- AUDITLOG.cpy: AUD-STATUS 88 values
    CONSTRAINT chk_audit_status
        CHECK (audit_status IS NULL OR audit_status IN ('SUCC', 'FAIL', 'WARN'))
);

COMMENT ON TABLE audit_trail IS
    'Audit trail — translated from AUDITLOG.cpy (AUD-RECORD)';

CREATE INDEX idx_audit_trail_entity
    ON audit_trail (entity_type, entity_id);

CREATE INDEX idx_audit_trail_user
    ON audit_trail (user_id, event_timestamp);

CREATE INDEX idx_audit_trail_timestamp
    ON audit_trail (event_timestamp);


-- ============================================================================
-- return_codes
-- Source: DB2 RTNCODES (RTNCODES.sql) + RTNCODE.cpy
-- ============================================================================
CREATE TABLE return_codes (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- RTNCODES: TIMESTAMP
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- RTNCODES: PROGRAM_ID CHAR(8)
    program_id      VARCHAR(8)  NOT NULL,
    -- RTNCODES: RETURN_CODE INTEGER / RC-CURRENT-CODE PIC S9(4)
    return_code     INTEGER     NOT NULL,
    -- RTNCODES: HIGHEST_CODE INTEGER / RC-HIGHEST-CODE PIC S9(4)
    highest_code    INTEGER     NOT NULL,
    -- RTNCODES: STATUS_CODE CHAR(1) — level-88: S/W/E/F
    status_code     VARCHAR(1)  NOT NULL,
    -- RTNCODES: MESSAGE_TEXT VARCHAR(80) / RC-MESSAGE PIC X(80)
    message_text    VARCHAR(80),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- RTNCODE.cpy: RC-STATUS 88 values S/W/E/F
    CONSTRAINT chk_return_codes_status
        CHECK (status_code IN ('S', 'W', 'E', 'F')),
    -- Natural composite key from DB2 PK
    CONSTRAINT uq_return_codes_natural_key
        UNIQUE (recorded_at, program_id)
);

COMMENT ON TABLE return_codes IS
    'Program return codes — translated from DB2 RTNCODES (RTNCODES.sql, RTNCODE.cpy)';

CREATE INDEX idx_return_codes_program
    ON return_codes (program_id, recorded_at);

CREATE INDEX idx_return_codes_status
    ON return_codes (status_code, recorded_at);
