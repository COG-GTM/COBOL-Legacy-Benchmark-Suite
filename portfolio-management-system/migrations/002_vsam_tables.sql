-- Migration: 002_vsam_tables.sql
-- Description: PostgreSQL tables migrated from VSAM file definitions
-- Source: src/database/vsam/vsam-definitions.txt
-- Date: 2026-01-28

-- ============================================================================
-- VSAM FILE MIGRATION NOTES
-- ============================================================================
-- VSAM KSDS (Key-Sequenced Data Set) files are migrated to PostgreSQL tables
-- with primary keys matching the original VSAM key structure.
--
-- Original VSAM characteristics:
-- - PORTMSTR: KSDS, 400-byte records, 12-byte key
-- - TRANHIST: KSDS, 300-byte records, 20-byte key
-- - POSHIST: KSDS, 350-byte records, 18-byte key
--
-- PostgreSQL equivalents use:
-- - Composite primary keys matching VSAM key structure
-- - B-tree indexes for KSDS-like access patterns
-- - Additional indexes for alternate key access (VSAM AIX equivalent)
-- ============================================================================

-- ============================================================================
-- POSITION FILE TABLE (from VSAM POSHIST/POSFILE)
-- Operational position data accessed by online programs
-- Key: Portfolio ID (8) + Position Date (8) + Investment ID (10) = 26 bytes
-- ============================================================================

CREATE TABLE IF NOT EXISTS position_file (
    -- Key fields (matching VSAM key structure)
    portfolio_id        CHAR(8)         NOT NULL,
    position_date       CHAR(8)         NOT NULL,  -- YYYYMMDD format
    investment_id       CHAR(10)        NOT NULL,
    
    -- Position data fields
    quantity            NUMERIC(15,4)   NOT NULL DEFAULT 0,
    cost_basis          NUMERIC(15,2)   NOT NULL DEFAULT 0,
    market_value        NUMERIC(15,2)   NOT NULL DEFAULT 0,
    currency_code       CHAR(3)         NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    
    -- Audit fields
    last_maint_date     CHAR(26),       -- ISO timestamp string
    last_maint_user     CHAR(8),
    
    CONSTRAINT pk_position_file PRIMARY KEY (portfolio_id, position_date, investment_id)
);

-- Alternate index equivalent (VSAM AIX)
CREATE INDEX idx_position_file_investment ON position_file (investment_id, position_date);
CREATE INDEX idx_position_file_status ON position_file (status);

COMMENT ON TABLE position_file IS 'Position file - migrated from VSAM KSDS POSHIST/POSFILE';

-- ============================================================================
-- TRANSACTION FILE TABLE (from VSAM TRANHIST)
-- Transaction history accessed by batch and online programs
-- Key: Trans Date (8) + Trans Time (6) + Portfolio ID (8) + Sequence (6) = 28 bytes
-- ============================================================================

CREATE TABLE IF NOT EXISTS transaction_file (
    -- Key fields (matching VSAM key structure)
    trans_date          CHAR(8)         NOT NULL,  -- YYYYMMDD format
    trans_time          CHAR(6)         NOT NULL,  -- HHMMSS format
    portfolio_id        CHAR(8)         NOT NULL,
    sequence_no         CHAR(6)         NOT NULL,
    
    -- Transaction data fields
    investment_id       CHAR(10)        NOT NULL,
    trans_type          CHAR(2)         NOT NULL,
    quantity            NUMERIC(15,4)   NOT NULL DEFAULT 0,
    price               NUMERIC(15,4)   NOT NULL DEFAULT 0,
    amount              NUMERIC(15,2)   NOT NULL DEFAULT 0,
    currency_code       CHAR(3)         NOT NULL DEFAULT 'USD',
    status              CHAR(1)         NOT NULL DEFAULT 'P',
    
    -- Audit fields
    process_date        CHAR(26),       -- ISO timestamp string
    process_user        CHAR(8),
    
    CONSTRAINT pk_transaction_file PRIMARY KEY (trans_date, trans_time, portfolio_id, sequence_no)
);

-- Alternate indexes (VSAM AIX equivalents)
CREATE INDEX idx_transaction_file_portfolio ON transaction_file (portfolio_id, trans_date);
CREATE INDEX idx_transaction_file_investment ON transaction_file (investment_id, trans_date);
CREATE INDEX idx_transaction_file_status ON transaction_file (status, trans_date);

COMMENT ON TABLE transaction_file IS 'Transaction file - migrated from VSAM KSDS TRANHIST';

