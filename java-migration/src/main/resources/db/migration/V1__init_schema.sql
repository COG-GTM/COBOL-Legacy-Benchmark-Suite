-- ============================================================
-- Flyway Migration V1: Initial Schema
-- Translated from COBOL DB2 definitions and VSAM file layouts
-- ============================================================

CREATE TABLE portfolio_master (
    portfolio_id      VARCHAR(8)      NOT NULL,
    account_no        VARCHAR(10),
    account_type      VARCHAR(2)      NOT NULL DEFAULT 'TX',
    branch_id         VARCHAR(2)      NOT NULL DEFAULT '01',
    client_id         VARCHAR(10)     NOT NULL,
    client_name       VARCHAR(30),
    client_type       VARCHAR(1),
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     VARCHAR(3)      NOT NULL DEFAULT 'USD',
    risk_level        VARCHAR(1)      NOT NULL DEFAULT 'M',
    status            VARCHAR(1)      NOT NULL DEFAULT 'A',
    open_date         DATE            NOT NULL,
    close_date        DATE,
    total_value       DECIMAL(15,2)   DEFAULT 0,
    cash_balance      DECIMAL(15,2)   DEFAULT 0,
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id)
);

CREATE TABLE investment_positions (
    portfolio_id      VARCHAR(8)      NOT NULL,
    investment_id     VARCHAR(10)     NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    cost_basis        DECIMAL(18,2)   NOT NULL,
    market_value      DECIMAL(18,2)   NOT NULL,
    currency_code     VARCHAR(3)      NOT NULL DEFAULT 'USD',
    status            VARCHAR(1)      DEFAULT 'A',
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

CREATE TABLE transaction_history (
    transaction_id    VARCHAR(20)     NOT NULL,
    portfolio_id      VARCHAR(8)      NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     VARCHAR(10)     NOT NULL,
    transaction_type  VARCHAR(2)      NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    price             DECIMAL(18,4)   NOT NULL,
    amount            DECIMAL(18,2)   NOT NULL,
    currency_code     VARCHAR(3)      NOT NULL DEFAULT 'USD',
    status            VARCHAR(1)      NOT NULL DEFAULT 'P',
    process_date      TIMESTAMP       NOT NULL,
    process_user      VARCHAR(8)      NOT NULL,
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

CREATE TABLE history_record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id      VARCHAR(8)      NOT NULL,
    history_date      VARCHAR(8),
    history_time      VARCHAR(6),
    seq_no            VARCHAR(4),
    record_type       VARCHAR(2),
    action_code       VARCHAR(1),
    before_image      VARCHAR(400),
    after_image       VARCHAR(400),
    reason_code       VARCHAR(4),
    process_date      TIMESTAMP,
    process_user      VARCHAR(8)
);

CREATE TABLE audit_log (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_timestamp   TIMESTAMP       NOT NULL,
    system_id         VARCHAR(8),
    user_id           VARCHAR(8),
    program_name      VARCHAR(8),
    terminal_id       VARCHAR(8),
    audit_type        VARCHAR(4)      NOT NULL,
    audit_action      VARCHAR(8)      NOT NULL,
    audit_status      VARCHAR(4)      NOT NULL,
    portfolio_id      VARCHAR(8),
    account_no        VARCHAR(10),
    before_image      VARCHAR(100),
    after_image       VARCHAR(100),
    message           VARCHAR(200)
);

CREATE TABLE error_log (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_timestamp   TIMESTAMP       NOT NULL,
    program_id        VARCHAR(8)      NOT NULL,
    error_type        VARCHAR(1)      NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        VARCHAR(8)      NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    additional_info   VARCHAR(500),
    user_id           VARCHAR(8)
);

CREATE TABLE batch_control (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name          VARCHAR(8)      NOT NULL,
    process_date      VARCHAR(8)      NOT NULL,
    sequence_no       INTEGER         DEFAULT 0,
    status            VARCHAR(1)      NOT NULL DEFAULT 'R',
    step_name         VARCHAR(8),
    program_name      VARCHAR(8),
    start_time        TIMESTAMP,
    end_time          TIMESTAMP,
    return_code       INTEGER         DEFAULT 0,
    error_desc        VARCHAR(80),
    restart_count     INTEGER         DEFAULT 0,
    records_read      BIGINT          DEFAULT 0,
    records_processed BIGINT          DEFAULT 0,
    records_error     BIGINT          DEFAULT 0
);

CREATE TABLE checkpoint_restart (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id        VARCHAR(8)      NOT NULL,
    run_date          VARCHAR(8),
    run_time          VARCHAR(6),
    status            VARCHAR(1),
    records_read      BIGINT          DEFAULT 0,
    records_processed BIGINT          DEFAULT 0,
    records_error     BIGINT          DEFAULT 0,
    restart_count     INTEGER         DEFAULT 0,
    last_key          VARCHAR(50),
    last_checkpoint_time TIMESTAMP,
    phase             VARCHAR(2),
    commit_frequency  INTEGER         DEFAULT 1000,
    max_errors        INTEGER         DEFAULT 100
);

-- Indexes (translated from DB2 index definitions)
CREATE INDEX idx_port_master_client ON portfolio_master (client_id, status);
CREATE INDEX idx_positions_date ON investment_positions (position_date, portfolio_id);
CREATE INDEX idx_trans_hist_port ON transaction_history (portfolio_id, transaction_date);
CREATE INDEX idx_trans_hist_date ON transaction_history (transaction_date, portfolio_id);
CREATE INDEX idx_error_log_date ON error_log (error_timestamp, program_id);
CREATE INDEX idx_audit_log_date ON audit_log (audit_timestamp);
CREATE INDEX idx_batch_control_job ON batch_control (job_name, process_date);
