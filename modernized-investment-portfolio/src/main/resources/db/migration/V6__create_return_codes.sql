--====================================================================
-- RETURN CODE LOGGING TABLE (PostgreSQL)
-- Migrated from: src/database/db2/RTNCODES.sql
-- COBOL Copybook: RTNCODE.cpy / COMMON.cpy (RETURN-CODES)
--====================================================================

CREATE TABLE return_codes (
    log_timestamp     TIMESTAMP       NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    return_code       INTEGER         NOT NULL,
    highest_code      INTEGER         NOT NULL,
    status_code       CHAR(1)         NOT NULL,
    message_text      VARCHAR(80),
    PRIMARY KEY (log_timestamp, program_id)
);

-- Return codes: 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Critical

COMMENT ON TABLE return_codes IS 'Return Code Log - Program execution return codes (migrated from DB2 RTNCODES)';

-- Index for program analysis (from DB2: RTNCODES_PRG_IDX)
CREATE INDEX idx_rtncodes_program
    ON return_codes (program_id, log_timestamp);

-- Index for status analysis (from DB2: RTNCODES_STS_IDX)
CREATE INDEX idx_rtncodes_status
    ON return_codes (status_code, log_timestamp);
