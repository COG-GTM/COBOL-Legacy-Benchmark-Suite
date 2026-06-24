package com.portfolio.service;

import com.portfolio.model.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Audit logging service.
 * Mirrors COBOL AUDPROC subroutine and the audit trail logic in:
 * - PORTMSTR.cbl paragraph 2100-LOG-PORTFOLIO-UPDATE
 * - PORTTRAN.cbl paragraph 2300-UPDATE-AUDIT-TRAIL
 * - PORTDEL.cbl paragraph 2300-WRITE-AUDIT
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Write an audit record.
     * Mirrors the CALL 'AUDPROC' USING AUDIT-RECORD pattern from COBOL.
     */
    @Transactional
    public void logAction(String auditType, String action, String status,
                          String portfolioId, String accountNo,
                          String beforeImage, String afterImage, String message) {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setSystemId("PORTMGMT");
        log.setUserId("SYSTEM");
        log.setProgram("JAVAMIG");
        log.setAuditType(auditType);
        log.setAction(action);
        log.setAuditStatus(status);
        log.setPortfolioId(portfolioId);
        log.setAccountNo(accountNo);
        log.setBeforeImage(beforeImage);
        log.setAfterImage(afterImage);
        log.setMessage(message);
        auditLogRepository.save(log);
    }
}
