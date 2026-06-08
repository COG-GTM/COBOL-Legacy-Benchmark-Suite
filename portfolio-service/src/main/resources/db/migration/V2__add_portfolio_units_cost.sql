-- V2: Add total_units and total_cost columns to portfolio table
-- Required by Portfolio aggregate root applyBuy/applySell/applyFee methods

ALTER TABLE portfolio ADD COLUMN total_units DECIMAL(15,4) DEFAULT 0;
ALTER TABLE portfolio ADD COLUMN total_cost  DECIMAL(15,2) DEFAULT 0;
