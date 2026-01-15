--=====================================================================
-- AUTHFILE Table Definition and Enhancement (Phase 1 - Modernization)
-- Creates AUTHFILE table with modern authentication fields
-- Version: 1.0
-- Date: 2024
--=====================================================================

--====================================================================
-- AUTHFILE TABLE
-- Source: src/programs/online/SECMGR.cbl
-- Purpose: User authorization and access control
--====================================================================

CREATE TABLESPACE AUTHFILE
  IN POSMVP
  USING STOGROUP POSMVPSG
  PRIQTY 720
  SECQTY 360
  SEGSIZE 32
  COMPRESS YES;

CREATE TABLE AUTHFILE (
    -- Primary Key
    AUTH_ID           INTEGER         NOT NULL GENERATED ALWAYS AS IDENTITY,
    
    -- User Identification (from SECMGR.cbl SEC-USER-ID)
    USER_ID           VARCHAR(8)      NOT NULL,
    
    -- Resource Access (from SECMGR.cbl SEC-RESOURCE-NAME)
    RESOURCE          VARCHAR(50)     NOT NULL,
    
    -- Access Type (from SECMGR.cbl SEC-ACCESS-TYPE)
    ACCESS_TYPE       VARCHAR(20)     NOT NULL,
    
    -- Modern Authentication Fields
    ROLE_BASED_ACCESS SMALLINT        DEFAULT 0,
    
    -- API Security
    API_KEY_HASH      VARCHAR(256),
    
    -- Token Management
    TOKEN_EXPIRY      TIMESTAMP,
    
    -- Multi-Factor Authentication
    MFA_ENABLED       SMALLINT        DEFAULT 0,
    MFA_TYPE          VARCHAR(20),
    MFA_SECRET_HASH   VARCHAR(256),
    
    -- OAuth/OIDC Support
    OAUTH_PROVIDER    VARCHAR(50),
    OAUTH_SUBJECT_ID  VARCHAR(100),
    
    -- Session Management
    MAX_SESSIONS      INTEGER         DEFAULT 5,
    SESSION_TIMEOUT   INTEGER         DEFAULT 3600,
    
    -- Password Policy
    PASSWORD_HASH     VARCHAR(256),
    PASSWORD_SALT     VARCHAR(64),
    PASSWORD_EXPIRES  DATE,
    FAILED_ATTEMPTS   INTEGER         DEFAULT 0,
    LOCKED_UNTIL      TIMESTAMP,
    
    -- Role-Based Access Control
    ROLE_NAME         VARCHAR(50),
    ROLE_LEVEL        INTEGER         DEFAULT 0,
    
    -- Audit Fields
    CREATED_DATE      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY        VARCHAR(50)     NOT NULL,
    LAST_MODIFIED     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_BY       VARCHAR(50),
    LAST_LOGIN        TIMESTAMP,
    
    -- Status
    STATUS            CHAR(1)         NOT NULL DEFAULT 'A',
    
    -- Constraints
    CONSTRAINT PK_AUTHFILE 
        PRIMARY KEY (AUTH_ID),
    CONSTRAINT UK_AUTHFILE_USER_RESOURCE
        UNIQUE (USER_ID, RESOURCE, ACCESS_TYPE)
) IN POSMVP.AUTHFILE;

--====================================================================
-- INDEXES
--====================================================================

-- User lookup index
CREATE INDEX AUTHFILE_IX1
  ON AUTHFILE
  (USER_ID ASC,
   STATUS ASC);

-- Resource access lookup
CREATE INDEX AUTHFILE_IX2
  ON AUTHFILE
  (RESOURCE ASC,
   ACCESS_TYPE ASC);

-- Role-based lookup
CREATE INDEX AUTHFILE_IX3
  ON AUTHFILE
  (ROLE_NAME ASC,
   ROLE_LEVEL DESC);

-- API key lookup
CREATE INDEX AUTHFILE_IX4
  ON AUTHFILE
  (API_KEY_HASH ASC)
  WHERE API_KEY_HASH IS NOT NULL;

-- OAuth subject lookup
CREATE INDEX AUTHFILE_IX5
  ON AUTHFILE
  (OAUTH_PROVIDER ASC,
   OAUTH_SUBJECT_ID ASC)
  WHERE OAUTH_PROVIDER IS NOT NULL;

