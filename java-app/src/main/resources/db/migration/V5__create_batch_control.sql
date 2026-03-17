-- ============================================================================
-- V5: Batch Control Tables
-- Migrated from: BCHCTL.cpy copybook (lines 9-39)
-- Status codes: R=Ready, A=Active, W=Waiting, D=Done, E=Error
-- Supports the prerequisite job dependency model (OCCURS 10 TIMES)
-- ============================================================================

CREATE TABLE batch_control (
    job_name          CHAR(8)         NOT NULL,
    process_date      CHAR(8)         NOT NULL,
    sequence_no       INT             NOT NULL,
    status            CHAR(1)         NOT NULL DEFAULT 'R',
    step_name         CHAR(8),
    program_name      CHAR(8),
    start_time        CHAR(8),
    end_time          CHAR(8),
    prereq_count      INT             NOT NULL DEFAULT 0,
    return_code       INT             NOT NULL DEFAULT 0,
    error_desc        VARCHAR(80),
    restart_count     INT             NOT NULL DEFAULT 0,
    attempt_ts        TIMESTAMP,
    complete_ts       TIMESTAMP,
    CONSTRAINT pk_batch_control PRIMARY KEY (job_name, process_date, sequence_no),
    CONSTRAINT chk_batch_status CHECK (status IN ('R', 'A', 'W', 'D', 'E'))
);

-- Prerequisite jobs table (replaces BCT-PREREQ-JOBS OCCURS 10 TIMES array)
CREATE TABLE batch_control_prereqs (
    job_name          CHAR(8)         NOT NULL,
    process_date      CHAR(8)         NOT NULL,
    sequence_no       INT             NOT NULL,
    prereq_index      INT             NOT NULL,
    prereq_name       CHAR(8)         NOT NULL,
    prereq_seq        INT             NOT NULL DEFAULT 0,
    prereq_rc         INT             NOT NULL DEFAULT 0,
    CONSTRAINT pk_batch_prereqs PRIMARY KEY (job_name, process_date, sequence_no, prereq_index),
    CONSTRAINT fk_prereqs_control FOREIGN KEY (job_name, process_date, sequence_no)
        REFERENCES batch_control(job_name, process_date, sequence_no)
);

-- Index for finding jobs by status
CREATE INDEX idx_batch_control_status ON batch_control (status, process_date);

-- Index for prerequisite lookups
CREATE INDEX idx_batch_prereqs_name ON batch_control_prereqs (prereq_name, process_date);
