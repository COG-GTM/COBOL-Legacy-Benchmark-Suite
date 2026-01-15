--=====================================================================
-- AUDITLOG Table Enhancement (Phase 1 - Modernization)
-- Enhances audit logging with modern observability fields
-- Version: 1.0
-- Date: 2024
--=====================================================================

--====================================================================
-- AUDITLOG TABLE CREATION AND ENHANCEMENT
-- Source: src/programs/online/SECMGR.cbl (P300-LOG-ACCESS)
-- Purpose: Security and access audit trail
--====================================================================

-- Create AUDITLOG table if not exists (based on SECMGR.cbl INSERT statement)
CREATE TABLESPACE AUDITLOG
  IN POSMVP
  USING STOGROUP POSMVPSG
  PRIQTY 7200
  SECQTY 1440
  SEGSIZE 64
  COMPRESS YES
  PARTITION BY RANGE(AUDIT_DATE)
  (PARTITION 1 ENDING AT ('2024-03-31'),
   PARTITION 2 ENDING AT ('2024-06-30'),
   PARTITION 3 ENDING AT ('2024-09-30'),
   PARTITION 4 ENDING AT ('2024-12-31'),
   PARTITION 5 ENDING AT ('2025-03-31'),
   PARTITION 6 ENDING AT ('2025-06-30'),
   PARTITION 7 ENDING AT ('2025-09-30'),
   PARTITION 8 ENDING AT ('2025-12-31'));

CREATE TABLE AUDITLOG (
    -- Primary Key
    AUDIT_ID          BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,
    
    -- Original fields from SECMGR.cbl P300-LOG-ACCESS
    TIMESTAMP         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    USER_ID           VARCHAR(8)      NOT NULL,
    TERMINAL_ID       VARCHAR(4),
    TRANS_ID          VARCHAR(4),
    PROGRAM           VARCHAR(8),
    ACCESS_TYPE       VARCHAR(20),
    
    -- Enhanced fields for modern audit requirements
    AUDIT_DATE        DATE            NOT NULL DEFAULT CURRENT_DATE,
    
    -- Request Context
    REQUEST_ID        VARCHAR(36),
    CORRELATION_ID    VARCHAR(36),
    SESSION_ID        VARCHAR(50),
    
    -- Client Information
    CLIENT_IP         VARCHAR(45),
    CLIENT_USER_AGENT VARCHAR(500),
    CLIENT_DEVICE     VARCHAR(100),
    
    -- Action Details
    ACTION_CATEGORY   VARCHAR(30)     NOT NULL DEFAULT 'ACCESS',
    ACTION_RESULT     VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS',
    ACTION_DETAILS    VARCHAR(2000),
    
    -- Resource Information
    RESOURCE_TYPE     VARCHAR(50),
    RESOURCE_ID       VARCHAR(100),
    RESOURCE_NAME     VARCHAR(200),
    
    -- Data Change Tracking
    OLD_VALUE         CLOB(100K),
    NEW_VALUE         CLOB(100K),
    CHANGED_FIELDS    VARCHAR(1000),
    
    -- Security Context
    AUTHENTICATION_METHOD VARCHAR(30),
    AUTHORIZATION_LEVEL   VARCHAR(20),
    RISK_SCORE           DECIMAL(5,2),
    
    -- Compliance Fields
    COMPLIANCE_FLAG   CHAR(1)         DEFAULT 'N',
    RETENTION_PERIOD  INTEGER         DEFAULT 2555,
    DATA_CLASSIFICATION VARCHAR(20)   DEFAULT 'INTERNAL',
    
    -- Microservice Context
    SERVICE_NAME      VARCHAR(50),
    SERVICE_VERSION   VARCHAR(20),
    ENVIRONMENT       VARCHAR(20)     DEFAULT 'PRODUCTION',
    HOST_NAME         VARCHAR(100),
    
    -- Performance Metrics
    RESPONSE_TIME_MS  INTEGER,
    
    -- Constraints
    CONSTRAINT PK_AUDITLOG 
        PRIMARY KEY (AUDIT_ID)
) IN POSMVP.AUDITLOG;