-- Token expiry monitoring
CREATE INDEX AUTHFILE_IX6
  ON AUTHFILE
  (TOKEN_EXPIRY ASC)
  WHERE TOKEN_EXPIRY IS NOT NULL;

--====================================================================
-- TABLE COMMENTS
--====================================================================

COMMENT ON TABLE AUTHFILE IS
  'Authorization File - User access control and authentication management';

COMMENT ON COLUMN AUTHFILE.USER_ID IS
  'User identifier (from SECMGR SEC-USER-ID)';
COMMENT ON COLUMN AUTHFILE.RESOURCE IS
  'Resource name for access control (from SECMGR SEC-RESOURCE-NAME)';
COMMENT ON COLUMN AUTHFILE.ACCESS_TYPE IS
  'Type of access: READ, WRITE, EXECUTE, ADMIN (from SECMGR SEC-ACCESS-TYPE)';
COMMENT ON COLUMN AUTHFILE.ROLE_BASED_ACCESS IS
  'Flag indicating role-based access (1=enabled, 0=disabled)';
COMMENT ON COLUMN AUTHFILE.API_KEY_HASH IS
  'SHA-256 hash of API key for service authentication';
COMMENT ON COLUMN AUTHFILE.TOKEN_EXPIRY IS
  'Expiration timestamp for authentication tokens';
COMMENT ON COLUMN AUTHFILE.MFA_ENABLED IS
  'Multi-factor authentication flag (1=enabled, 0=disabled)';
COMMENT ON COLUMN AUTHFILE.MFA_TYPE IS
  'MFA type: TOTP, SMS, EMAIL, HARDWARE_TOKEN';
COMMENT ON COLUMN AUTHFILE.OAUTH_PROVIDER IS
  'OAuth/OIDC provider name for federated authentication';
COMMENT ON COLUMN AUTHFILE.ROLE_NAME IS
  'Role name for RBAC: ADMIN, MANAGER, ANALYST, VIEWER';
COMMENT ON COLUMN AUTHFILE.ROLE_LEVEL IS
  'Numeric role level for hierarchical permissions';

--====================================================================
-- CHECK CONSTRAINTS
--====================================================================

ALTER TABLE AUTHFILE
  ADD CONSTRAINT CHK_AUTHFILE_STATUS
  CHECK (STATUS IN ('A', 'I', 'L', 'D'));

ALTER TABLE AUTHFILE
  ADD CONSTRAINT CHK_AUTHFILE_ACCESS_TYPE
  CHECK (ACCESS_TYPE IN ('READ', 'WRITE', 'EXECUTE', 'ADMIN', 'DELETE', 'ALL'));

ALTER TABLE AUTHFILE
  ADD CONSTRAINT CHK_AUTHFILE_MFA_TYPE
  CHECK (MFA_TYPE IS NULL OR MFA_TYPE IN ('TOTP', 'SMS', 'EMAIL', 'HARDWARE_TOKEN', 'PUSH'));

ALTER TABLE AUTHFILE
  ADD CONSTRAINT CHK_AUTHFILE_ROLE_LEVEL
  CHECK (ROLE_LEVEL BETWEEN 0 AND 100);

ALTER TABLE AUTHFILE
  ADD CONSTRAINT CHK_AUTHFILE_FAILED_ATTEMPTS
  CHECK (FAILED_ATTEMPTS >= 0);

--====================================================================
-- STORED PROCEDURES
--====================================================================

-- Procedure to check user authorization (modernized from SECMGR P200-CHECK-AUTH)
CREATE PROCEDURE CHECK_USER_AUTHORIZATION
  (IN P_USER_ID VARCHAR(8),
   IN P_RESOURCE VARCHAR(50),
   IN P_ACCESS_TYPE VARCHAR(20),
   OUT P_AUTHORIZED SMALLINT,
   OUT P_ERROR_MSG VARCHAR(100))
  LANGUAGE SQL
