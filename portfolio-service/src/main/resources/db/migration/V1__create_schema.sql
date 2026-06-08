-- V1: Initial schema — translated from COBOL copybooks and DB2 DDL
-- Source: src/copybook/common/PORTFLIO.cpy, POSREC.cpy, TRNREC.cpy, HISTREC.cpy,
--         AUDITLOG.cpy, src/copybook/db2/DBTBLS.cpy, src/database/db2/*.sql

CREATE TABLE portfolio (
    port_id         VARCHAR(8)     PRIMARY KEY,
    account_no      VARCHAR(10)    NOT NULL,
    client_name     VARCHAR(30),
    client_type     VARCHAR(12),
    create_date     DATE,
    last_maintenance DATE,
    status          VARCHAR(10),
    total_value     DECIMAL(15,2)  DEFAULT 0,
    cash_balance    DECIMAL(15,2)  DEFAULT 0,
    last_user       VARCHAR(8),
    last_trans_date DATE
);

CREATE TABLE position (
    portfolio_id    VARCHAR(8)     NOT NULL,
    position_date   VARCHAR(8)     NOT NULL,
    investment_id   VARCHAR(10)    NOT NULL,
    quantity        DECIMAL(15,4)  DEFAULT 0,
    cost_basis      DECIMAL(15,2)  DEFAULT 0,
    market_value    DECIMAL(15,2)  DEFAULT 0,
    currency        VARCHAR(3),
    status          VARCHAR(10),
    last_maint_date TIMESTAMP,
    last_maint_user VARCHAR(8),
    PRIMARY KEY (portfolio_id, position_date, investment_id)
);

CREATE TABLE history_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    VARCHAR(8)     NOT NULL,
    hist_date       VARCHAR(8),
    hist_time       VARCHAR(6),
    seq_no          VARCHAR(4),
    record_type     VARCHAR(12),
    action_code     VARCHAR(8),
    before_image    VARCHAR(400),
    after_image     VARCHAR(400),
    reason_code     VARCHAR(4),
    process_date    TIMESTAMP,
    process_user    VARCHAR(8)
);

CREATE TABLE audit_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp       TIMESTAMP,
    system_id       VARCHAR(8),
    user_id         VARCHAR(8),
    program         VARCHAR(8),
    terminal        VARCHAR(8),
    audit_type      VARCHAR(12),
    action          VARCHAR(10),
    status          VARCHAR(4),
    portfolio_id    VARCHAR(8),
    account_no      VARCHAR(10),
    before_image    VARCHAR(100),
    after_image     VARCHAR(100),
    message         VARCHAR(100)
);

-- Position History (from DB2 DBTBLS.cpy / POSHIST.sql)
CREATE TABLE position_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no      VARCHAR(8),
    portfolio_id    VARCHAR(10),
    trans_date      VARCHAR(10),
    trans_time      VARCHAR(8),
    trans_type      VARCHAR(2),
    security_id     VARCHAR(12),
    quantity        DECIMAL(15,3),
    price           DECIMAL(15,3),
    amount          DECIMAL(15,2),
    fees            DECIMAL(15,2),
    total_amount    DECIMAL(15,2),
    cost_basis      DECIMAL(15,2),
    gain_loss       DECIMAL(15,2),
    process_date    VARCHAR(10),
    process_time    VARCHAR(8),
    program_id      VARCHAR(8),
    user_id         VARCHAR(8),
    audit_timestamp VARCHAR(26)
);

-- Error Log (from DB2 DBTBLS.cpy / ERRLOG.sql)
CREATE TABLE error_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_timestamp VARCHAR(26),
    program_id      VARCHAR(8),
    error_type      VARCHAR(1),
    error_severity  INT,
    error_code      VARCHAR(8),
    error_message   VARCHAR(200),
    process_date    VARCHAR(10),
    process_time    VARCHAR(8),
    user_id         VARCHAR(8),
    additional_info VARCHAR(500)
);
