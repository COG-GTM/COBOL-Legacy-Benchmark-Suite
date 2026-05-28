-- =====================================================================
-- V5: Indexes matching VSAM key structures and DB2 indexes
-- Reference: db2-definitions.sql, POSHIST.sql, ERRLOG.sql
-- =====================================================================

-- Portfolio indexes (from IDX_PORT_MASTER_CLIENT)
CREATE INDEX idx_portfolio_status ON portfolio (status);
CREATE INDEX idx_portfolio_client_type ON portfolio (client_type, status);

-- Position indexes (from IDX_POSITIONS_DATE)
CREATE INDEX idx_position_date ON position (pos_date, portfolio_id);

-- TransactionRecord indexes (from IDX_TRANS_HIST_PORT, IDX_TRANS_HIST_DATE)
CREATE INDEX idx_trn_portfolio_date ON transaction_record (portfolio_id, trn_date);
CREATE INDEX idx_trn_date_portfolio ON transaction_record (trn_date, portfolio_id);

-- PositionHistory indexes (from POSHIST_IX1, POSHIST_IX2)
CREATE INDEX idx_poshist_account_portfolio ON position_history (account_no, portfolio_id);
CREATE INDEX idx_poshist_security_date ON position_history (security_id, trans_date);
CREATE INDEX idx_poshist_process_program ON position_history (process_date, program_id);

-- HistoryRecord indexes
CREATE INDEX idx_hist_portfolio_date ON history_record (portfolio_id, hist_date);

-- AuditRecord indexes
CREATE INDEX idx_audit_portfolio ON audit_record (portfolio_id);
CREATE INDEX idx_audit_timestamp ON audit_record (timestamp);

-- ErrorLog indexes (from ERRLOG_IX1)
CREATE INDEX idx_errlog_program_date ON error_log (program_id, process_date);
CREATE INDEX idx_errlog_date_severity ON error_log (process_date, error_severity);

-- BatchControlRecord indexes
CREATE INDEX idx_batchctl_job_date ON batch_control_record (job_name, process_date);
CREATE INDEX idx_batchctl_status ON batch_control_record (status);

-- ProcessSequenceRecord indexes
CREATE INDEX idx_procseq_type ON process_sequence_record (type, start_time);

-- CheckpointControl indexes
CREATE INDEX idx_ckpt_program_date ON checkpoint_control (program_id, run_date);
