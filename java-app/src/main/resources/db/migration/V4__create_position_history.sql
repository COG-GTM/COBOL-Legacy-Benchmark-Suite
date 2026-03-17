-- ============================================================================
-- V4: Position History Table
-- Replaces: VSAM POSHIST file (350-byte records,
--           key = Portfolio ID 8 + Position Date 8 + Investment ID 10)
-- Copybook: HISTREC.cpy
-- Record types: PT=Portfolio, PS=Position, TR=Transaction
-- Action codes: A=Add, C=Change, D=Delete
-- ============================================================================

CREATE TABLE position_history (
    portfolio_id      CHAR(8)         NOT NULL,
    history_date      DATE            NOT NULL,
    history_time      TIME            NOT NULL,
    sequence_no       VARCHAR(20)     NOT NULL,
    record_type       CHAR(2)         NOT NULL,
    action_code       CHAR(1)         NOT NULL,
    investment_id     CHAR(10),
    quantity          NUMERIC(18,4),
    cost_basis        NUMERIC(18,2),
    market_value      NUMERIC(18,2),
    before_image      TEXT,
    after_image       TEXT,
    reason_code       CHAR(4),
    process_date      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    process_user      VARCHAR(8)      NOT NULL,
    CONSTRAINT pk_position_history PRIMARY KEY (portfolio_id, history_date, history_time, sequence_no),
    CONSTRAINT chk_hist_record_type CHECK (record_type IN ('PT', 'PS', 'TR')),
    CONSTRAINT chk_hist_action_code CHECK (action_code IN ('A', 'C', 'D'))
);

-- Index for date-range queries on position history
CREATE INDEX idx_pos_hist_date ON position_history (history_date, portfolio_id);

-- Index for portfolio-based lookups
CREATE INDEX idx_pos_hist_portfolio ON position_history (portfolio_id, history_date);
