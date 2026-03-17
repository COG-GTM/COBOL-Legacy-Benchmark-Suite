-- ============================================================================
-- V6: Audit Log Table
-- Migrated from: AUDITLOG.cpy copybook and AUDPROC.cbl, RPTAUD00.cbl
-- Audit types: TRAN=Transaction, USER=User Action, SYST=System Event
-- Actions: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN
-- Statuses: SUCC=Success, FAIL=Failure, WARN=Warning
-- ============================================================================

CREATE TABLE audit_log (
    id                BIGSERIAL       PRIMARY KEY,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    system_id         VARCHAR(8),
    user_id           VARCHAR(8)      NOT NULL,
    program_name      VARCHAR(8),
    terminal_id       VARCHAR(8),
    audit_type        VARCHAR(4)      NOT NULL,
    action            VARCHAR(8)      NOT NULL,
    status            VARCHAR(4)      NOT NULL DEFAULT 'SUCC',
    portfolio_id      CHAR(8),
    account_no        CHAR(10),
    before_image      TEXT,
    after_image       TEXT,
    message           VARCHAR(256),
    CONSTRAINT chk_audit_type CHECK (audit_type IN ('TRAN', 'USER', 'SYST')),
    CONSTRAINT chk_audit_status CHECK (status IN ('SUCC', 'FAIL', 'WARN'))
);

-- Index for user-based audit queries
CREATE INDEX idx_audit_user ON audit_log (user_id, audit_timestamp);

-- Index for portfolio-based audit queries
CREATE INDEX idx_audit_portfolio ON audit_log (portfolio_id, audit_timestamp);

-- Index for timestamp-based queries (report generation)
CREATE INDEX idx_audit_timestamp ON audit_log (audit_timestamp);

-- Index for audit type filtering
CREATE INDEX idx_audit_type ON audit_log (audit_type, action);
