-- Flyway migration V2: Position table
-- Mapped from COBOL copybook POSREC.cpy (POSITION-RECORD)
-- VSAM KSDS file POSHIST with KEY LENGTH 18

CREATE TABLE position (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,

    -- POS-KEY (composite: POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID)
    portfolio_id    VARCHAR(8)      NOT NULL,   -- POS-PORTFOLIO-ID PIC X(08)
    position_date   DATE            NOT NULL,   -- POS-DATE PIC X(08) [YYYYMMDD]
    investment_id   VARCHAR(10)     NOT NULL,   -- POS-INVESTMENT-ID PIC X(10)

    -- POS-DATA
    quantity        DECIMAL(15,4)   NOT NULL,   -- POS-QUANTITY PIC S9(11)V9(4) COMP-3
    cost_basis      DECIMAL(15,2)   NOT NULL,   -- POS-COST-BASIS PIC S9(13)V9(2) COMP-3
    market_value    DECIMAL(15,2)   NOT NULL,   -- POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3
    currency        VARCHAR(3)      NOT NULL,   -- POS-CURRENCY PIC X(03)
    status          VARCHAR(1)      NOT NULL,   -- POS-STATUS PIC X(01) [A/C/P]

    -- POS-AUDIT
    last_maint_date TIMESTAMP,                  -- POS-LAST-MAINT-DATE PIC X(26)
    last_maint_user VARCHAR(8),                 -- POS-LAST-MAINT-USER PIC X(08)

    CONSTRAINT fk_position_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(port_id)
);

CREATE INDEX idx_position_portfolio ON position(portfolio_id);
CREATE INDEX idx_position_investment ON position(portfolio_id, investment_id);
