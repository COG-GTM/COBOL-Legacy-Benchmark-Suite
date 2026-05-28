-- Portfolio Master Table
CREATE TABLE portfolios (
    portfolio_id    VARCHAR(8)      NOT NULL PRIMARY KEY,
    account_no      VARCHAR(10)     NOT NULL,
    client_name     VARCHAR(30),
    client_type     VARCHAR(1),
    create_date     VARCHAR(8),
    last_maint      VARCHAR(8),
    status          VARCHAR(1)      NOT NULL,
    total_value     DECIMAL(18,2),
    cash_balance    DECIMAL(18,2),
    last_user       VARCHAR(8),
    last_trans      VARCHAR(8)
);

-- Transaction Records Table
CREATE TABLE transaction_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    trn_date            VARCHAR(8)      NOT NULL,
    trn_time            VARCHAR(6)      NOT NULL,
    portfolio_id        VARCHAR(8)      NOT NULL,
    sequence_no         VARCHAR(6)      NOT NULL,
    investment_id       VARCHAR(10),
    trn_type            VARCHAR(2)      NOT NULL,
    quantity            DECIMAL(18,4)   NOT NULL,
    price               DECIMAL(18,4)   NOT NULL,
    amount              DECIMAL(18,2)   NOT NULL,
    currency            VARCHAR(3),
    status              VARCHAR(10)     NOT NULL,
    process_date        TIMESTAMP,
    process_user        VARCHAR(8),
    error_message       VARCHAR(500),
    adjudication_status VARCHAR(20),
    fee_amount          DECIMAL(18,2),
    settlement_amount   DECIMAL(18,2),
    cost_basis_adjustment DECIMAL(18,2)
);

-- Position Records Table
CREATE TABLE positions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    VARCHAR(8)      NOT NULL,
    position_date   VARCHAR(8),
    investment_id   VARCHAR(10)     NOT NULL,
    quantity        DECIMAL(18,4)   NOT NULL,
    cost_basis      DECIMAL(18,2)   NOT NULL,
    market_value    DECIMAL(18,2)   NOT NULL,
    currency        VARCHAR(3),
    status          VARCHAR(1)      NOT NULL,
    last_maint_date TIMESTAMP,
    last_maint_user VARCHAR(8),
    realized_gain_loss DECIMAL(18,2)
);

-- Position History Table
CREATE TABLE position_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no      VARCHAR(10)     NOT NULL,
    portfolio_id    VARCHAR(10)     NOT NULL,
    trans_date      VARCHAR(10)     NOT NULL,
    trans_time      VARCHAR(8)      NOT NULL,
    trans_type      VARCHAR(2)      NOT NULL,
    security_id     VARCHAR(12)     NOT NULL,
    quantity        DECIMAL(18,3)   NOT NULL,
    price           DECIMAL(18,3)   NOT NULL,
    amount          DECIMAL(18,2)   NOT NULL,
    fees            DECIMAL(18,2),
    total_amount    DECIMAL(18,2)   NOT NULL,
    cost_basis      DECIMAL(18,2)   NOT NULL,
    gain_loss       DECIMAL(18,2)   NOT NULL,
    process_date    VARCHAR(10),
    process_time    VARCHAR(8),
    program_id      VARCHAR(8),
    user_id         VARCHAR(8),
    audit_timestamp TIMESTAMP
);

-- Batch Control Records Table
CREATE TABLE batch_control_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name        VARCHAR(8)      NOT NULL,
    process_date    VARCHAR(8)      NOT NULL,
    sequence_no     INTEGER,
    status          VARCHAR(1)      NOT NULL,
    step_name       VARCHAR(8),
    program_name    VARCHAR(8),
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    return_code     INTEGER,
    error_desc      VARCHAR(80),
    restart_count   INTEGER,
    records_read    BIGINT,
    records_written BIGINT
);

-- Checkpoint Control Table
CREATE TABLE checkpoint_controls (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id          VARCHAR(8)      NOT NULL,
    run_date            VARCHAR(8),
    run_time            VARCHAR(6),
    status              VARCHAR(1)      NOT NULL,
    records_read        BIGINT,
    records_processed   BIGINT,
    records_error       BIGINT,
    restart_count       INTEGER,
    last_key            VARCHAR(50),
    last_time           TIMESTAMP,
    phase               VARCHAR(2),
    commit_frequency    INTEGER,
    max_errors          INTEGER
);

-- Audit Records Table
CREATE TABLE audit_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_timestamp TIMESTAMP       NOT NULL,
    system_id       VARCHAR(8),
    user_id         VARCHAR(8),
    program_name    VARCHAR(8),
    terminal_id     VARCHAR(8),
    audit_type      VARCHAR(4)      NOT NULL,
    action          VARCHAR(8)      NOT NULL,
    status          VARCHAR(4),
    portfolio_id    VARCHAR(8),
    account_no      VARCHAR(10),
    before_image    VARCHAR(100),
    after_image     VARCHAR(100),
    message         VARCHAR(100)
);

-- Error Log Table
CREATE TABLE error_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_timestamp TIMESTAMP       NOT NULL,
    program_id      VARCHAR(8)      NOT NULL,
    error_type      VARCHAR(1)      NOT NULL,
    error_severity  INTEGER         NOT NULL,
    error_code      VARCHAR(8)      NOT NULL,
    error_message   VARCHAR(200)    NOT NULL,
    process_date    TIMESTAMP,
    user_id         VARCHAR(8),
    additional_info VARCHAR(500)
);

-- History Records (staging table for history load)
CREATE TABLE history_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no      VARCHAR(10)     NOT NULL,
    portfolio_id    VARCHAR(10)     NOT NULL,
    trans_date      VARCHAR(10)     NOT NULL,
    trans_time      VARCHAR(8)      NOT NULL,
    trans_type      VARCHAR(2)      NOT NULL,
    security_id     VARCHAR(12)     NOT NULL,
    quantity        DECIMAL(18,4)   NOT NULL,
    price           DECIMAL(18,4)   NOT NULL,
    amount          DECIMAL(18,2)   NOT NULL,
    fees            DECIMAL(18,2),
    total_amount    DECIMAL(18,2)   NOT NULL,
    cost_basis      DECIMAL(18,2)   NOT NULL,
    gain_loss       DECIMAL(18,2)   NOT NULL,
    status          VARCHAR(10)
);

-- Indexes
CREATE INDEX idx_trn_status ON transaction_records (status);
CREATE INDEX idx_trn_portfolio ON transaction_records (portfolio_id, trn_date);
CREATE INDEX idx_pos_portfolio ON positions (portfolio_id, investment_id);
CREATE INDEX idx_poshist_portfolio ON position_history (portfolio_id, trans_date);
CREATE INDEX idx_errlog_date ON error_logs (process_date, error_severity);
CREATE INDEX idx_hist_status ON history_records (status);
