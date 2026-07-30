-- Table for the COBOL portfolio transaction entity.
--
-- Source of truth: src/copybook/common/TRNREC.cpy (01 TRANSACTION-RECORD), stored on the VSAM KSDS
-- TRANHIST (src/database/vsam/vsam-definitions.txt). Column types reproduce the COBOL precision and
-- scale exactly; see java-migration/MIGRATION-NOTES.md for the field mapping table with byte offsets.
--
--   COBOL PIC                      bytes  SQL
--   PIC X(n)                       n      VARCHAR(n)
--   PIC S9(11)V9(4) COMP-3         8      DECIMAL(15,4)
--   PIC S9(13)V9(2) COMP-3         8      DECIMAL(15,2)

CREATE TABLE portfolio_transaction (
    -- 05 TRN-KEY: the 28 byte VSAM key, bytes 1-28
    trn_date          VARCHAR(8)     NOT NULL, -- TRN-DATE          PIC X(08)  bytes 1-8    YYYYMMDD
    trn_time          VARCHAR(6)     NOT NULL, -- TRN-TIME          PIC X(06)  bytes 9-14   HHMMSS
    trn_portfolio_id  VARCHAR(8)     NOT NULL, -- TRN-PORTFOLIO-ID  PIC X(08)  bytes 15-22
    trn_sequence_no   VARCHAR(6)     NOT NULL, -- TRN-SEQUENCE-NO   PIC X(06)  bytes 23-28

    -- 05 TRN-DATA
    trn_investment_id VARCHAR(10)    NOT NULL, -- TRN-INVESTMENT-ID PIC X(10)  bytes 29-38
    trn_type          VARCHAR(2)     NOT NULL, -- TRN-TYPE          PIC X(02)  bytes 39-40  BU/SL/TR/FE
    trn_quantity      DECIMAL(15, 4) NOT NULL, -- TRN-QUANTITY      PIC S9(11)V9(4) COMP-3  bytes 41-48
    trn_price         DECIMAL(15, 4) NOT NULL, -- TRN-PRICE         PIC S9(11)V9(4) COMP-3  bytes 49-56
    trn_amount        DECIMAL(15, 2) NOT NULL, -- TRN-AMOUNT        PIC S9(13)V9(2) COMP-3  bytes 57-64
    trn_currency      VARCHAR(3)     NOT NULL, -- TRN-CURRENCY      PIC X(03)  bytes 65-67
    trn_status        VARCHAR(1)     NOT NULL, -- TRN-STATUS        PIC X(01)  byte  68     P/D/F/R

    -- 05 TRN-AUDIT
    trn_process_date  VARCHAR(26),             -- TRN-PROCESS-DATE  PIC X(26)  bytes 69-94
    trn_process_user  VARCHAR(8),              -- TRN-PROCESS-USER  PIC X(08)  bytes 95-102

    -- 05 TRN-FILLER PIC X(50), bytes 103-152, is record padding and is not migrated.

    CONSTRAINT pk_portfolio_transaction
        PRIMARY KEY (trn_date, trn_time, trn_portfolio_id, trn_sequence_no),
    CONSTRAINT ck_trn_type   CHECK (trn_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT ck_trn_status CHECK (trn_status IN ('P', 'D', 'F', 'R'))
);

-- Access paths used by the COBOL programs and by the browse endpoint.
CREATE INDEX idx_trn_portfolio_id ON portfolio_transaction (trn_portfolio_id);
CREATE INDEX idx_trn_status ON portfolio_transaction (trn_status);
CREATE INDEX idx_trn_investment_id ON portfolio_transaction (trn_investment_id);
