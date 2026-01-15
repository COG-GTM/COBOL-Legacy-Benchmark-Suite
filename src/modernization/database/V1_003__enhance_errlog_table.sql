--=====================================================================
-- ERRLOG Table Enhancement (Phase 1 - Modernization)
-- Adds microservice and modern observability fields
-- Version: 1.0
-- Date: 2024
--=====================================================================

--====================================================================
-- ERRLOG TABLE ENHANCEMENTS
-- Source: src/programs/online/ERRHNDL.cbl
-- Original table: src/database/db2/ERRLOG.sql
--====================================================================

-- Add new columns for microservice architecture support
ALTER TABLE ERRLOG ADD COLUMN
    MICROSERVICE_NAME VARCHAR(50);

ALTER TABLE ERRLOG ADD COLUMN
    REQUEST_ID VARCHAR(36);

ALTER TABLE ERRLOG ADD COLUMN
    USER_SESSION_ID VARCHAR(50);

ALTER TABLE ERRLOG ADD COLUMN
    STACK_TRACE CLOB(32K);

ALTER TABLE ERRLOG ADD COLUMN
    RESOLUTION_STATUS VARCHAR(20) DEFAULT 'OPEN';

ALTER TABLE ERRLOG ADD COLUMN
    RESOLVED_TIMESTAMP TIMESTAMP;

ALTER TABLE ERRLOG ADD COLUMN
    RESOLVED_BY VARCHAR(50);

ALTER TABLE ERRLOG ADD COLUMN
    CORRELATION_ID VARCHAR(36);

ALTER TABLE ERRLOG ADD COLUMN
    ENVIRONMENT VARCHAR(20) DEFAULT 'PRODUCTION';

ALTER TABLE ERRLOG ADD COLUMN
    HOST_NAME VARCHAR(100);

ALTER TABLE ERRLOG ADD COLUMN
    ERROR_CATEGORY VARCHAR(30);

--====================================================================
-- NEW INDEXES FOR ENHANCED COLUMNS
--====================================================================

-- Index for microservice filtering
CREATE INDEX ERRLOG_IX_MICROSERVICE
  ON ERRLOG
  (MICROSERVICE_NAME ASC,
   ERROR_TIMESTAMP DESC);

-- Index for request tracing
CREATE INDEX ERRLOG_IX_REQUEST
  ON ERRLOG
  (REQUEST_ID ASC);

-- Index for correlation tracking
CREATE INDEX ERRLOG_IX_CORRELATION
  ON ERRLOG
  (CORRELATION_ID ASC);

-- Index for resolution status monitoring
CREATE INDEX ERRLOG_IX_RESOLUTION
  ON ERRLOG
  (RESOLUTION_STATUS ASC,
   ERROR_TIMESTAMP DESC);

-- Index for session-based error lookup
CREATE INDEX ERRLOG_IX_SESSION
  ON ERRLOG
  (USER_SESSION_ID ASC,
   ERROR_TIMESTAMP DESC);

--====================================================================
-- COLUMN COMMENTS
--====================================================================

COMMENT ON COLUMN ERRLOG.MICROSERVICE_NAME IS
  'Name of the microservice that generated the error (for distributed systems)';
COMMENT ON COLUMN ERRLOG.REQUEST_ID IS
  'Unique request identifier (UUID) for distributed tracing';
COMMENT ON COLUMN ERRLOG.USER_SESSION_ID IS
  'User session identifier for session-based error tracking';
COMMENT ON COLUMN ERRLOG.STACK_TRACE IS
  'Full stack trace for debugging (replaces truncated ADDITIONAL_INFO)';
COMMENT ON COLUMN ERRLOG.RESOLUTION_STATUS IS
  'Error resolution status: OPEN, IN_PROGRESS, RESOLVED, IGNORED';
COMMENT ON COLUMN ERRLOG.RESOLVED_TIMESTAMP IS
  'Timestamp when the error was resolved';
COMMENT ON COLUMN ERRLOG.RESOLVED_BY IS
  'User or system that resolved the error';
COMMENT ON COLUMN ERRLOG.CORRELATION_ID IS
  'Correlation ID for linking related errors across services';
COMMENT ON COLUMN ERRLOG.ENVIRONMENT IS
  'Environment where error occurred: DEVELOPMENT, STAGING, PRODUCTION';
COMMENT ON COLUMN ERRLOG.HOST_NAME IS
  'Host/server name where the error occurred';
