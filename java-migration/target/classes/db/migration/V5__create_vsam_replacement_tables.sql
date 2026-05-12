-- Tables replacing VSAM files (from vsam-definitions.txt)

-- Audit Log table (replaces AUDITLOG VSAM)
CREATE TABLE audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_timestamp TIMESTAMP NOT NULL,
    system_id       VARCHAR(20),
    user_id         VARCHAR(8),
    program         VARCHAR(8),
    terminal        VARCHAR(8),
    audit_type      VARCHAR(4) NOT NULL,
    audit_action    VARCHAR(8) NOT NULL,
    audit_status    VARCHAR(4) NOT NULL,
    portfolio_id    VARCHAR(8),
    account_no      VARCHAR(10),
    before_image    VARCHAR(500),
    after_image     VARCHAR(500),
    message         VARCHAR(200)
);

CREATE INDEX idx_audit_portfolio ON audit_log(portfolio_id);
CREATE INDEX idx_audit_timestamp ON audit_log(audit_timestamp);

-- History Record table (replaces HISTREC VSAM)
CREATE TABLE history_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id   VARCHAR(8) NOT NULL,
    history_date   VARCHAR(8) NOT NULL,
    history_time   VARCHAR(6),
    sequence_no    VARCHAR(4),
    record_type    VARCHAR(2) NOT NULL,
    action_code    CHAR(1) NOT NULL,
    before_image   CLOB,
    after_image    CLOB,
    reason_code    VARCHAR(4),
    process_date   TIMESTAMP,
    process_user   VARCHAR(8)
);

CREATE INDEX idx_history_portfolio ON history_record(portfolio_id);

-- Batch Control table
CREATE TABLE batch_control (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name       VARCHAR(8) NOT NULL,
    process_date   VARCHAR(8) NOT NULL,
    sequence_no    INT NOT NULL DEFAULT 1,
    status         CHAR(1) NOT NULL DEFAULT 'R',
    step_name      VARCHAR(8),
    program_name   VARCHAR(8),
    start_time     VARCHAR(8),
    end_time       VARCHAR(8),
    return_code    INT,
    error_desc     VARCHAR(80),
    restart_count  INT DEFAULT 0,
    attempt_ts     TIMESTAMP,
    complete_ts    TIMESTAMP
);

CREATE INDEX idx_batch_job ON batch_control(job_name, process_date);

-- Checkpoint Control table
CREATE TABLE checkpoint_control (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id        VARCHAR(8) NOT NULL,
    run_date          VARCHAR(8),
    run_time          VARCHAR(6),
    status            CHAR(1) NOT NULL DEFAULT 'I',
    records_read      BIGINT DEFAULT 0,
    records_processed BIGINT DEFAULT 0,
    records_error     BIGINT DEFAULT 0,
    restart_count     INT DEFAULT 0,
    last_key          VARCHAR(50),
    last_time         TIMESTAMP,
    phase             VARCHAR(2) DEFAULT '00',
    commit_frequency  INT DEFAULT 1000,
    max_errors        INT DEFAULT 100,
    max_restarts      INT DEFAULT 3,
    restart_mode      CHAR(1) DEFAULT 'N'
);

CREATE INDEX idx_checkpoint_prog ON checkpoint_control(program_id, run_date);
