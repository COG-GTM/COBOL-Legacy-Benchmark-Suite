--====================================================================
-- HISTORY RECORD TABLE
-- New table for VSAM history record (from HISTREC.cpy)
-- No existing DB2 table; created to consolidate VSAM data
--====================================================================
CREATE TABLE history_record (
    portfolio_id      VARCHAR(8)       NOT NULL,
    hist_date         VARCHAR(8)       NOT NULL,
    hist_time         VARCHAR(6)       NOT NULL,
    seq_no            VARCHAR(4)       NOT NULL,
    record_type       VARCHAR(2)       NOT NULL,
    action_code       VARCHAR(1)       NOT NULL,
    before_image      TEXT,
    after_image       TEXT,
    reason_code       VARCHAR(4),
    process_date      VARCHAR(26),
    process_user      VARCHAR(8),
    PRIMARY KEY (portfolio_id, hist_date, hist_time, seq_no)
);
