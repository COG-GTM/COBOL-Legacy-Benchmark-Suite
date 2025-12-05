-- ============================================================================
-- V6: Create Audit Log Schema
-- Description: Audit trail for compliance and security
-- Replaces: COBOL AUDITLOG copybook structure
-- Source: COBOL Copybook AUDITLOG.cpy
-- ============================================================================

-- Audit Log table
CREATE TABLE audit_log (
    audit_id            BIGSERIAL PRIMARY KEY,
    timestamp           TIMESTAMP NOT NULL,
    system_id           VARCHAR(8) NOT NULL,
    user_id             VARCHAR(8) NOT NULL,
    program_id          VARCHAR(8) NOT NULL,
    terminal_id         VARCHAR(8),
    audit_type          VARCHAR(4) NOT NULL CHECK (audit_type IN ('TRAN', 'USER', 'SYST')),
    action_type         VARCHAR(8) NOT NULL CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'INQUIRE', 'LOGIN', 'LOGOUT', 'STARTUP', 'SHUTDOWN')),
    status              VARCHAR(4) NOT NULL CHECK (status IN ('SUCC', 'FAIL', 'WARN')),
    portfolio_id        VARCHAR(8),
    account_number      VARCHAR(10),
    before_image        VARCHAR(100),
    after_image         VARCHAR(100),
    message             VARCHAR(100)
);

-- Indexes for audit_log table
CREATE INDEX idx_audit_portfolio ON audit_log (portfolio_id);
CREATE INDEX idx_audit_user ON audit_log (user_id, timestamp);
CREATE INDEX idx_audit_action ON audit_log (action_type, timestamp);
CREATE INDEX idx_audit_timestamp ON audit_log (timestamp);

-- Comments for documentation
COMMENT ON TABLE audit_log IS 'Audit Trail - Replaces COBOL AUDITLOG structure';
COMMENT ON COLUMN audit_log.audit_type IS 'TRAN=Transaction, USER=User Action, SYST=System Event';
COMMENT ON COLUMN audit_log.action_type IS 'CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN';
COMMENT ON COLUMN audit_log.status IS 'SUCC=Success, FAIL=Failure, WARN=Warning';