--====================================================================
-- INDEXES
--====================================================================

-- Date-based lookup (primary access pattern)
CREATE INDEX AUDITLOG_IX1
  ON AUDITLOG
  (AUDIT_DATE ASC,
   TIMESTAMP DESC);

-- User activity lookup
CREATE INDEX AUDITLOG_IX2
  ON AUDITLOG
  (USER_ID ASC,
   TIMESTAMP DESC);

-- Request tracing
CREATE INDEX AUDITLOG_IX3
  ON AUDITLOG
  (REQUEST_ID ASC);

-- Correlation tracking
CREATE INDEX AUDITLOG_IX4
  ON AUDITLOG
  (CORRELATION_ID ASC);

-- Session-based lookup
CREATE INDEX AUDITLOG_IX5
  ON AUDITLOG
  (SESSION_ID ASC,
   TIMESTAMP DESC);

-- Action result monitoring
CREATE INDEX AUDITLOG_IX6
  ON AUDITLOG
  (ACTION_RESULT ASC,
   AUDIT_DATE DESC);

-- Resource access tracking
CREATE INDEX AUDITLOG_IX7
  ON AUDITLOG
  (RESOURCE_TYPE ASC,
   RESOURCE_ID ASC,
   TIMESTAMP DESC);

-- Compliance reporting
CREATE INDEX AUDITLOG_IX8
  ON AUDITLOG
  (COMPLIANCE_FLAG ASC,
   DATA_CLASSIFICATION ASC,
   AUDIT_DATE DESC);

-- Service-based lookup
CREATE INDEX AUDITLOG_IX9
  ON AUDITLOG
  (SERVICE_NAME ASC,
   TIMESTAMP DESC);

--====================================================================
-- TABLE COMMENTS
--====================================================================

COMMENT ON TABLE AUDITLOG IS
  'Audit Log Table - Security and access audit trail with modern observability';

COMMENT ON COLUMN AUDITLOG.TIMESTAMP IS
  'Audit event timestamp (from SECMGR WS-TIMESTAMP)';
COMMENT ON COLUMN AUDITLOG.USER_ID IS
  'User identifier (from SECMGR WS-USER-ID)';
COMMENT ON COLUMN AUDITLOG.TERMINAL_ID IS
  'CICS terminal identifier (from SECMGR WS-TERMINAL-ID)';
COMMENT ON COLUMN AUDITLOG.TRANS_ID IS
  'CICS transaction identifier (from SECMGR WS-TRANSACTION-ID)';
COMMENT ON COLUMN AUDITLOG.PROGRAM IS
  'Program name (from SECMGR WS-PROGRAM-NAME)';
COMMENT ON COLUMN AUDITLOG.ACCESS_TYPE IS
  'Type of access performed (from SECMGR WS-ACCESS-TYPE)';
COMMENT ON COLUMN AUDITLOG.REQUEST_ID IS
  'Unique request identifier (UUID) for distributed tracing';
COMMENT ON COLUMN AUDITLOG.CORRELATION_ID IS
  'Correlation ID for linking related audit events';
COMMENT ON COLUMN AUDITLOG.ACTION_CATEGORY IS
  'Category: ACCESS, LOGIN, LOGOUT, DATA_READ, DATA_WRITE, CONFIG_CHANGE, SECURITY';
COMMENT ON COLUMN AUDITLOG.ACTION_RESULT IS
  'Result: SUCCESS, FAILURE, DENIED, ERROR, TIMEOUT';
COMMENT ON COLUMN AUDITLOG.OLD_VALUE IS
  'Previous value for data change auditing';
COMMENT ON COLUMN AUDITLOG.NEW_VALUE IS
  'New value for data change auditing';
COMMENT ON COLUMN AUDITLOG.RISK_SCORE IS
  'Calculated risk score for security analytics (0.00-100.00)';
COMMENT ON COLUMN AUDITLOG.COMPLIANCE_FLAG IS
  'Compliance-relevant event flag (Y/N)';
