-- ============================================================================
-- V1: Portfolio Master Table
-- Migrated from: DB2 PORTFOLIO_MASTER (db2-definitions.sql lines 10-24)
-- Also replaces: VSAM PORTMSTR KSDS file (400-byte fixed records,
--                key = Portfolio ID 8 bytes + Account Type 2 bytes + Branch ID 2 bytes)
-- Copybook: PORTFLIO.cpy
-- ============================================================================

CREATE TABLE portfolio_master (
    portfolio_id      CHAR(8)         NOT NULL,
    account_type      CHAR(2)         NOT NULL,
    branch_id         CHAR(2)         NOT NULL,
    client_id         CHAR(10)        NOT NULL,
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    risk_level        CHAR(1)         NOT NULL,
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    open_date         DATE            NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_portfolio_master PRIMARY KEY (portfolio_id),
    CONSTRAINT chk_portfolio_status CHECK (status IN ('A', 'C', 'S')),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('1', '2', '3', '4', '5'))
);

-- Index matching DB2 IDX_PORT_MASTER_CLIENT (db2-definitions.sql line 67-68)
CREATE INDEX idx_port_master_client ON portfolio_master (client_id, status);

-- Index for VSAM alternate key access patterns
CREATE INDEX idx_port_master_branch ON portfolio_master (branch_id, account_type);

-- View: Active Portfolios (replaces DB2 view from db2-definitions.sql lines 82-86)
CREATE VIEW active_portfolios AS
    SELECT *
    FROM portfolio_master
    WHERE status = 'A'
    AND (close_date IS NULL OR close_date > CURRENT_DATE);
