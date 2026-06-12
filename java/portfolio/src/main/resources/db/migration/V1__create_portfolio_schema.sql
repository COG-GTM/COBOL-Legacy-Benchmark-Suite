-- =====================================================================
-- V1: Portfolio domain schema, translated from the COBOL copybooks
--   PORTFLIO.cpy -> portfolio_master
--   TRNREC.cpy   -> transaction_record
--   POSREC.cpy   -> position_record
--   HISTREC.cpy  -> history_record
-- Type mapping rules (see java/docs/field-mappings.md):
--   PIC X(n)            -> CHAR(n) / VARCHAR(n)
--   PIC 9(8) (date)     -> INTEGER (raw YYYYMMDD, preserves byte parity)
--   PIC S9(13)V99 COMP-3 -> DECIMAL(15,2)
--   PIC S9(11)V9(4) COMP-3 -> DECIMAL(15,4)
-- Standard SQL: runs on both H2 (PostgreSQL mode, dev/CI) and PostgreSQL (prod).
-- =====================================================================

-- PORTFLIO.cpy : PORT-RECORD
CREATE TABLE portfolio_master (
    port_id           CHAR(8)        NOT NULL,
    port_account_no   CHAR(10)       NOT NULL,
    port_client_name  VARCHAR(30)    NOT NULL,
    port_client_type  CHAR(1)        NOT NULL,
    port_create_date  INTEGER        NOT NULL,
    port_last_maint   INTEGER        NOT NULL,
    port_status       CHAR(1)        NOT NULL,
    port_total_value  DECIMAL(15, 2) NOT NULL,
    port_cash_balance DECIMAL(15, 2) NOT NULL,
    port_last_user    VARCHAR(8)     NOT NULL,
    port_last_trans   INTEGER        NOT NULL,
    port_filler       VARCHAR(50),
    CONSTRAINT pk_portfolio_master PRIMARY KEY (port_id, port_account_no)
);

CREATE INDEX idx_port_master_id ON portfolio_master (port_id);
CREATE INDEX idx_port_master_client ON portfolio_master (port_client_type, port_status);

-- TRNREC.cpy : TRANSACTION-RECORD
CREATE TABLE transaction_record (
    trn_date          CHAR(8)        NOT NULL,
    trn_time          CHAR(6)        NOT NULL,
    trn_portfolio_id  CHAR(8)        NOT NULL,
    trn_sequence_no   CHAR(6)        NOT NULL,
    trn_investment_id CHAR(10)       NOT NULL,
    trn_type          CHAR(2)        NOT NULL,
    trn_quantity      DECIMAL(15, 4) NOT NULL,
    trn_price         DECIMAL(15, 4) NOT NULL,
    trn_amount        DECIMAL(15, 2) NOT NULL,
    trn_currency      CHAR(3)        NOT NULL,
    trn_status        CHAR(1)        NOT NULL,
    trn_process_date  CHAR(26)       NOT NULL,
    trn_process_user  VARCHAR(8)     NOT NULL,
    trn_filler        VARCHAR(50),
    CONSTRAINT pk_transaction_record PRIMARY KEY (trn_date, trn_time, trn_portfolio_id, trn_sequence_no)
);

CREATE INDEX idx_trans_port ON transaction_record (trn_portfolio_id, trn_date);
CREATE INDEX idx_trans_status ON transaction_record (trn_status);

-- POSREC.cpy : POSITION-RECORD
CREATE TABLE position_record (
    pos_portfolio_id  CHAR(8)        NOT NULL,
    pos_date          CHAR(8)        NOT NULL,
    pos_investment_id CHAR(10)       NOT NULL,
    pos_quantity      DECIMAL(15, 4) NOT NULL,
    pos_cost_basis    DECIMAL(15, 2) NOT NULL,
    pos_market_value  DECIMAL(15, 2) NOT NULL,
    pos_currency      CHAR(3)        NOT NULL,
    pos_status        CHAR(1)        NOT NULL,
    pos_last_maint_date VARCHAR(26)  NOT NULL,
    pos_last_maint_user VARCHAR(8)   NOT NULL,
    pos_filler        VARCHAR(50),
    CONSTRAINT pk_position_record PRIMARY KEY (pos_portfolio_id, pos_date, pos_investment_id)
);

CREATE INDEX idx_pos_date ON position_record (pos_date, pos_portfolio_id);
CREATE INDEX idx_pos_status ON position_record (pos_status);

-- HISTREC.cpy : HISTORY-RECORD
CREATE TABLE history_record (
    hist_portfolio_id CHAR(8)        NOT NULL,
    hist_date         CHAR(8)        NOT NULL,
    hist_time         CHAR(6)        NOT NULL,
    hist_seq_no       CHAR(4)        NOT NULL,
    hist_record_type  CHAR(2)        NOT NULL,
    hist_action_code  CHAR(1)        NOT NULL,
    hist_before_image VARCHAR(400),
    hist_after_image  VARCHAR(400),
    hist_reason_code  VARCHAR(4),
    hist_process_date CHAR(26)       NOT NULL,
    hist_process_user VARCHAR(8)     NOT NULL,
    hist_filler       VARCHAR(50),
    CONSTRAINT pk_history_record PRIMARY KEY (hist_portfolio_id, hist_date, hist_time, hist_seq_no)
);

CREATE INDEX idx_hist_type ON history_record (hist_record_type);
