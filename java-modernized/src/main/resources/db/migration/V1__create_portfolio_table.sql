-- Flyway migration V1: Portfolio master table
-- Mapped from COBOL copybook PORTFLIO.cpy (PORT-RECORD)
-- VSAM KSDS file PORTMSTR with KEY LENGTH 12, KEY POSITION 1

CREATE TABLE portfolio (
    -- PORT-KEY (composite: PORT-ID + PORT-ACCOUNT-NO)
    port_id         VARCHAR(8)      NOT NULL,   -- PORT-ID PIC X(8)
    account_no      VARCHAR(10)     NOT NULL,   -- PORT-ACCOUNT-NO PIC X(10)

    -- PORT-CLIENT-INFO
    client_name     VARCHAR(30)     NOT NULL,   -- PORT-CLIENT-NAME PIC X(30)
    client_type     VARCHAR(1)      NOT NULL,   -- PORT-CLIENT-TYPE PIC X(1) [I/C/T]

    -- PORT-PORTFOLIO-INFO
    create_date     DATE            NOT NULL,   -- PORT-CREATE-DATE PIC 9(8)
    last_maint_date DATE,                       -- PORT-LAST-MAINT PIC 9(8)
    status          VARCHAR(1)      NOT NULL,   -- PORT-STATUS PIC X(1) [A/C/S]

    -- PORT-FINANCIAL-INFO
    total_value     DECIMAL(15,2)   NOT NULL DEFAULT 0, -- PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3
    cash_balance    DECIMAL(15,2)   NOT NULL DEFAULT 0, -- PORT-CASH-BALANCE PIC S9(13)V99 COMP-3

    -- PORT-AUDIT-INFO
    last_user       VARCHAR(8),                 -- PORT-LAST-USER PIC X(8)
    last_trans_date DATE,                       -- PORT-LAST-TRANS PIC 9(8)

    PRIMARY KEY (port_id)
);

CREATE INDEX idx_portfolio_status ON portfolio(status);
CREATE INDEX idx_portfolio_account ON portfolio(account_no);
CREATE INDEX idx_portfolio_client_type ON portfolio(client_type);
