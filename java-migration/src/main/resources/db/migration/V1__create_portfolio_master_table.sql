-- ============================================================
-- Flyway Migration: V1 - Create Portfolio Master Table
-- Source: COBOL copybook PORTFLIO.cpy
-- VSAM: PORTMSTR (KSDS, key=PORT-ID + PORT-ACCOUNT-NO)
-- ============================================================

CREATE TABLE portfolio_master (
    -- PORT-ID PIC X(8) - Primary key
    port_id             VARCHAR(8)      NOT NULL,
    -- PORT-ACCOUNT-NO PIC X(10)
    port_account_no     VARCHAR(10)     NOT NULL,
    -- PORT-CLIENT-NAME PIC X(30)
    port_client_name    VARCHAR(30),
    -- PORT-CLIENT-TYPE PIC X(1) - I=Individual, C=Corporate, T=Trust
    port_client_type    VARCHAR(1),
    -- PORT-CREATE-DATE PIC 9(8) - YYYYMMDD
    port_create_date    DATE,
    -- PORT-LAST-MAINT PIC 9(8) - YYYYMMDD
    port_last_maint     DATE,
    -- PORT-STATUS PIC X(1) - A=Active, C=Closed, S=Suspended
    port_status         VARCHAR(1),
    -- PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3
    port_total_value    DECIMAL(15, 2),
    -- PORT-CASH-BALANCE PIC S9(13)V99 COMP-3
    port_cash_balance   DECIMAL(15, 2),
    -- PORT-LAST-USER PIC X(8)
    port_last_user      VARCHAR(8),
    -- PORT-LAST-TRANS PIC 9(8) - YYYYMMDD
    port_last_trans     DATE,

    CONSTRAINT pk_portfolio_master PRIMARY KEY (port_id)
);

-- Index on status for findByPortStatus queries
CREATE INDEX idx_portfolio_status ON portfolio_master (port_status);

-- Index on client type for findByPortClientType queries
CREATE INDEX idx_portfolio_client_type ON portfolio_master (port_client_type);

-- Index on account number for findByPortAccountNo queries
CREATE INDEX idx_portfolio_account_no ON portfolio_master (port_account_no);