COMMENT ON COLUMN AUDITLOG.RETENTION_PERIOD IS
  'Retention period in days (default 7 years = 2555 days)';
COMMENT ON COLUMN AUDITLOG.DATA_CLASSIFICATION IS
  'Data classification: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED';

--====================================================================
-- CHECK CONSTRAINTS
--====================================================================

ALTER TABLE AUDITLOG
  ADD CONSTRAINT CHK_AUDITLOG_ACTION_CATEGORY
  CHECK (ACTION_CATEGORY IN ('ACCESS', 'LOGIN', 'LOGOUT', 'DATA_READ', 'DATA_WRITE', 
    'DATA_DELETE', 'CONFIG_CHANGE', 'SECURITY', 'ADMIN', 'SYSTEM', 'API_CALL'));

ALTER TABLE AUDITLOG
  ADD CONSTRAINT CHK_AUDITLOG_ACTION_RESULT
  CHECK (ACTION_RESULT IN ('SUCCESS', 'FAILURE', 'DENIED', 'ERROR', 'TIMEOUT', 'PARTIAL'));

ALTER TABLE AUDITLOG
  ADD CONSTRAINT CHK_AUDITLOG_COMPLIANCE_FLAG
  CHECK (COMPLIANCE_FLAG IN ('Y', 'N'));

