-- Flyway migration V3: Transaction table
-- Mapped from COBOL copybook TRNREC.cpy (TRANSACTION-RECORD)
-- VSAM KSDS file TRANHIST with KEY LENGTH 20

CREATE TABLE transaction (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,

    -- TRN-KEY (composite: TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO)
    transaction_date    DATE            NOT NULL,   -- TRN-DATE PIC X(08) [YYYYMMDD]
    transaction_time    VARCHAR(6),                 -- TRN-TIME PIC X(06) [HHMMSS]
    portfolio_id        VARCHAR(8)      NOT NULL,   -- TRN-PORTFOLIO-ID PIC X(08)
    sequence_no         VARCHAR(6),                 -- TRN-SEQUENCE-NO PIC X(06)

    -- TRN-DATA
    investment_id       VARCHAR(10)     NOT NULL,   -- TRN-INVESTMENT-ID PIC X(10)
    transaction_type    VARCHAR(2)      NOT NULL,   -- TRN-TYPE PIC X(02) [BU/SL/TR/FE]
    quantity            DECIMAL(15,4)   NOT NULL,   -- TRN-QUANTITY PIC S9(11)V9(4) COMP-3
    price               DECIMAL(15,4)   NOT NULL,   -- TRN-PRICE PIC S9(11)V9(4) COMP-3
    amount              DECIMAL(15,2)   NOT NULL,   -- TRN-AMOUNT PIC S9(13)V9(2) COMP-3
    currency            VARCHAR(3)      NOT NULL,   -- TRN-CURRENCY PIC X(03)
    status              VARCHAR(1)      NOT NULL,   -- TRN-STATUS PIC X(01) [P/D/F/R]

    -- TRN-AUDIT
    process_date        TIMESTAMP,                  -- TRN-PROCESS-DATE PIC X(26)
    process_user        VARCHAR(8),                 -- TRN-PROCESS-USER PIC X(08)

    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(port_id)
);

CREATE INDEX idx_transaction_portfolio ON transaction(portfolio_id);
CREATE INDEX idx_transaction_type ON transaction(portfolio_id, transaction_type);
CREATE INDEX idx_transaction_date ON transaction(transaction_date);
