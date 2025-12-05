-- ============================================================================
-- V7: Create Batch Control Schema
-- Description: Batch job control and checkpoint/restart support
-- Replaces: VSAM BCHCTL (Batch Control File)
-- Source: COBOL Copybook BCHCTL.cpy
-- ============================================================================

-- Batch Control table (replaces VSAM BCHCTL)
CREATE TABLE batch_control (
    job_name            VARCHAR(8) NOT NULL,
    process_date        VARCHAR(8) NOT NULL,
    sequence_number     INTEGER NOT NULL,
    status              CHAR(1) NOT NULL DEFAULT 'R' CHECK (status IN ('R', 'A', 'W', 'D', 'E')),
    step_name           VARCHAR(8),
    program_name        VARCHAR(8),
    start_time          TIME,
    end_time            TIME,
    return_code         INTEGER,
    error_description   VARCHAR(80),
    restart_count       INTEGER DEFAULT 0,
    attempt_timestamp   TIMESTAMP,
    complete_timestamp  TIMESTAMP,
    PRIMARY KEY (job_name, process_date, sequence_number)
);

-- Indexes for batch_control table
CREATE INDEX idx_batch_status ON batch_control (status, process_date);
CREATE INDEX idx_batch_program ON batch_control (program_name, process_date);

-- Return Codes table (replaces DB2 RTNCODES)
CREATE TABLE return_codes (
    timestamp           TIMESTAMP NOT NULL,
    program_id          VARCHAR(8) NOT NULL,
    return_code         INTEGER NOT NULL,
    highest_code        INTEGER NOT NULL,
    status_code         CHAR(1) NOT NULL,
    message_text        VARCHAR(80),
    PRIMARY KEY (timestamp, program_id)
);

-- Indexes for return_codes table
CREATE INDEX idx_rtncodes_program ON return_codes (program_id, timestamp);
CREATE INDEX idx_rtncodes_status ON return_codes (status_code, timestamp);

-- Comments for documentation
COMMENT ON TABLE batch_control IS 'Batch Control - Replaces VSAM BCHCTL file';
COMMENT ON COLUMN batch_control.status IS 'R=Ready, A=Active, W=Waiting, D=Done, E=Error';
COMMENT ON TABLE return_codes IS 'Return Codes - Replaces DB2 RTNCODES table';
