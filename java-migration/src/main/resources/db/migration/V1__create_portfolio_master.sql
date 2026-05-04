-- Flyway Migration V1: Portfolio Master Table
-- Source: src/database/db2/db2-definitions.sql (PORTFOLIO_MASTER)
-- COBOL Copybook: PORTFLIO.cpy

CREATE TABLE portfolio_master (
    portfolio_id      VARCHAR(8)         NOT NULL,
    account_type      VARCHAR(2)         NOT NULL,
    branch_id         VARCHAR(2)         NOT NULL,
    client_id         VARCHAR(10)        NOT NULL,
    client_name       VARCHAR(30)     NOT NULL,
    client_type       VARCHAR(1)         NOT NULL,
    portfolio_name    VARCHAR(50),
    currency_code     VARCHAR(3)         NOT NULL DEFAULT 'USD',
    risk_level        VARCHAR(1),
    status            VARCHAR(1)         NOT NULL DEFAULT 'A',
    open_date         DATE            NOT NULL,
    close_date        DATE,
    total_value       DECIMAL(15,2)   NOT NULL DEFAULT 0,
    cash_balance      DECIMAL(15,2)   NOT NULL DEFAULT 0,
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    last_trans_date   DATE,
    filler            VARCHAR(50),
    PRIMARY KEY (portfolio_id)
);

COMMENT ON TABLE portfolio_master IS 'Portfolio Master - migrated from COBOL PORTFLIO.cpy + DB2 PORTFOLIO_MASTER';
COMMENT ON COLUMN portfolio_master.client_type IS 'I=Individual, C=Corporate, T=Trust (level-88 PORT-INDIVIDUAL/CORPORATE/TRUST)';
COMMENT ON COLUMN portfolio_master.status IS 'A=Active, C=Closed, S=Suspended (level-88 PORT-ACTIVE/CLOSED/SUSPENDED)';
