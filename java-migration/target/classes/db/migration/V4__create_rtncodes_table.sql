-- Return Codes Table (from RTNCODES.sql)
CREATE TABLE rtncodes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_timestamp TIMESTAMP NOT NULL,
    program_id      VARCHAR(8) NOT NULL,
    return_code     INT NOT NULL,
    highest_code    INT NOT NULL,
    status_code     CHAR(1) NOT NULL,
    message_text    VARCHAR(80)
);

CREATE INDEX idx_rtncodes_program ON rtncodes(program_id);
