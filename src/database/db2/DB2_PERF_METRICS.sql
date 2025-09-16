--********************************************************************
-- VERSION: 1.0
-- DATE: 2024
--********************************************************************

--====================================================================
--====================================================================
CREATE TABLE DB2_PERF_METRICS (
    PLAN_NAME       CHAR(8)         NOT NULL,
    CONNECT_TIME    TIMESTAMP       NOT NULL,
    RESPONSE_TIME   DECIMAL(9,3)    NOT NULL,
    THREAD_ID       CHAR(8)         NOT NULL,
    PROGRAM_NAME    CHAR(8)         NOT NULL,
    LAST_MAINT_DATE TIMESTAMP       NOT NULL,
    LAST_MAINT_USER VARCHAR(8)      NOT NULL,
    PRIMARY KEY (PLAN_NAME, CONNECT_TIME, THREAD_ID)
);

--====================================================================
--====================================================================
CREATE INDEX IDX_PERF_METRICS_PROGRAM 
    ON DB2_PERF_METRICS (PROGRAM_NAME, CONNECT_TIME);

CREATE INDEX IDX_PERF_METRICS_RESPONSE 
    ON DB2_PERF_METRICS (RESPONSE_TIME DESC, CONNECT_TIME);

--====================================================================
-- NOTES:
--====================================================================
--********************************************************************
