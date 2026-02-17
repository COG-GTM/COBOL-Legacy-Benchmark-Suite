--********************************************************************
-- DB2 TABLE DEFINITIONS FOR SECURITY MANAGER (SECMGR)
-- Tables referenced by SECMGR.cbl P200-CHECK-AUTH and P300-LOG-ACCESS
-- VERSION: 1.0
--********************************************************************

--====================================================================
-- AUTHORIZATION FILE TABLE
-- Used by P200-CHECK-AUTH to verify user permissions
--====================================================================
CREATE TABLE AUTHFILE (
    USER_ID       CHAR(8)     NOT NULL,
    RESOURCE      CHAR(8)     NOT NULL,
    ACCESS_TYPE   CHAR(8)     NOT NULL,
    PRIMARY KEY (USER_ID, RESOURCE, ACCESS_TYPE)
);

CREATE INDEX IDX_AUTHFILE_LOOKUP
    ON AUTHFILE (USER_ID, RESOURCE, ACCESS_TYPE);

--====================================================================
-- AUDIT LOG TABLE
-- Used by P300-LOG-ACCESS to record access attempts
--====================================================================
CREATE TABLE AUDITLOG (
    ID            CHAR(36)    NOT NULL,
    TIMESTAMP     TIMESTAMP   NOT NULL,
    USER_ID       CHAR(8)     NOT NULL,
    TERMINAL_ID   CHAR(4)     NOT NULL,
    TRANS_ID      CHAR(4)     NOT NULL,
    PROGRAM       CHAR(8)     NOT NULL,
    ACCESS_TYPE   CHAR(8)     NOT NULL,
    PRIMARY KEY (ID)
);

CREATE INDEX IDX_AUDITLOG_USER
    ON AUDITLOG (USER_ID, TIMESTAMP);

CREATE INDEX IDX_AUDITLOG_TIME
    ON AUDITLOG (TIMESTAMP);

--********************************************************************
-- NOTES:
-- 1. AUTHFILE composite key ensures unique permission entries
-- 2. IDX_AUTHFILE_LOOKUP optimizes the authorization query in
--    P200-CHECK-AUTH (SELECT COUNT(*) WHERE USER_ID AND RESOURCE
--    AND ACCESS_TYPE)
-- 3. AUDITLOG indexes support queries by user and time range
-- 4. ID column uses UUID format for the modernized implementation;
--    original COBOL relied on DB2 implicit row identification
--********************************************************************
