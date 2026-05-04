-- Flyway Migration V2: Investment Positions Table
-- Source: src/database/db2/db2-definitions.sql (INVESTMENT_POSITIONS)
-- COBOL Copybook: POSREC.cpy

CREATE TABLE investment_positions (
    portfolio_id      VARCHAR(8)         NOT NULL,
    investment_id     VARCHAR(10)        NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          DECIMAL(15,4)   NOT NULL DEFAULT 0,
    cost_basis        DECIMAL(15,2)   NOT NULL DEFAULT 0,
    market_value      DECIMAL(15,2)   NOT NULL DEFAULT 0,
    currency_code     VARCHAR(3)         NOT NULL DEFAULT 'USD',
    status            VARCHAR(1)         NOT NULL DEFAULT 'A',
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    filler            VARCHAR(50),
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE investment_positions IS 'Investment Positions - migrated from COBOL POSREC.cpy + DB2 INVESTMENT_POSITIONS';
COMMENT ON COLUMN investment_positions.status IS 'A=Active, C=Closed, P=Pending (level-88 POS-STATUS-ACTIVE/CLOSED/PEND)';
