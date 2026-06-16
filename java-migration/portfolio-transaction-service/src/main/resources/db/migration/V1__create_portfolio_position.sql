-- Portfolio position / master table updated by PORTTRAN.
-- Reconstructed from the missing PORTREC copybook based on PORTTRAN field usage.
--   PORT-ID          PIC X(8)            -> portfolio_id (PK / VSAM RECORD KEY)
--   PORT-ACCOUNT-NO  PIC X(10)           -> account_no
--   PORT-TOTAL-UNITS PIC S9(11)V9(4) COMP-3 -> total_units DECIMAL(15,4)
--   PORT-TOTAL-COST  PIC S9(13)V9(2) COMP-3 -> total_cost  DECIMAL(15,2)
CREATE TABLE portfolio_position (
    portfolio_id VARCHAR(8)     NOT NULL,
    account_no   VARCHAR(10),
    total_units  DECIMAL(15, 4) NOT NULL DEFAULT 0,
    total_cost   DECIMAL(15, 2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_portfolio_position PRIMARY KEY (portfolio_id)
);
