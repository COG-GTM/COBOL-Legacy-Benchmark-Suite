-- ============================================================================
-- V5: Create Error Log Schema
-- Description: Error logging for application monitoring
-- Replaces: DB2 ERRLOG table
-- Source: DB2 ERRLOG.sql
-- ============================================================================

-- Error Log table (replaces DB2 ERRLOG)
CREATE TABLE error_log (
    error_timestamp     TIMESTAMP NOT NULL,
    program_id          VARCHAR(8) NOT NULL,
    error_type          CHAR(1) NOT NULL CHECK (error_type IN ('S', 'A', 'D')),
    error_severity      INTEGER NOT NULL CHECK (error_severity BETWEEN 1 AND 4),
    error_code          VARCHAR(8) NOT NULL,
    error_message       VARCHAR(200) NOT NULL,
    process_date        DATE NOT NULL,
    process_time        TIME NOT NULL,
    user_id             VARCHAR(8) NOT NULL,
    additional_info     VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);

-- Indexes for error_log table
CREATE INDEX idx_errlog_process ON error_log (process_date, error_severity DESC);
CREATE INDEX idx_errlog_program ON error_log (program_id, error_timestamp);
CREATE INDEX idx_errlog_severity ON error_log (error_severity, process_date);

-- Comments for documentation
COMMENT ON TABLE error_log IS 'Error Logging - Replaces DB2 ERRLOG table';
COMMENT ON COLUMN error_log.error_type IS 'S=System, A=Application, D=Data';
COMMENT ON COLUMN error_log.error_severity IS '1=Info, 2=Warning, 3=Error, 4=Severe';

-- Cleanup function for old error logs
CREATE OR REPLACE FUNCTION cleanup_error_logs(retention_days INTEGER)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM error_log
    WHERE process_date < CURRENT_DATE - retention_days;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
