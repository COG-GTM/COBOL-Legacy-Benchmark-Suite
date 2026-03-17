-- ============================================================================
-- V2: Investment Positions Table
-- Migrated from: DB2 INVESTMENT_POSITIONS (db2-definitions.sql lines 29-41)
-- Copybook: POSREC.cpy (POS-KEY: Portfolio ID 8 + Date 8 + Investment ID 10)
-- COBOL types: PIC S9(11)V9(4) COMP-3 -> NUMERIC(15,4)
--              PIC S9(13)V9(2) COMP-3 -> NUMERIC(15,2)
-- ============================================================================

CREATE TABLE investment_positions (
    portfolio_id      CHAR(8)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          NUMERIC(18,4)   NOT NULL DEFAULT 0,
    cost_basis        NUMERIC(18,2)   NOT NULL DEFAULT 0,
    market_value      NUMERIC(18,2)   NOT NULL DEFAULT 0,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL DEFAULT 'A',
    investment_name   VARCHAR(50),
    last_activity_date CHAR(8),
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_investment_positions PRIMARY KEY (portfolio_id, investment_id, position_date),
    CONSTRAINT fk_positions_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id),
    CONSTRAINT chk_position_status CHECK (status IN ('A', 'C', 'P'))
);

-- Index matching DB2 IDX_POSITIONS_DATE (db2-definitions.sql line 70-71)
CREATE INDEX idx_positions_date ON investment_positions (position_date, portfolio_id);

-- View: Current Positions (replaces DB2 view from db2-definitions.sql lines 88-92)
CREATE VIEW current_positions AS
    SELECT p.*, pm.portfolio_name, pm.client_id
    FROM investment_positions p
    JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day';
