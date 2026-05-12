-- Error Log Table (from ERRLOG.sql)
CREATE TABLE errlog (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_timestamp TIMESTAMP NOT NULL,
    program_id      VARCHAR(8) NOT NULL,
    error_type      CHAR(1) NOT NULL,
    error_severity  INT NOT NULL,
    error_code      VARCHAR(8) NOT NULL,
    error_message   VARCHAR(200) NOT NULL,
    process_date    DATE NOT NULL,
    process_time    TIME NOT NULL,
    user_id         VARCHAR(8) NOT NULL,
    additional_info VARCHAR(500)
);

CREATE INDEX idx_errlog_severity ON errlog(error_severity);
CREATE INDEX idx_errlog_date ON errlog(process_date, error_severity);
