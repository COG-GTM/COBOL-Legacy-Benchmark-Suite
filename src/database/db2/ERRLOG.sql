--=====================================================================
-- Table Name:   ERRLOG
-- Description:  Error Logging Table Definition
-- Database:     POSMVP
-- Tablespace:   ERRLOG
-- Version: 1.0
-- Date: 2024
--
-- Stores application, system, and data errors logged by ERRPROC.
-- Indexed by timestamp+program (primary) and date+severity
-- (secondary) for efficient error analysis and cleanup.
--
-- Grants: POSAPP (SELECT, INSERT), POSRPT (SELECT)
-- Cleanup: ERRLOG_CLEANUP stored procedure purges by retention.
--=====================================================================

CREATE TABLESPACE ERRLOG
  IN POSMVP
  USING STOGROUP POSMVPSG
  PRIQTY 1440
  SECQTY 720
  SEGSIZE 32
  COMPRESS YES;

-- Column definitions:
--   ERROR_TIMESTAMP - When the error occurred (part of PK)
--   PROGRAM_ID      - Which program raised the error (part of PK)
--   ERROR_TYPE      - S=System, A=Application, D=Data
--   ERROR_SEVERITY  - 1=Info, 2=Warning, 3=Error, 4=Severe
--   ERROR_CODE      - Application-specific error code
--   ERROR_MESSAGE   - Human-readable error description
--   PROCESS_DATE    - Business processing date
--   PROCESS_TIME    - Time within the processing cycle
--   USER_ID         - User or batch job that triggered the error
--   ADDITIONAL_INFO - Optional extended diagnostic data
CREATE TABLE ERRLOG
  (ERROR_TIMESTAMP   TIMESTAMP       NOT NULL,
   PROGRAM_ID        CHAR(8)         NOT NULL,
   ERROR_TYPE        CHAR(1)         NOT NULL,
   ERROR_SEVERITY    INTEGER         NOT NULL,
   ERROR_CODE        CHAR(8)         NOT NULL,
   ERROR_MESSAGE     VARCHAR(200)    NOT NULL,
   PROCESS_DATE      DATE            NOT NULL,
   PROCESS_TIME      TIME            NOT NULL,
   USER_ID           CHAR(8)         NOT NULL,
   ADDITIONAL_INFO   VARCHAR(500))
  IN POSMVP.ERRLOG;

-- Primary Index
CREATE UNIQUE INDEX ERRLOG_PK
  ON ERRLOG
  (ERROR_TIMESTAMP ASC,
   PROGRAM_ID ASC)
  CLUSTER;

ALTER TABLE ERRLOG
  ADD CONSTRAINT ERRLOG_PK
  PRIMARY KEY (ERROR_TIMESTAMP, PROGRAM_ID);

-- Secondary Index
CREATE INDEX ERRLOG_IX1
  ON ERRLOG
  (PROCESS_DATE ASC,
   ERROR_SEVERITY DESC);

-- Table Comments
COMMENT ON TABLE ERRLOG IS
  'Error Logging Table - Stores application errors and warnings';

COMMENT ON COLUMN ERRLOG.ERROR_TYPE IS
  'Error Type (S=System, A=Application, D=Data)';
COMMENT ON COLUMN ERRLOG.ERROR_SEVERITY IS
  'Error Severity (1=Info, 2=Warning, 3=Error, 4=Severe)';

-- Grants
GRANT SELECT, INSERT ON ERRLOG TO POSAPP;
GRANT SELECT ON ERRLOG TO POSRPT;

-- Cleanup Procedure
CREATE PROCEDURE ERRLOG_CLEANUP
  (IN RETENTION_DAYS INTEGER)
  LANGUAGE SQL
BEGIN
  DELETE FROM ERRLOG
  WHERE PROCESS_DATE < CURRENT DATE - RETENTION_DAYS DAYS;
END;  