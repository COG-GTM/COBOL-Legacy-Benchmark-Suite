-- Flyway Migration V4: Error Log Table
-- Source: src/database/db2/ERRLOG.sql
-- COBOL Copybook: DBTBLS.cpy (ERRLOG-RECORD)

CREATE TABLE errlog (
    error_timestamp   TIMESTAMP       NOT NULL,
    program_id        VARCHAR(8)         NOT NULL,
    error_type        VARCHAR(1)         NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        VARCHAR(8)         NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      VARCHAR(8)      NOT NULL,
    user_id           VARCHAR(8)         NOT NULL,
    additional_info   VARCHAR(500),
    PRIMARY KEY (error_timestamp, program_id)
);

COMMENT ON TABLE errlog IS 'Error Logging Table - migrated from DB2 ERRLOG';
COMMENT ON COLUMN errlog.error_type IS 'S=System, A=Application, D=Data (level-88 EL-TYPE-SYSTEM/APP/DATA)';
COMMENT ON COLUMN errlog.error_severity IS '1=Info, 2=Warning, 3=Error, 4=Severe (level-88 EL-SEV-INFO/WARN/ERROR/SEVERE)';
