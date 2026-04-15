--=====================================================================
-- Table Name:   RTNCODES
-- Description:  Return Code Logging Table
--
-- Records every return code set by batch programs via the RTNCDE00
-- service. Used by RTNANA00 to analyze patterns across jobs.
--
-- Column definitions:
--   TIMESTAMP    - When the return code was logged (part of PK)
--   PROGRAM_ID   - Program that set the code (part of PK)
--   RETURN_CODE  - Return code value (0/4/8/12/16)
--   HIGHEST_CODE - Highest code seen so far in this program run
--   STATUS_CODE  - S=Success, W=Warning, E=Error, F=Severe
--   MESSAGE_TEXT - Optional descriptive message
--=====================================================================
CREATE TABLE RTNCODES (
    TIMESTAMP       TIMESTAMP NOT NULL,
    PROGRAM_ID      CHAR(8) NOT NULL,
    RETURN_CODE     INTEGER NOT NULL,
    HIGHEST_CODE    INTEGER NOT NULL,
    STATUS_CODE     CHAR(1) NOT NULL,
    MESSAGE_TEXT    VARCHAR(80),
    PRIMARY KEY (TIMESTAMP, PROGRAM_ID)
);

-- Index for querying all codes by program over time
CREATE INDEX RTNCODES_PRG_IDX ON 
    RTNCODES (PROGRAM_ID, TIMESTAMP);

-- Index for filtering by status (e.g., find all errors)
CREATE INDEX RTNCODES_STS_IDX ON 
    RTNCODES (STATUS_CODE, TIMESTAMP);   