ALTER TABLE AUDITLOG
  ADD CONSTRAINT CHK_AUDITLOG_DATA_CLASS
  CHECK (DATA_CLASSIFICATION IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED', 'PII', 'PHI'));

ALTER TABLE AUDITLOG
  ADD CONSTRAINT CHK_AUDITLOG_ENVIRONMENT
  CHECK (ENVIRONMENT IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION', 'DR'));

ALTER TABLE AUDITLOG
  ADD CONSTRAINT CHK_AUDITLOG_RISK_SCORE
  CHECK (RISK_SCORE IS NULL OR (RISK_SCORE >= 0 AND RISK_SCORE <= 100));

--====================================================================
-- STORED PROCEDURES
--====================================================================

-- Procedure to log access (modernized from SECMGR P300-LOG-ACCESS)
CREATE PROCEDURE LOG_ACCESS
  (IN P_USER_ID VARCHAR(8),
   IN P_TERMINAL_ID VARCHAR(4),
   IN P_TRANS_ID VARCHAR(4),
   IN P_PROGRAM VARCHAR(8),
   IN P_ACCESS_TYPE VARCHAR(20),
   IN P_REQUEST_ID VARCHAR(36),
   IN P_ACTION_RESULT VARCHAR(20),
   IN P_ACTION_DETAILS VARCHAR(2000))
  LANGUAGE SQL
BEGIN
  INSERT INTO AUDITLOG
    (USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE,
     REQUEST_ID, ACTION_RESULT, ACTION_DETAILS, SERVICE_NAME)
  VALUES
    (P_USER_ID, P_TERMINAL_ID, P_TRANS_ID, P_PROGRAM, P_ACCESS_TYPE,
     P_REQUEST_ID, P_ACTION_RESULT, P_ACTION_DETAILS, 'LEGACY_COBOL');
END;

-- Procedure to log data changes
CREATE PROCEDURE LOG_DATA_CHANGE
  (IN P_USER_ID VARCHAR(8),
   IN P_RESOURCE_TYPE VARCHAR(50),
   IN P_RESOURCE_ID VARCHAR(100),
   IN P_OLD_VALUE CLOB(100K),
   IN P_NEW_VALUE CLOB(100K),
   IN P_CHANGED_FIELDS VARCHAR(1000),
   IN P_REQUEST_ID VARCHAR(36))
  LANGUAGE SQL
BEGIN
  INSERT INTO AUDITLOG
    (USER_ID, ACTION_CATEGORY, RESOURCE_TYPE, RESOURCE_ID,
     OLD_VALUE, NEW_VALUE, CHANGED_FIELDS, REQUEST_ID,
     COMPLIANCE_FLAG, DATA_CLASSIFICATION)
  VALUES
    (P_USER_ID, 'DATA_WRITE', P_RESOURCE_TYPE, P_RESOURCE_ID,
     P_OLD_VALUE, P_NEW_VALUE, P_CHANGED_FIELDS, P_REQUEST_ID,
     'Y', 'CONFIDENTIAL');
END;

-- Procedure to get user activity report
CREATE PROCEDURE GET_USER_ACTIVITY
  (IN P_USER_ID VARCHAR(8),
   IN P_START_DATE DATE,
   IN P_END_DATE DATE)
  LANGUAGE SQL
  DYNAMIC RESULT SETS 1
BEGIN
  DECLARE C1 CURSOR WITH RETURN FOR
    SELECT TIMESTAMP, ACTION_CATEGORY, ACTION_RESULT,
           RESOURCE_TYPE, RESOURCE_NAME, ACTION_DETAILS
    FROM AUDITLOG
    WHERE USER_ID = P_USER_ID
      AND AUDIT_DATE BETWEEN P_START_DATE AND P_END_DATE
    ORDER BY TIMESTAMP DESC;
  OPEN C1;
END;

-- Procedure to get compliance report
CREATE PROCEDURE GET_COMPLIANCE_REPORT
  (IN P_START_DATE DATE,
   IN P_END_DATE DATE,
   IN P_DATA_CLASSIFICATION VARCHAR(20))
  LANGUAGE SQL
  DYNAMIC RESULT SETS 1
BEGIN
  DECLARE C1 CURSOR WITH RETURN FOR
    SELECT AUDIT_DATE, USER_ID, ACTION_CATEGORY, ACTION_RESULT,
           RESOURCE_TYPE, RESOURCE_ID, DATA_CLASSIFICATION
    FROM AUDITLOG
    WHERE AUDIT_DATE BETWEEN P_START_DATE AND P_END_DATE
      AND COMPLIANCE_FLAG = 'Y'
      AND (P_DATA_CLASSIFICATION IS NULL OR DATA_CLASSIFICATION = P_DATA_CLASSIFICATION)
    ORDER BY AUDIT_DATE DESC, TIMESTAMP DESC;
  OPEN C1;
END;

-- Procedure to archive old audit records
CREATE PROCEDURE ARCHIVE_AUDIT_RECORDS
  (IN P_ARCHIVE_BEFORE_DATE DATE)
  LANGUAGE SQL
BEGIN
  -- Archive records older than specified date
  INSERT INTO AUDITLOG_ARCHIVE
  SELECT * FROM AUDITLOG
  WHERE AUDIT_DATE < P_ARCHIVE_BEFORE_DATE;
  
  -- Delete archived records
  DELETE FROM AUDITLOG
  WHERE AUDIT_DATE < P_ARCHIVE_BEFORE_DATE;
END;

--====================================================================
-- GRANTS
--====================================================================

GRANT SELECT, INSERT ON AUDITLOG TO POSAPP;
GRANT SELECT ON AUDITLOG TO POSRPT;
GRANT EXECUTE ON PROCEDURE LOG_ACCESS TO POSAPP;
GRANT EXECUTE ON PROCEDURE LOG_DATA_CHANGE TO POSAPP;
GRANT EXECUTE ON PROCEDURE GET_USER_ACTIVITY TO POSRPT;
GRANT EXECUTE ON PROCEDURE GET_COMPLIANCE_REPORT TO POSRPT;
GRANT EXECUTE ON PROCEDURE ARCHIVE_AUDIT_RECORDS TO POSAPP;

--====================================================================
-- NOTES:
--====================================================================
-- 1. Table is partitioned by AUDIT_DATE for efficient date range queries
-- 2. Original SECMGR fields preserved for backward compatibility
-- 3. Enhanced fields support modern observability and compliance requirements
-- 4. OLD_VALUE and NEW_VALUE use CLOB for large data change tracking
-- 5. RISK_SCORE enables security analytics and anomaly detection
-- 6. RETENTION_PERIOD supports regulatory compliance (default 7 years)
-- 7. DATA_CLASSIFICATION supports data governance requirements
-- 8. Procedures modernize SECMGR.cbl audit logging functionality
--====================================================================
