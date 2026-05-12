-- Position History Table (from POSHIST.sql - DB2 to standard SQL)
CREATE TABLE poshist (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no     VARCHAR(8) NOT NULL,
    portfolio_id   VARCHAR(10) NOT NULL,
    trans_date     DATE NOT NULL,
    trans_time     TIME NOT NULL,
    trans_type     VARCHAR(2) NOT NULL,
    security_id    VARCHAR(12) NOT NULL,
    quantity       DECIMAL(15,3) NOT NULL DEFAULT 0,
    price          DECIMAL(15,3) NOT NULL DEFAULT 0,
    amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    fees           DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
    cost_basis     DECIMAL(15,2) NOT NULL DEFAULT 0,
    gain_loss      DECIMAL(15,2) NOT NULL DEFAULT 0,
    process_date   DATE NOT NULL,
    process_time   TIME NOT NULL,
    program_id     VARCHAR(8) NOT NULL,
    user_id        VARCHAR(8) NOT NULL,
    audit_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_poshist_acct ON poshist(account_no, portfolio_id, trans_date);
CREATE INDEX idx_poshist_security ON poshist(security_id, trans_date);
