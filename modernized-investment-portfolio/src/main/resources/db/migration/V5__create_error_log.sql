--====================================================================
-- ERROR LOG TABLE (PostgreSQL)
-- Migrated from: src/database/db2/ERRLOG.sql
-- COBOL Copybook: DBTBLS.cpy (ERRLOG-RECORD)
--====================================================================

CREATE TABLE error_log (
    error_timestamp   TIMESTAMP       NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    error_type        CHAR(1)         NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        CHAR(8)         NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    additional_info   VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);

-- Error types: S=System, A=Application, D=Data
-- Severity levels: 1=Info, 2=Warning, 3=Error, 4=Severe

COMMENT ON TABLE error_log IS 'Error Log - Application errors and warnings (migrated from DB2 ERRLOG)';
COMMENT ON COLUMN error_log.error_type IS 'Error Type: S=System, A=Application, D=Data';
COMMENT ON COLUMN error_log.error_severity IS 'Severity: 1=Info, 2=Warning, 3=Error, 4=Severe';

-- Secondary index: process date + severity (from DB2: ERRLOG_IX1)
CREATE INDEX idx_errlog_process_severity
    ON error_log (process_date, error_severity DESC);
