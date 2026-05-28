-- Portfolio table (from PORTFLIO.cpy)
CREATE TABLE portfolio (
    portfolio_id   VARCHAR(8)     NOT NULL PRIMARY KEY,
    account_no     VARCHAR(10)    NOT NULL,
    client_name    VARCHAR(30),
    client_type    VARCHAR(1),
    create_date    DATE,
    last_maint_date DATE,
    status         VARCHAR(10)    NOT NULL,
    total_value    DECIMAL(15,2),
    cash_balance   DECIMAL(15,2),
    last_user      VARCHAR(8),
    last_trans_date DATE
);

-- Position table (from POSREC.cpy)
CREATE TABLE position (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    VARCHAR(8)     NOT NULL,
    position_date   DATE           NOT NULL,
    investment_id   VARCHAR(10)    NOT NULL,
    quantity        DECIMAL(15,4),
    cost_basis      DECIMAL(15,2),
    market_value    DECIMAL(15,2),
    currency        VARCHAR(3),
    status          VARCHAR(10)    NOT NULL,
    last_maint_date TIMESTAMP,
    last_maint_user VARCHAR(8)
);

-- Transaction record table (from TRNREC.cpy)
CREATE TABLE transaction_record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_date  DATE           NOT NULL,
    transaction_time  TIME,
    portfolio_id      VARCHAR(8)     NOT NULL,
    sequence_no       VARCHAR(6),
    investment_id     VARCHAR(10),
    transaction_type  VARCHAR(10)    NOT NULL,
    quantity          DECIMAL(15,4),
    price             DECIMAL(15,4),
    amount            DECIMAL(15,2),
    currency          VARCHAR(3),
    status            VARCHAR(1),
    process_date      TIMESTAMP,
    process_user      VARCHAR(8)
);

-- Audit record table (from AUDITLOG.cpy)
CREATE TABLE audit_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp     TIMESTAMP      NOT NULL,
    system_id     VARCHAR(8),
    user_id       VARCHAR(8),
    program       VARCHAR(8),
    terminal      VARCHAR(8),
    audit_type    VARCHAR(4)     NOT NULL,
    action        VARCHAR(8)     NOT NULL,
    status        VARCHAR(4)     NOT NULL,
    portfolio_id  VARCHAR(8),
    account_no    VARCHAR(10),
    before_image  VARCHAR(500),
    after_image   VARCHAR(500),
    message       VARCHAR(500)
);

-- Error log table (from DBTBLS.cpy ERRLOG-RECORD)
CREATE TABLE error_log (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_timestamp  TIMESTAMP      NOT NULL,
    program_id       VARCHAR(8),
    error_type       VARCHAR(1)     NOT NULL,
    error_severity   INT,
    error_code       VARCHAR(8),
    error_message    VARCHAR(500),
    process_date     TIMESTAMP,
    user_id          VARCHAR(8),
    additional_info  VARCHAR(1000)
);

-- Batch control record table (from BCHCTL.cpy)
CREATE TABLE batch_control_record (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name            VARCHAR(8)     NOT NULL,
    process_date        VARCHAR(8)     NOT NULL,
    sequence_no         INT,
    status              VARCHAR(10)    NOT NULL,
    step_name           VARCHAR(8),
    program_name        VARCHAR(8),
    start_time          VARCHAR(8),
    end_time            VARCHAR(8),
    return_code         INT,
    error_desc          VARCHAR(200),
    restart_count       INT,
    attempt_timestamp   TIMESTAMP,
    complete_timestamp  TIMESTAMP
);

-- Position history table (from DBTBLS.cpy POSHIST-RECORD)
CREATE TABLE position_history (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no       VARCHAR(8),
    portfolio_id     VARCHAR(10),
    trans_date       VARCHAR(10),
    trans_time       VARCHAR(8),
    trans_type       VARCHAR(2),
    security_id      VARCHAR(12),
    quantity         DECIMAL(15,3),
    price            DECIMAL(15,3),
    amount           DECIMAL(15,2),
    fees             DECIMAL(15,2),
    total_amount     DECIMAL(15,2),
    cost_basis       DECIMAL(15,2),
    gain_loss        DECIMAL(15,2),
    process_date     VARCHAR(10),
    process_time     VARCHAR(8),
    program_id       VARCHAR(8),
    user_id          VARCHAR(8),
    audit_timestamp  TIMESTAMP
);

-- Checkpoint control table (from CKPRST.cpy)
CREATE TABLE checkpoint_control (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id        VARCHAR(8)     NOT NULL,
    run_date          VARCHAR(8)     NOT NULL,
    run_time          VARCHAR(6),
    status            VARCHAR(1)     NOT NULL,
    records_read      BIGINT,
    records_processed BIGINT,
    records_error     BIGINT,
    restart_count     INT,
    last_key          VARCHAR(50),
    last_time         TIMESTAMP,
    phase             VARCHAR(2),
    commit_frequency  INT,
    max_errors        INT,
    max_restarts      INT
);

-- Archive tables for maintenance
CREATE TABLE transaction_record_archive (
    id                BIGINT PRIMARY KEY,
    transaction_date  DATE           NOT NULL,
    transaction_time  TIME,
    portfolio_id      VARCHAR(8)     NOT NULL,
    sequence_no       VARCHAR(6),
    investment_id     VARCHAR(10),
    transaction_type  VARCHAR(10)    NOT NULL,
    quantity          DECIMAL(15,4),
    price             DECIMAL(15,4),
    amount            DECIMAL(15,2),
    currency          VARCHAR(3),
    status            VARCHAR(1),
    process_date      TIMESTAMP,
    process_user      VARCHAR(8),
    archived_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_position_portfolio ON position(portfolio_id);
CREATE INDEX idx_position_status ON position(status);
CREATE INDEX idx_transaction_portfolio ON transaction_record(portfolio_id);
CREATE INDEX idx_transaction_date ON transaction_record(transaction_date);
CREATE INDEX idx_audit_timestamp ON audit_record(timestamp);
CREATE INDEX idx_audit_action ON audit_record(action);
CREATE INDEX idx_error_timestamp ON error_log(error_timestamp);
CREATE INDEX idx_batch_status ON batch_control_record(status);
CREATE INDEX idx_checkpoint_program ON checkpoint_control(program_id);
