-- Phase 1: DB2/VSAM to relational schema for the COBOL Legacy Benchmark Suite
-- Target: H2 (PostgreSQL-compatible syntax where possible)
-- Numeric types: DECIMAL(p,s) matches COBOL packed-decimal (COMP-3) precision/scale.

-- ------------------------------------------------------------------
-- DB2 tables from src/database/db2/db2-definitions.sql
-- ------------------------------------------------------------------

CREATE TABLE portfolio_master (
    portfolio_id      CHAR(8)         NOT NULL,
    account_type      CHAR(2)         NOT NULL,
    branch_id         CHAR(2)         NOT NULL,
    client_id         CHAR(10)        NOT NULL,
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    risk_level        CHAR(1)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    open_date         DATE            NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id)
);

CREATE TABLE investment_positions (
    portfolio_id      CHAR(8)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    cost_basis        DECIMAL(18,2)   NOT NULL,
    market_value      DECIMAL(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

CREATE TABLE transaction_history (
    transaction_id    CHAR(20)        NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    transaction_type  CHAR(2)         NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    price             DECIMAL(18,4)   NOT NULL,
    amount            DECIMAL(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    process_date      TIMESTAMP       NOT NULL,
    process_user      VARCHAR(8)      NOT NULL,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

CREATE INDEX idx_port_master_client ON portfolio_master (client_id, status);
CREATE INDEX idx_positions_date     ON investment_positions (position_date, portfolio_id);
CREATE INDEX idx_trans_hist_port    ON transaction_history (portfolio_id, transaction_date);
CREATE INDEX idx_trans_hist_date    ON transaction_history (transaction_date, portfolio_id);

-- ------------------------------------------------------------------
-- DB2 table from src/database/db2/POSHIST.sql
-- ------------------------------------------------------------------

CREATE TABLE poshist (
    account_no        CHAR(8)         NOT NULL,
    portfolio_id      CHAR(10)        NOT NULL,
    trans_date        DATE            NOT NULL,
    trans_time        TIME            NOT NULL,
    trans_type        CHAR(2)         NOT NULL,
    security_id       CHAR(12)        NOT NULL,
    quantity          DECIMAL(15,3)   NOT NULL,
    price             DECIMAL(15,3)   NOT NULL,
    amount            DECIMAL(15,2)   NOT NULL,
    fees              DECIMAL(15,2)   NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)   NOT NULL,
    cost_basis        DECIMAL(15,2)   NOT NULL,
    gain_loss         DECIMAL(15,2)   NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

CREATE INDEX poshist_ix1 ON poshist (security_id, trans_date);
CREATE INDEX poshist_ix2 ON poshist (process_date, program_id);

-- ------------------------------------------------------------------
-- DB2 table from src/database/db2/ERRLOG.sql
-- ------------------------------------------------------------------

CREATE TABLE errlog (
    error_timestamp   TIMESTAMP       NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    error_type        CHAR(1)         NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        CHAR(8)         NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    additional_info   VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);

CREATE INDEX errlog_ix1 ON errlog (process_date, error_severity);

-- ------------------------------------------------------------------
-- DB2 table from src/database/db2/RTNCODES.sql
-- ------------------------------------------------------------------

CREATE TABLE rtncodes (
    log_timestamp     TIMESTAMP       NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    return_code       INTEGER         NOT NULL,
    highest_code      INTEGER         NOT NULL,
    status_code       CHAR(1)         NOT NULL,
    message_text      VARCHAR(80),
    PRIMARY KEY (log_timestamp, program_id)
);

CREATE INDEX rtncodes_prg_idx ON rtncodes (program_id, log_timestamp);
CREATE INDEX rtncodes_sts_idx ON rtncodes (status_code, log_timestamp);

-- ------------------------------------------------------------------
-- VSAM KSDS files mapped from src/database/vsam/vsam-definitions.txt
-- and copybooks in src/copybook/common/
-- ------------------------------------------------------------------

CREATE TABLE vsam_portmstr (
    portfolio_id      CHAR(8)         NOT NULL,
    account_no        CHAR(10)        NOT NULL,
    client_name       VARCHAR(30)     NOT NULL,
    client_type       CHAR(1)         NOT NULL,
    create_date       DATE            NOT NULL,
    last_maint_date   DATE            NOT NULL,
    status            CHAR(1)         NOT NULL,
    total_value       DECIMAL(15,2)   NOT NULL,
    cash_balance      DECIMAL(15,2)   NOT NULL,
    last_user         CHAR(8)         NOT NULL,
    last_trans_date   DATE            NOT NULL,
    filler            CHAR(50),
    PRIMARY KEY (portfolio_id, account_no)
);

CREATE INDEX idx_vsam_portmstr_client ON vsam_portmstr (client_name);
CREATE INDEX idx_vsam_portmstr_status ON vsam_portmstr (status);

CREATE TABLE vsam_poshist (
    portfolio_id      CHAR(8)         NOT NULL,
    position_date     DATE            NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    quantity          DECIMAL(15,4)   NOT NULL,
    cost_basis        DECIMAL(15,2)   NOT NULL,
    market_value      DECIMAL(15,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   CHAR(8)         NOT NULL,
    filler            CHAR(50),
    PRIMARY KEY (portfolio_id, position_date, investment_id)
);

CREATE INDEX idx_vsam_poshist_invest ON vsam_poshist (investment_id, position_date);
CREATE INDEX idx_vsam_poshist_status  ON vsam_poshist (status);

CREATE TABLE vsam_tranhist (
    transaction_id    CHAR(20)        NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    sequence_number   CHAR(6)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    transaction_type  CHAR(2)         NOT NULL,
    quantity          DECIMAL(15,4)   NOT NULL,
    price             DECIMAL(15,4)   NOT NULL,
    amount            DECIMAL(15,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    process_date      TIMESTAMP       NOT NULL,
    process_user      CHAR(8)         NOT NULL,
    filler            CHAR(50),
    PRIMARY KEY (transaction_id)
);

CREATE INDEX idx_vsam_tranhist_portfolio ON vsam_tranhist (portfolio_id, transaction_date);
CREATE INDEX idx_vsam_tranhist_invest    ON vsam_tranhist (investment_id, transaction_date);

-- ------------------------------------------------------------------
-- Additional copybook tables from src/copybook/common/
-- ------------------------------------------------------------------

CREATE TABLE history_record (
    portfolio_id      CHAR(8)         NOT NULL,
    history_date      DATE            NOT NULL,
    history_time      TIME            NOT NULL,
    sequence_no       CHAR(4)         NOT NULL,
    record_type       CHAR(2)         NOT NULL,
    action_code       CHAR(1)         NOT NULL,
    before_image      VARCHAR(400)    NOT NULL,
    after_image       VARCHAR(400)    NOT NULL,
    reason_code       CHAR(4)         NOT NULL,
    process_timestamp TIMESTAMP       NOT NULL,
    process_user      CHAR(8)         NOT NULL,
    filler            CHAR(50),
    PRIMARY KEY (portfolio_id, history_date, history_time, sequence_no)
);

CREATE TABLE audit_log (
    id                BIGINT          GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    log_timestamp     TIMESTAMP       NOT NULL,
    system_id         CHAR(8)         NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    program           CHAR(8)         NOT NULL,
    terminal          CHAR(8)         NOT NULL,
    type              CHAR(4)         NOT NULL,
    action            CHAR(8)         NOT NULL,
    status            CHAR(4)         NOT NULL,
    portfolio_id      CHAR(8),
    account_no        CHAR(10),
    before_image      VARCHAR(100),
    after_image       VARCHAR(100),
    message           VARCHAR(100)
);

CREATE INDEX audit_log_timestamp_idx ON audit_log (log_timestamp);
