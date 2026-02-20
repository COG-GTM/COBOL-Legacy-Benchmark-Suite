"""Database schema definitions - migrated from DB2 SQL DDL files.

Provides SQL DDL statements for creating database tables that mirror
the original DB2 for z/OS table definitions used by the COBOL programs.
Compatible with SQLite for local development and PostgreSQL for production.
"""

POSITION_HISTORY_DDL = """
CREATE TABLE IF NOT EXISTS position_history (
    account_no      VARCHAR(10)     NOT NULL,
    portfolio_id    VARCHAR(8)      NOT NULL,
    trans_date      VARCHAR(8)      NOT NULL,
    trans_time      VARCHAR(6)      NOT NULL,
    trans_type      VARCHAR(2)      NOT NULL,
    security_id     VARCHAR(8)      NOT NULL,
    quantity        DECIMAL(15,4)   NOT NULL DEFAULT 0,
    price           DECIMAL(15,4)   NOT NULL DEFAULT 0,
    amount          DECIMAL(15,2)   NOT NULL DEFAULT 0,
    fees            DECIMAL(11,2)   NOT NULL DEFAULT 0,
    total_amount    DECIMAL(15,2)   NOT NULL DEFAULT 0,
    cost_basis      DECIMAL(15,2)   NOT NULL DEFAULT 0,
    gain_loss       DECIMAL(15,2)   NOT NULL DEFAULT 0,
    process_date    VARCHAR(10)     NOT NULL,
    process_time    VARCHAR(8)      NOT NULL,
    program_id      VARCHAR(8)      NOT NULL,
    user_id         VARCHAR(8)      NOT NULL,
    audit_timestamp VARCHAR(26)     NOT NULL,
    PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time, security_id)
);

CREATE INDEX IF NOT EXISTS idx_poshist_portfolio
    ON position_history (portfolio_id, trans_date);

CREATE INDEX IF NOT EXISTS idx_poshist_security
    ON position_history (security_id, trans_date);
"""

ERROR_LOG_DDL = """
CREATE TABLE IF NOT EXISTS error_log (
    error_timestamp VARCHAR(26)     NOT NULL,
    error_source    VARCHAR(8)      NOT NULL,
    error_type      VARCHAR(12)     NOT NULL,
    error_severity  INTEGER         NOT NULL DEFAULT 0,
    error_code      VARCHAR(10)     NOT NULL,
    error_message   VARCHAR(256)    NOT NULL,
    error_program   VARCHAR(8)      NOT NULL,
    error_paragraph VARCHAR(30)     NOT NULL DEFAULT '',
    error_sqlcode   INTEGER         NOT NULL DEFAULT 0,
    error_sqlstate  VARCHAR(5)      NOT NULL DEFAULT '',
    error_data      VARCHAR(256)    NOT NULL DEFAULT '',
    PRIMARY KEY (error_timestamp, error_source, error_code)
);

CREATE INDEX IF NOT EXISTS idx_errlog_source
    ON error_log (error_source, error_timestamp);

CREATE INDEX IF NOT EXISTS idx_errlog_severity
    ON error_log (error_severity, error_timestamp);
"""

AUDIT_LOG_DDL = """
CREATE TABLE IF NOT EXISTS audit_log (
    audit_timestamp VARCHAR(26)     NOT NULL,
    system_id       VARCHAR(8)      NOT NULL,
    user_id         VARCHAR(8)      NOT NULL,
    program         VARCHAR(8)      NOT NULL,
    terminal        VARCHAR(8)      NOT NULL DEFAULT '',
    audit_type      VARCHAR(2)      NOT NULL,
    audit_action    VARCHAR(10)     NOT NULL,
    audit_status    VARCHAR(2)      NOT NULL,
    key_info        VARCHAR(50)     NOT NULL DEFAULT '',
    before_image    VARCHAR(500)    NOT NULL DEFAULT '',
    after_image     VARCHAR(500)    NOT NULL DEFAULT '',
    PRIMARY KEY (audit_timestamp, system_id, user_id, program)
);

CREATE INDEX IF NOT EXISTS idx_audit_user
    ON audit_log (user_id, audit_timestamp);

CREATE INDEX IF NOT EXISTS idx_audit_program
    ON audit_log (program, audit_timestamp);
"""

ALL_DDL = [POSITION_HISTORY_DDL, ERROR_LOG_DDL, AUDIT_LOG_DDL]


def create_all_tables(connection) -> None:
    """Create all database tables using a DB-API 2.0 connection."""
    cursor = connection.cursor()
    for ddl in ALL_DDL:
        for statement in ddl.strip().split(";"):
            statement = statement.strip()
            if statement:
                cursor.execute(statement)
    connection.commit()
    cursor.close()
