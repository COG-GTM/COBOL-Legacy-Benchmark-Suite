-- Flyway migration V4: Audit log table
-- Mapped from COBOL copybook AUDITLOG.cpy (AUDIT-RECORD)

CREATE TABLE audit_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,

    -- AUD-HEADER
    timestamp       TIMESTAMP       NOT NULL,   -- AUD-TIMESTAMP PIC X(26)
    system_id       VARCHAR(8),                 -- AUD-SYSTEM-ID PIC X(8)
    user_id         VARCHAR(8),                 -- AUD-USER-ID PIC X(8)
    program         VARCHAR(8),                 -- AUD-PROGRAM PIC X(8)

    -- AUD-TYPE, AUD-ACTION, AUD-STATUS
    audit_type      VARCHAR(4)      NOT NULL,   -- AUD-TYPE PIC X(4) [TRAN/USER/SYST]
    action          VARCHAR(8)      NOT NULL,   -- AUD-ACTION PIC X(8) [CREATE/UPDATE/DELETE/...]
    status          VARCHAR(4)      NOT NULL,   -- AUD-STATUS PIC X(4) [SUCC/FAIL/WARN]

    -- AUD-KEY-INFO
    portfolio_id    VARCHAR(8),                 -- AUD-PORTFOLIO-ID PIC X(8)
    account_no      VARCHAR(10),                -- AUD-ACCOUNT-NO PIC X(10)

    -- AUD-IMAGES
    before_image    VARCHAR(500),               -- AUD-BEFORE-IMAGE PIC X(100) [expanded for JSON]
    after_image     VARCHAR(500),               -- AUD-AFTER-IMAGE PIC X(100) [expanded for JSON]
    message         VARCHAR(255)                -- AUD-MESSAGE PIC X(100)
);

CREATE INDEX idx_audit_portfolio ON audit_log(portfolio_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