COMMENT ON COLUMN ERRLOG.ERROR_CATEGORY IS
  'Error category: DATABASE, NETWORK, VALIDATION, BUSINESS_LOGIC, SECURITY';

--====================================================================
-- CHECK CONSTRAINTS
--====================================================================

ALTER TABLE ERRLOG
  ADD CONSTRAINT CHK_ERRLOG_RESOLUTION_STATUS
  CHECK (RESOLUTION_STATUS IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'IGNORED', 'ESCALATED'));

ALTER TABLE ERRLOG
  ADD CONSTRAINT CHK_ERRLOG_ENVIRONMENT
  CHECK (ENVIRONMENT IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION', 'DR'));

ALTER TABLE ERRLOG
  ADD CONSTRAINT CHK_ERRLOG_CATEGORY
  CHECK (ERROR_CATEGORY IS NULL OR ERROR_CATEGORY IN 
    ('DATABASE', 'NETWORK', 'VALIDATION', 'BUSINESS_LOGIC', 'SECURITY', 
     'INTEGRATION', 'CONFIGURATION', 'RESOURCE', 'UNKNOWN'));

--====================================================================
-- STORED PROCEDURES
--====================================================================

-- Procedure to update error resolution status
CREATE PROCEDURE UPDATE_ERROR_RESOLUTION
  (IN P_ERROR_TIMESTAMP TIMESTAMP,
   IN P_PROGRAM_ID CHAR(8),
   IN P_NEW_STATUS VARCHAR(20),
   IN P_RESOLVED_BY VARCHAR(50))
  LANGUAGE SQL
BEGIN
  UPDATE ERRLOG
  SET RESOLUTION_STATUS = P_NEW_STATUS,
      RESOLVED_TIMESTAMP = CASE 
        WHEN P_NEW_STATUS = 'RESOLVED' THEN CURRENT_TIMESTAMP 
        ELSE RESOLVED_TIMESTAMP 
      END,
      RESOLVED_BY = P_RESOLVED_BY
  WHERE ERROR_TIMESTAMP = P_ERROR_TIMESTAMP
    AND PROGRAM_ID = P_PROGRAM_ID;
END;

-- Procedure to get error statistics by microservice
CREATE PROCEDURE GET_ERROR_STATS_BY_SERVICE
  (IN P_START_DATE DATE,
   IN P_END_DATE DATE)
  LANGUAGE SQL
  DYNAMIC RESULT SETS 1
BEGIN
  DECLARE C1 CURSOR WITH RETURN FOR
    SELECT MICROSERVICE_NAME,
           ERROR_SEVERITY,
           RESOLUTION_STATUS,
           COUNT(*) AS ERROR_COUNT
    FROM ERRLOG
    WHERE PROCESS_DATE BETWEEN P_START_DATE AND P_END_DATE
    GROUP BY MICROSERVICE_NAME, ERROR_SEVERITY, RESOLUTION_STATUS
    ORDER BY MICROSERVICE_NAME, ERROR_SEVERITY DESC;
  OPEN C1;
END;

--====================================================================
-- MIGRATION NOTES:
--====================================================================
-- 1. Existing ERRLOG data will have NULL values for new columns
-- 2. MICROSERVICE_NAME defaults to 'LEGACY_COBOL' for migrated records
-- 3. REQUEST_ID can be populated from ERR-TRACE-ID in ERRHNDL.cbl
-- 4. STACK_TRACE replaces limited ADDITIONAL_INFO for detailed debugging
-- 5. RESOLUTION_STATUS enables error lifecycle management
-- 6. CORRELATION_ID enables distributed tracing across services
--====================================================================

-- Set default values for existing records
UPDATE ERRLOG
SET MICROSERVICE_NAME = 'LEGACY_COBOL',
    RESOLUTION_STATUS = 'RESOLVED',
    ENVIRONMENT = 'PRODUCTION'
WHERE MICROSERVICE_NAME IS NULL;

--====================================================================
-- GRANTS
--====================================================================

GRANT SELECT, INSERT, UPDATE ON ERRLOG TO POSAPP;
GRANT SELECT ON ERRLOG TO POSRPT;
GRANT EXECUTE ON PROCEDURE UPDATE_ERROR_RESOLUTION TO POSAPP;
GRANT EXECUTE ON PROCEDURE GET_ERROR_STATS_BY_SERVICE TO POSRPT;
