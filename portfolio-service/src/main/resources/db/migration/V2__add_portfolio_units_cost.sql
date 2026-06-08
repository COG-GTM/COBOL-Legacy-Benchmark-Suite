-- V2: Add total_units and total_cost to portfolio table
-- Source: PORTTRAN.cbl PORT-TOTAL-UNITS / PORT-TOTAL-COST used in 2210-2240

ALTER TABLE portfolio ADD COLUMN total_units DECIMAL(15,4) DEFAULT 0;
ALTER TABLE portfolio ADD COLUMN total_cost  DECIMAL(15,2) DEFAULT 0;
