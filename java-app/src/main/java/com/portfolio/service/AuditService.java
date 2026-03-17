package com.portfolio.service;

import com.portfolio.model.AuditRecord;
import com.portfolio.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Service.
 * Replaces: AUDPROC.cbl - Centralized audit trail management.
 * Methods: logAccess, logTransaction, logSystemEvent.
 * Persists to audit_log table (V6 migration).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Logs a user access event.
     * Replaces AUDPROC audit logging for user actions.
     */
    public void logAccess(String userId, String resource, String action) {
        AuditRecord record = new AuditRecord();
        record.setAuditTimestamp(LocalDateTime.now());
        record.setUserId(userId);
        record.setProgramName(resource);
        record.setAuditType("USER");
        record.setAction(action);
        record.setStatus("SUCC");
        record.setMessage("User " + userId + " performed " + action + " on " + resource);

        auditRepository.save(record);
        log.debug("Audit: user={}, resource={}, action={}", userId, resource, action);
    }

    /**
     * Logs a transaction event.
     * Replaces AUDPROC audit logging for transactions.
     */
    public void logTransaction(String transactionId, String action, String result,
                               String userId, String portfolioId) {
        AuditRecord record = new AuditRecord();
        record.setAuditTimestamp(LocalDateTime.now());
        record.setUserId(userId);
        record.setAuditType("TRAN");
        record.setAction(action);
        record.setStatus(result);
        record.setPortfolioId(portfolioId);
        record.setMessage("Transaction " + transactionId + ": " + action + " -> " + result);

        auditRepository.save(record);
        log.debug("Audit: transaction={}, action={}, result={}", transactionId, action, result);
    }

    /**
     * Logs a system event.
     * Replaces AUDPROC system event logging.
     */
    public void logSystemEvent(String action, String status, String message) {
        AuditRecord record = new AuditRecord();
        record.setAuditTimestamp(LocalDateTime.now());
        record.setUserId("SYSTEM");
        record.setSystemId("PORTMGMT");
        record.setAuditType("SYST");
        record.setAction(action);
        record.setStatus(status);
        record.setMessage(message);

        auditRepository.save(record);
        log.debug("Audit: system event={}, status={}", action, status);
    }

    /**
     * Retrieves audit records for a date range.
     */
    public List<AuditRecord> getAuditRecords(LocalDateTime start, LocalDateTime end) {
        return auditRepository.findByAuditTimestampBetween(start, end);
    }

    /**
     * Retrieves audit records for a user.
     */
    public List<AuditRecord> getAuditRecordsByUser(String userId) {
        return auditRepository.findByUserId(userId);
    }
}
