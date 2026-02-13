-- Migration 001: Create PostgreSQL tables from DB2 DDL definitions
-- Source: src/database/db2/db2-definitions.sql, POSHIST.sql, ERRLOG.sql, RTNCODES.sql
-- Target: PostgreSQL 16+

BEGIN;

-- ====================================================================
-- PORTFOLIO MASTER TABLE
-- Source: db2-definitions.sql - PORTFOLIO_MASTER
-- ====================================================================
CREATE TABLE portfolio_master (
    portfolio_id      CHAR(8)         NOT NULL,
    account_type      CHAR(2)         NOT NULL,
    branch_id         CHAR(2)         NOT NULL,
    client_id         CHAR(10)        NOT NULL,
    portfolio_name    VARCHAR(50)     NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    risk_level        CHAR(1)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    open_date         DATE            NOT NULL,
    close_date        DATE,
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_portfolio_master PRIMARY KEY (portfolio_id),
    CONSTRAINT chk_portfolio_status CHECK (status IN ('A', 'C', 'S')),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('1', '2', '3', '4', '5'))
);

COMMENT ON TABLE portfolio_master IS 'Portfolio Master - migrated from DB2 PORTFOLIO_MASTER';
COMMENT ON COLUMN portfolio_master.status IS 'A=Active, C=Closed, S=Suspended';

CREATE INDEX idx_portfolio_master_client
    ON portfolio_master (client_id, status);

-- ====================================================================
-- INVESTMENT POSITIONS TABLE
-- Source: db2-definitions.sql - INVESTMENT_POSITIONS
-- ====================================================================
CREATE TABLE investment_positions (
    portfolio_id      CHAR(8)         NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    position_date     DATE            NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    cost_basis        DECIMAL(18,2)   NOT NULL,
    market_value      DECIMAL(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    last_maint_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_maint_user   VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_investment_positions PRIMARY KEY (portfolio_id, investment_id, position_date),
    CONSTRAINT fk_positions_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id)
);

COMMENT ON TABLE investment_positions IS 'Investment Positions - migrated from DB2 INVESTMENT_POSITIONS';

CREATE INDEX idx_positions_date
    ON investment_positions (position_date, portfolio_id);

-- ====================================================================
-- TRANSACTION HISTORY TABLE
-- Source: db2-definitions.sql - TRANSACTION_HISTORY
-- ====================================================================
CREATE TABLE transaction_history (
    transaction_id    CHAR(20)        NOT NULL,
    portfolio_id      CHAR(8)         NOT NULL,
    transaction_date  DATE            NOT NULL,
    transaction_time  TIME            NOT NULL,
    investment_id     CHAR(10)        NOT NULL,
    transaction_type  CHAR(2)         NOT NULL,
    quantity          DECIMAL(18,4)   NOT NULL,
    price             DECIMAL(18,4)   NOT NULL,
    amount            DECIMAL(18,2)   NOT NULL,
    currency_code     CHAR(3)         NOT NULL,
    status            CHAR(1)         NOT NULL,
    process_date      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_user      VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_transaction_history PRIMARY KEY (transaction_id),
    CONSTRAINT fk_trans_hist_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio_master(portfolio_id),
    CONSTRAINT chk_trans_type CHECK (transaction_type IN ('BU', 'SL', 'TR', 'FE')),
    CONSTRAINT chk_trans_status CHECK (status IN ('P', 'F', 'R'))
);

COMMENT ON TABLE transaction_history IS 'Transaction History - migrated from DB2 TRANSACTION_HISTORY';
COMMENT ON COLUMN transaction_history.transaction_id IS 'Format: YYYYMMDDHHMMSS + 6-digit sequence';
COMMENT ON COLUMN transaction_history.transaction_type IS 'BU=Buy, SL=Sell, TR=Transfer, FE=Fee';
COMMENT ON COLUMN transaction_history.status IS 'P=Processed, F=Failed, R=Reversed';

CREATE INDEX idx_trans_hist_port
    ON transaction_history (portfolio_id, transaction_date);

CREATE INDEX idx_trans_hist_date
    ON transaction_history (transaction_date, portfolio_id);

-- ====================================================================
-- POSITION HISTORY TABLE (Partitioned)
-- Source: POSHIST.sql
-- DB2 partitioned by TRANS_DATE quarterly; PostgreSQL declarative partitioning
-- ====================================================================
CREATE TABLE poshist (
    account_no        CHAR(8)         NOT NULL,
    portfolio_id      CHAR(10)        NOT NULL,
    trans_date        DATE            NOT NULL,
    trans_time        TIME            NOT NULL,
    trans_type        CHAR(2)         NOT NULL,
    security_id       CHAR(12)        NOT NULL,
    quantity          DECIMAL(15,3)   NOT NULL,
    price             DECIMAL(15,3)   NOT NULL,
    amount            DECIMAL(15,2)   NOT NULL,
    fees              DECIMAL(15,2)   NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)   NOT NULL,
    cost_basis        DECIMAL(15,2)   NOT NULL,
    gain_loss         DECIMAL(15,2)   NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    audit_timestamp   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_poshist PRIMARY KEY (account_no, portfolio_id, trans_date, trans_time)
) PARTITION BY RANGE (trans_date);

