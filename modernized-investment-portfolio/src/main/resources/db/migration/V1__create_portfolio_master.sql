--====================================================================
-- PORTFOLIO MASTER TABLE (PostgreSQL)
-- Migrated from: src/database/db2/db2-definitions.sql
-- COBOL Copybook: PORTFLIO.cpy (PORT-RECORD)
--====================================================================

CREATE TABLE portfolio_master (
    portfolio_id      CHAR(8)         NOT NULL,
    account_type      CHAR(2)         NOT NULL,
    branch_id         CHAR(2)         NOT NULL,
    client_id         CHAR(10)        NOT NULL,
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    risk_level        CHAR(1)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    open_date         DATE            NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id)
);

-- Status codes: 'A'=Active, 'C'=Closed, 'S'=Suspended
-- Matches COBOL: PORT-STATUS with 88-level conditions

COMMENT ON TABLE portfolio_master IS 'Portfolio Master - Core portfolio information (migrated from DB2/COBOL)';
COMMENT ON COLUMN portfolio_master.portfolio_id IS 'Portfolio Identifier (COBOL: PORT-ID PIC X(8))';
COMMENT ON COLUMN portfolio_master.client_id IS 'Client Identifier (COBOL: PORT-ACCOUNT-NO PIC X(10))';
COMMENT ON COLUMN portfolio_master.status IS 'Portfolio Status: A=Active, C=Closed, S=Suspended';

-- Index for client lookups (from DB2: IDX_PORT_MASTER_CLIENT)
CREATE INDEX idx_portfolio_master_client
    ON portfolio_master (client_id, status);
