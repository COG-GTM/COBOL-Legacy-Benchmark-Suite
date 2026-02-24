--====================================================================
-- INVESTMENT POSITIONS TABLE (PostgreSQL)
-- Migrated from: src/database/db2/db2-definitions.sql
-- COBOL Copybook: POSREC.cpy (POSITION-RECORD)
--====================================================================

CREATE TABLE investment_positions (
    portfolio_id      CHAR(8)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          NUMERIC(18,4)   NOT NULL,
    cost_basis        NUMERIC(18,2)   NOT NULL,
    market_value      NUMERIC(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    last_maint_date   TIMESTAMP       NOT NULL,
    last_maint_user   VARCHAR(8)      NOT NULL,
    PRIMARY KEY (portfolio_id, investment_id, position_date),
    FOREIGN KEY (portfolio_id) REFERENCES portfolio_master(portfolio_id)
);

-- COBOL POS-KEY maps to composite PK: POS-PORTFOLIO-ID + POS-INVESTMENT-ID + POS-DATE
-- Matches VSAM Position Master key structure (ACCOUNT-NO + FUND-ID)
-- Status values: A=Active, C=Closed, P=Pending

COMMENT ON TABLE investment_positions IS 'Investment Positions - Portfolio holdings (migrated from DB2/VSAM POSMSTRE)';
COMMENT ON COLUMN investment_positions.portfolio_id IS 'Portfolio Identifier (COBOL: POS-PORTFOLIO-ID PIC X(08))';
COMMENT ON COLUMN investment_positions.investment_id IS 'Investment/Fund Identifier (COBOL: POS-INVESTMENT-ID PIC X(10))';
COMMENT ON COLUMN investment_positions.quantity IS 'Holding Quantity (COBOL: POS-QUANTITY PIC S9(11)V9(4))';
COMMENT ON COLUMN investment_positions.cost_basis IS 'Total Cost Basis (COBOL: POS-COST-BASIS PIC S9(13)V9(2))';
COMMENT ON COLUMN investment_positions.market_value IS 'Current Market Value (COBOL: POS-MARKET-VALUE PIC S9(13)V9(2))';

-- Index for date-based lookups (from DB2: IDX_POSITIONS_DATE)
CREATE INDEX idx_positions_date
    ON investment_positions (position_date, portfolio_id);
