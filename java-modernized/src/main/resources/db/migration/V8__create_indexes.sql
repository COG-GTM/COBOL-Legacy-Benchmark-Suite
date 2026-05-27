--====================================================================
-- INDEXES
-- Translated from db2-definitions.sql lines 67-77
-- and POSHIST.sql lines 60-68
--====================================================================

-- Portfolio Master indexes
CREATE INDEX idx_port_master_client
    ON portfolio_master (client_id, status);

-- Investment Positions indexes
CREATE INDEX idx_positions_date
    ON investment_positions (position_date, portfolio_id);

-- Transaction History indexes
CREATE INDEX idx_trans_hist_port
    ON transaction_history (portfolio_id, transaction_date);

CREATE INDEX idx_trans_hist_date
    ON transaction_history (transaction_date, portfolio_id);

-- Position History indexes (from POSHIST.sql)
CREATE INDEX idx_poshist_security
    ON position_history (security_id, trans_date);

CREATE INDEX idx_poshist_process
    ON position_history (process_date, program_id);

-- Error Log indexes (from ERRLOG.sql)
CREATE INDEX idx_errlog_severity
    ON error_log (process_date, error_severity DESC);

-- Audit Log indexes
CREATE INDEX idx_audit_portfolio
    ON audit_log (portfolio_id);

-- History Record indexes
CREATE INDEX idx_history_portfolio
    ON history_record (portfolio_id);