COMMENT ON TABLE poshist IS 'Position History Table - Stores all portfolio transaction history';
COMMENT ON COLUMN poshist.trans_type IS 'Transaction Type (BU=Buy, SL=Sell, TR=Transfer)';

CREATE TABLE poshist_q1_2024 PARTITION OF poshist
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE poshist_q2_2024 PARTITION OF poshist
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
CREATE TABLE poshist_q3_2024 PARTITION OF poshist
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');
CREATE TABLE poshist_q4_2024 PARTITION OF poshist
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
CREATE TABLE poshist_q1_2025 PARTITION OF poshist
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE poshist_q2_2025 PARTITION OF poshist
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE poshist_q3_2025 PARTITION OF poshist
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE poshist_q4_2025 PARTITION OF poshist
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE poshist_q1_2026 PARTITION OF poshist
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE poshist_q2_2026 PARTITION OF poshist
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');

CREATE INDEX idx_poshist_security
    ON poshist (security_id, trans_date);

CREATE INDEX idx_poshist_process
    ON poshist (process_date, program_id);

-- ====================================================================
-- ERROR LOG TABLE
-- Source: ERRLOG.sql
-- ====================================================================
CREATE TABLE errlog (
    error_timestamp   TIMESTAMP       NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    error_type        CHAR(1)         NOT NULL,
    error_severity    INTEGER         NOT NULL,
    error_code        CHAR(8)         NOT NULL,
    error_message     VARCHAR(200)    NOT NULL,
    process_date      DATE            NOT NULL,
    process_time      TIME            NOT NULL,
    user_id           CHAR(8)         NOT NULL,
    additional_info   VARCHAR(500),
    CONSTRAINT pk_errlog PRIMARY KEY (error_timestamp, program_id),
    CONSTRAINT chk_error_type CHECK (error_type IN ('S', 'A', 'D')),
    CONSTRAINT chk_error_severity CHECK (error_severity BETWEEN 1 AND 4)
);

COMMENT ON TABLE errlog IS 'Error Logging Table - Stores application errors and warnings';
COMMENT ON COLUMN errlog.error_type IS 'Error Type (S=System, A=Application, D=Data)';
COMMENT ON COLUMN errlog.error_severity IS 'Error Severity (1=Info, 2=Warning, 3=Error, 4=Severe)';

CREATE INDEX idx_errlog_date_severity
    ON errlog (process_date, error_severity DESC);

-- Cleanup function (migrated from DB2 stored procedure ERRLOG_CLEANUP)
CREATE OR REPLACE FUNCTION errlog_cleanup(retention_days INTEGER)
RETURNS VOID AS $$
BEGIN
    DELETE FROM errlog
    WHERE process_date < CURRENT_DATE - retention_days;
END;
$$ LANGUAGE plpgsql;

-- ====================================================================
-- RETURN CODES TABLE
-- Source: RTNCODES.sql
-- ====================================================================
CREATE TABLE return_codes (
    recorded_at       TIMESTAMP       NOT NULL,
    program_id        CHAR(8)         NOT NULL,
    return_code       INTEGER         NOT NULL,
    highest_code      INTEGER         NOT NULL,
    status_code       CHAR(1)         NOT NULL,
    message_text      VARCHAR(80),
    CONSTRAINT pk_return_codes PRIMARY KEY (recorded_at, program_id)
);

COMMENT ON TABLE return_codes IS 'Return Code Logging - migrated from DB2 RTNCODES';

CREATE INDEX idx_return_codes_program
    ON return_codes (program_id, recorded_at);

CREATE INDEX idx_return_codes_status
    ON return_codes (status_code, recorded_at);

-- ====================================================================
-- VIEWS
-- Source: db2-definitions.sql
-- DB2 CURRENT DATE -> PostgreSQL CURRENT_DATE
-- ====================================================================
CREATE VIEW active_portfolios AS
    SELECT *
    FROM portfolio_master
    WHERE status = 'A'
    AND (close_date IS NULL OR close_date > CURRENT_DATE);

CREATE VIEW current_positions AS
    SELECT p.*, pm.portfolio_name, pm.client_id
    FROM investment_positions p
    JOIN portfolio_master pm ON p.portfolio_id = pm.portfolio_id
    WHERE p.position_date = CURRENT_DATE - INTERVAL '1 day';

COMMIT;
