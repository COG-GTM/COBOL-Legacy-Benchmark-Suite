-- Investment Portfolio Management System - Initial Schema
-- Migrated from COBOL DB2 definitions

-- Portfolio Master Table (from db2-definitions.sql + PORTFLIO.cpy)
CREATE TABLE portfolio_master (
    portfolio_id      VARCHAR(8)      NOT NULL,
    account_no        VARCHAR(10),
    client_name       VARCHAR(30),
    client_type       VARCHAR(15),
    create_date       DATE,
    last_maint_date   TIMESTAMP,
    status            VARCHAR(15)     NOT NULL,
    total_value       DECIMAL(15,2),
    cash_balance      DECIMAL(15,2),
    last_user         VARCHAR(8),
    account_type      VARCHAR(2),
    branch_id         VARCHAR(2),
    client_id         VARCHAR(10),
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     VARCHAR(3),
    risk_level        VARCHAR(1),
    open_date         DATE,
    close_date        DATE,
    PRIMARY KEY (portfolio_id)
);

CREATE INDEX idx_port_master_client ON portfolio_master (client_id, status);

-- Investment Positions Table (from db2-definitions.sql + POSREC.cpy)
CREATE TABLE investment_positions (
    portfolio_id      VARCHAR(8)      NOT NULL,
    investment_id     VARCHAR(10)     NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    cost_basis        DECIMAL(18,2)   NOT NULL,
    market_value      DECIMAL(18,2)   NOT NULL,
    currency_code     VARCHAR(3)      NOT NULL,
    status            VARCHAR(10),
    last_maint_date   TIMESTAMP,
    last_maint_user   VARCHAR(8),
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

CREATE INDEX idx_positions_date ON investment_positions (position_date, portfolio_id);

-- Transaction History Table (from db2-definitions.sql + TRNREC.cpy)
CREATE TABLE transaction_history (
    transaction_id    VARCHAR(20)     NOT NULL,
    portfolio_id      VARCHAR(8)      NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     VARCHAR(10)     NOT NULL,
    transaction_type  VARCHAR(10)     NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    price             DECIMAL(18,4)   NOT NULL,
    amount            DECIMAL(18,2)   NOT NULL,
    currency_code     VARCHAR(3)      NOT NULL,
    status            VARCHAR(10)     NOT NULL,
    process_date      TIMESTAMP,
    process_user      VARCHAR(8),
    PRIMARY KEY (transaction_id),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

CREATE INDEX idx_trans_hist_port ON transaction_history (portfolio_id, transaction_date);
CREATE INDEX idx_trans_hist_date ON transaction_history (transaction_date, portfolio_id);

-- Position History Table (from POSHIST.sql + DBTBLS.cpy)
CREATE TABLE position_history (
    account_no        VARCHAR(8)      NOT NULL,
    portfolio_id      VARCHAR(10)     NOT NULL,
    trans_date        DATE            NOT NULL,
    trans_time        TIME            NOT NULL,
    trans_type        VARCHAR(2)      NOT NULL,
    security_id       VARCHAR(12)     NOT NULL,
    quantity          DECIMAL(15,3)   NOT NULL,
    price             DECIMAL(15,3)   NOT NULL,
    amount            DECIMAL(15,2)   NOT NULL,
    fees              DECIMAL(15,2)   NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)   NOT NULL,
    cost_basis        DECIMAL(15,2)   NOT NULL,
    gain_loss         DECIMAL(15,2)   NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    program_id        VARCHAR(8)      NOT NULL,
    user_id           VARCHAR(8)      NOT NULL,
    audit_timestamp   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
);

CREATE INDEX idx_poshist_security ON position_history (security_id, trans_date);
CREATE INDEX idx_poshist_process ON position_history (process_date, program_id);

-- Error Log Table (from ERRLOG.sql + DBTBLS.cpy)
CREATE TABLE error_log (
    error_timestamp   TIMESTAMP       NOT NULL,
    program_id        VARCHAR(8)      NOT NULL,
    error_type        VARCHAR(15)     NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        VARCHAR(8)      NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    user_id           VARCHAR(8)      NOT NULL,
    additional_info   VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);

CREATE INDEX idx_errlog_date_sev ON error_log (process_date, error_severity);

-- Audit Log Table (from AUDITLOG.cpy)
CREATE TABLE audit_log (
    id                BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audit_timestamp   TIMESTAMP       NOT NULL,
    system_id         VARCHAR(8),
    user_id           VARCHAR(8),
    program           VARCHAR(8),
    terminal          VARCHAR(8),
    audit_type        VARCHAR(20),
    audit_action      VARCHAR(20),
    audit_status      VARCHAR(10),
    portfolio_id      VARCHAR(8),
    account_no        VARCHAR(10),
    before_image      VARCHAR(100),
    after_image       VARCHAR(100),
    message           VARCHAR(100)
);

CREATE INDEX idx_audit_timestamp ON audit_log (audit_timestamp);
CREATE INDEX idx_audit_portfolio ON audit_log (portfolio_id);

-- Authorization File Table (from SECMGR.cbl)
CREATE TABLE auth_file (
    id                BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           VARCHAR(8)      NOT NULL,
    resource          VARCHAR(8)      NOT NULL,
    access_type       VARCHAR(8)      NOT NULL
);

CREATE INDEX idx_auth_user ON auth_file (user_id, resource, access_type);

-- Batch Control Table (from BCHCTL.cpy)
CREATE TABLE batch_control (
    job_name          VARCHAR(8)      NOT NULL,
    process_date      VARCHAR(8)      NOT NULL,
    sequence_no       INTEGER         NOT NULL,
    status            VARCHAR(10)     NOT NULL,
    step_name         VARCHAR(8),
    program_name      VARCHAR(8),
    start_time        VARCHAR(8),
    end_time          VARCHAR(8),
    prereq_count      INTEGER         DEFAULT 0,
    prereq_jobs       VARCHAR(1000),
    return_code       INTEGER         DEFAULT 0,
    error_desc        VARCHAR(80),
    restart_count     INTEGER         DEFAULT 0,
    attempt_timestamp TIMESTAMP,
    complete_timestamp TIMESTAMP,
    PRIMARY KEY (job_name, process_date, sequence_no)
);

-- Views (from db2-definitions.sql)
CREATE VIEW active_portfolios AS
    SELECT *
    FROM portfolio_master
    WHERE status = 'ACTIVE'
    AND (close_date IS NULL OR close_date > CURRENT_DATE);

CREATE VIEW current_positions AS
    SELECT p.*, pm.portfolio_name, pm.client_id
    FROM investment_positions p
    JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE p.position_date = CURRENT_DATE - INTERVAL '1' DAY;
