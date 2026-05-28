-- =====================================================================
-- V1: Portfolio, Position, and TransactionRecord tables
-- Migrated from COBOL VSAM copybooks: PORTFLIO.cpy, POSREC.cpy, TRNREC.cpy
-- and DB2 definitions: db2-definitions.sql
-- =====================================================================

CREATE TABLE portfolio (
    portfolio_id    VARCHAR(8)      NOT NULL,
    account_no      VARCHAR(10)     NOT NULL,
    client_name     VARCHAR(30),
    client_type     VARCHAR(12)     NOT NULL,
    create_date     DATE,
    last_maint_date DATE,
    status          VARCHAR(12)     NOT NULL,
    total_value     DECIMAL(15,2),
    cash_balance    DECIMAL(15,2),
    last_user       VARCHAR(8),
    last_trans_date DATE,
    filler          VARCHAR(50),
    PRIMARY KEY (portfolio_id)
);

CREATE TABLE position (
    portfolio_id    VARCHAR(8)      NOT NULL,
    pos_date        VARCHAR(8)      NOT NULL,
    investment_id   VARCHAR(10)     NOT NULL,
    quantity        DECIMAL(15,4),
    cost_basis      DECIMAL(15,2),
    market_value    DECIMAL(15,2),
    currency        VARCHAR(3),
    status          VARCHAR(10)     NOT NULL,
    last_maint_date TIMESTAMP,
    last_maint_user VARCHAR(8),
    PRIMARY KEY (portfolio_id, pos_date, investment_id)
);

CREATE TABLE transaction_record (
    trn_date        VARCHAR(8)      NOT NULL,
    trn_time        VARCHAR(6)      NOT NULL,
    portfolio_id    VARCHAR(8)      NOT NULL,
    sequence_no     VARCHAR(6)      NOT NULL,
    investment_id   VARCHAR(10),
    trn_type        VARCHAR(10)     NOT NULL,
    quantity        DECIMAL(15,4),
    price           DECIMAL(15,4),
    amount          DECIMAL(15,2),
    currency        VARCHAR(3),
    status          VARCHAR(10)     NOT NULL,
    process_date    TIMESTAMP,
    process_user    VARCHAR(8),
    PRIMARY KEY (trn_date, trn_time, portfolio_id, sequence_no)
);
