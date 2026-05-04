package com.portfolio.service.common;

import com.portfolio.domain.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Audit Service - migrated from COBOL AUDPROC.cbl.
 * Writes audit trail records to AUDITLOG table via JPA.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAudit(String systemId, String userId, String program, String terminal,
                         String auditType, String action, String status,
                         String portfolioId, String accountNo,
                         String beforeImage, String afterImage, String message) {
        try {
            AuditLog audit = new AuditLog();
            audit.setAuditTimestamp(LocalDateTime.now());
            audit.setSystemId(systemId != null ? systemId : "SYSTEM");
            audit.setUserId(userId != null ? userId : "UNKNOWN");
            audit.setProgram(program != null ? program : "UNKNOWN");
            audit.setTerminal(terminal);
            audit.setAuditType(auditType);
            audit.setAction(action);
            audit.setStatus(status);
            audit.setPortfolioId(portfolioId);
            audit.setAccountNo(accountNo);
            audit.setBeforeImage(beforeImage);
            audit.setAfterImage(afterImage);
            audit.setMessage(message);

            auditLogRepository.save(audit);
            log.debug("Audit logged: {} {} {} for portfolio {}", auditType, action, status, portfolioId);
        } catch (Exception e) {
            log.error("Failed to write audit record", e);
        }
    }

    @Transactional
    public void logTransaction(String userId, String program, String portfolioId,
                               String action, String status, String message) {
        logAudit("SYSTEM", userId, program, null, "TRAN", action, status,
                portfolioId, null, null, null, message);
    }

    @Transactional
    public void logUserAction(String userId, String program, String action,
                              String status, String message) {
        logAudit("SYSTEM", userId, program, null, "USER", action, status,
                null, null, null, null, message);
    }

    @Transactional
    public void logSystemEvent(String program, String action, String status, String message) {
        logAudit("SYSTEM", "SYSTEM", program, null, "SYST", action, status,
                null, null, null, null, message);
    }
}
