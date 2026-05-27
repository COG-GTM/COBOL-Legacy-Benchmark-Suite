--====================================================================
-- ERROR LOG TABLE
-- Translated from DB2 (ERRLOG.sql lines 15-26)
--====================================================================
CREATE TABLE error_log (
    error_timestamp   TIMESTAMP        NOT NULL,
    program_id        VARCHAR(8)       NOT NULL,
    error_type        VARCHAR(1)       NOT NULL,
    error_severity    INTEGER          NOT NULL,
    error_code        VARCHAR(8)       NOT NULL,
    error_message     VARCHAR(200)     NOT NULL,
    process_date      DATE             NOT NULL,
    process_time      TIME             NOT NULL,
    user_id           VARCHAR(8)       NOT NULL,
    additional_info   VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);