-- ============================================================================
-- BATCH CONTROL FILE TABLE (from VSAM batch control)
-- Batch process control and checkpoint/restart data
-- ============================================================================

CREATE TABLE IF NOT EXISTS batch_control (
    process_id          CHAR(8)         NOT NULL,
    process_date        CHAR(8)         NOT NULL,  -- YYYYMMDD format
    
    -- Control data
    process_status      CHAR(1)         NOT NULL DEFAULT 'P',
    start_timestamp     TIMESTAMP,
    end_timestamp       TIMESTAMP,
    records_read        INTEGER         NOT NULL DEFAULT 0,
    records_processed   INTEGER         NOT NULL DEFAULT 0,
    records_rejected    INTEGER         NOT NULL DEFAULT 0,
    last_checkpoint     TIMESTAMP,
    checkpoint_key      VARCHAR(100),
    
    -- Error tracking
    error_count         INTEGER         NOT NULL DEFAULT 0,
    last_error_code     CHAR(8),
    last_error_message  VARCHAR(200),
    
    CONSTRAINT pk_batch_control PRIMARY KEY (process_id, process_date)
);

COMMENT ON TABLE batch_control IS 'Batch control file - migrated from VSAM batch control structures';

-- ============================================================================
-- PROCESS SEQUENCE FILE TABLE (from VSAM process control)
-- Defines batch job sequences and dependencies
-- ============================================================================

CREATE TABLE IF NOT EXISTS process_sequence (
    sequence_id         CHAR(8)         NOT NULL,
    step_number         INTEGER         NOT NULL,
    
    -- Process definition
    process_id          CHAR(8)         NOT NULL,
    process_name        VARCHAR(50)     NOT NULL,
    process_type        CHAR(1)         NOT NULL,
    
    -- Dependencies
    depends_on          VARCHAR(200),   -- Comma-separated list of process IDs
    
    -- Scheduling
    schedule_type       CHAR(1)         NOT NULL DEFAULT 'D',  -- D=Daily, W=Weekly, M=Monthly
    schedule_time       CHAR(6),        -- HHMMSS
    
    -- Status
    status              CHAR(1)         NOT NULL DEFAULT 'A',
    last_run_date       CHAR(8),
    last_run_status     CHAR(1),
    
    CONSTRAINT pk_process_sequence PRIMARY KEY (sequence_id, step_number)
);

CREATE INDEX idx_process_sequence_process ON process_sequence (process_id);

COMMENT ON TABLE process_sequence IS 'Process sequence file - migrated from VSAM PRCCTL';

-- ============================================================================
-- MONITOR LOG FILE TABLE (from VSAM monitoring)
-- System monitoring and performance data
-- ============================================================================

CREATE TABLE IF NOT EXISTS monitor_log (
    log_timestamp       TIMESTAMP       NOT NULL,
    monitor_type        CHAR(4)         NOT NULL,
    
    -- Metrics
    metric_name         VARCHAR(50)     NOT NULL,
    metric_value        NUMERIC(15,4)   NOT NULL,
    metric_unit         CHAR(10),
    
    -- Context
    program_id          CHAR(8),
    resource_id         VARCHAR(50),
    
    CONSTRAINT pk_monitor_log PRIMARY KEY (log_timestamp, monitor_type, metric_name)
);

CREATE INDEX idx_monitor_log_type ON monitor_log (monitor_type, log_timestamp);

COMMENT ON TABLE monitor_log IS 'Monitor log file - migrated from VSAM monitoring structures';

-- ============================================================================
-- ARCHIVE CONTROL TABLE (from VSAM archive management)
-- Tracks archived data for retention management
-- ============================================================================

CREATE TABLE IF NOT EXISTS archive_control (
    archive_id          SERIAL          NOT NULL,
    archive_date        DATE            NOT NULL,
    
    -- Archive details
    source_table        VARCHAR(50)     NOT NULL,
    archive_type        CHAR(1)         NOT NULL,  -- D=Daily, M=Monthly, Y=Yearly
    records_archived    INTEGER         NOT NULL DEFAULT 0,
    
    -- Date range
    data_start_date     DATE            NOT NULL,
    data_end_date       DATE            NOT NULL,
    
    -- Status
    status              CHAR(1)         NOT NULL DEFAULT 'C',  -- C=Complete, P=Partial, F=Failed
    archive_location    VARCHAR(200),
    
    CONSTRAINT pk_archive_control PRIMARY KEY (archive_id)
);

CREATE INDEX idx_archive_control_table ON archive_control (source_table, archive_date);

COMMENT ON TABLE archive_control IS 'Archive control - migrated from VSAM archive management';
