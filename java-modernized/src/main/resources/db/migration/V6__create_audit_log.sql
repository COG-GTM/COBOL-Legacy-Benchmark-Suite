--====================================================================
-- AUDIT LOG TABLE
-- New table for audit trail (from AUDITLOG.cpy)
--====================================================================
CREATE TABLE audit_log (
    id                BIGSERIAL        PRIMARY KEY,
    timestamp         TIMESTAMP        NOT NULL,
    system_id         VARCHAR(8),
    user_id           VARCHAR(8),
    program           VARCHAR(8),
    terminal          VARCHAR(8),
    audit_type        VARCHAR(4)       NOT NULL,
    action            VARCHAR(8)       NOT NULL,
    status            VARCHAR(4)       NOT NULL,
    portfolio_id      VARCHAR(8),
    account_no        VARCHAR(10),
    before_image      VARCHAR(100),
    after_image       VARCHAR(100),
    message           VARCHAR(100)
);