BEGIN
  DECLARE V_COUNT INTEGER;
  DECLARE V_STATUS CHAR(1);
  DECLARE V_LOCKED_UNTIL TIMESTAMP;
  
  SET P_AUTHORIZED = 0;
  SET P_ERROR_MSG = '';
  
  -- Check if user exists and is active
  SELECT STATUS, LOCKED_UNTIL INTO V_STATUS, V_LOCKED_UNTIL
  FROM AUTHFILE
  WHERE USER_ID = P_USER_ID
    AND RESOURCE = P_RESOURCE
    AND ACCESS_TYPE = P_ACCESS_TYPE
  FETCH FIRST 1 ROW ONLY;
  
  IF V_STATUS IS NULL THEN
    SET P_ERROR_MSG = 'Access denied - no authorization record';
  ELSEIF V_STATUS = 'L' THEN
    SET P_ERROR_MSG = 'Account locked';
  ELSEIF V_STATUS = 'I' THEN
    SET P_ERROR_MSG = 'Account inactive';
  ELSEIF V_LOCKED_UNTIL IS NOT NULL AND V_LOCKED_UNTIL > CURRENT_TIMESTAMP THEN
    SET P_ERROR_MSG = 'Account temporarily locked';
  ELSE
    SET P_AUTHORIZED = 1;
  END IF;
END;

-- Procedure to record failed login attempt
CREATE PROCEDURE RECORD_FAILED_LOGIN
  (IN P_USER_ID VARCHAR(8),
   IN P_MAX_ATTEMPTS INTEGER)
  LANGUAGE SQL
BEGIN
  UPDATE AUTHFILE
  SET FAILED_ATTEMPTS = FAILED_ATTEMPTS + 1,
      LOCKED_UNTIL = CASE 
        WHEN FAILED_ATTEMPTS + 1 >= P_MAX_ATTEMPTS 
        THEN CURRENT_TIMESTAMP + 30 MINUTES
        ELSE LOCKED_UNTIL
      END,
      STATUS = CASE 
        WHEN FAILED_ATTEMPTS + 1 >= P_MAX_ATTEMPTS 
        THEN 'L'
        ELSE STATUS
      END
  WHERE USER_ID = P_USER_ID;
END;

-- Procedure to reset failed attempts on successful login
CREATE PROCEDURE RECORD_SUCCESSFUL_LOGIN
  (IN P_USER_ID VARCHAR(8))
  LANGUAGE SQL
BEGIN
  UPDATE AUTHFILE
  SET FAILED_ATTEMPTS = 0,
      LOCKED_UNTIL = NULL,
      LAST_LOGIN = CURRENT_TIMESTAMP,
      STATUS = 'A'
  WHERE USER_ID = P_USER_ID
    AND STATUS IN ('A', 'L');
END;

-- Procedure to validate API key
CREATE PROCEDURE VALIDATE_API_KEY
  (IN P_API_KEY_HASH VARCHAR(256),
   OUT P_USER_ID VARCHAR(8),
   OUT P_VALID SMALLINT)
  LANGUAGE SQL
BEGIN
  SET P_VALID = 0;
  SET P_USER_ID = '';
  
  SELECT USER_ID INTO P_USER_ID
  FROM AUTHFILE
  WHERE API_KEY_HASH = P_API_KEY_HASH
    AND STATUS = 'A'
    AND (TOKEN_EXPIRY IS NULL OR TOKEN_EXPIRY > CURRENT_TIMESTAMP)
  FETCH FIRST 1 ROW ONLY;
  
  IF P_USER_ID <> '' THEN
    SET P_VALID = 1;
  END IF;
END;

--====================================================================
-- GRANTS
--====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON AUTHFILE TO POSAPP;
GRANT SELECT ON AUTHFILE TO POSRPT;
GRANT EXECUTE ON PROCEDURE CHECK_USER_AUTHORIZATION TO POSAPP;
GRANT EXECUTE ON PROCEDURE RECORD_FAILED_LOGIN TO POSAPP;
GRANT EXECUTE ON PROCEDURE RECORD_SUCCESSFUL_LOGIN TO POSAPP;
GRANT EXECUTE ON PROCEDURE VALIDATE_API_KEY TO POSAPP;

--====================================================================
-- NOTES:
--====================================================================
-- 1. ROLE_BASED_ACCESS and MFA_ENABLED use SMALLINT (0/1) for DB2 compatibility
-- 2. Password and API key fields store hashes, never plaintext
-- 3. OAuth fields support federated identity providers
-- 4. FAILED_ATTEMPTS and LOCKED_UNTIL implement account lockout policy
-- 5. Procedures modernize SECMGR.cbl authorization logic
-- 6. Status codes: A=Active, I=Inactive, L=Locked, D=Deleted
--====================================================================
