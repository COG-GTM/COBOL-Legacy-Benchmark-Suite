package com.portfolio.service;

import com.portfolio.entity.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.util.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuditProcessor {

    private static final Logger log = LoggerFactory.getLogger(AuditProcessor.class);
    private final AuditLogRepository auditLogRepository;

    public AuditProcessor(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAudit(String userId, String programName, String auditType,
                         String action, String status, String portfolioId,
                         String accountNo, String message) {
        AuditLog audit = new AuditLog();
        audit.setAuditTimestamp(LocalDateTime.now());
        audit.setSystemId("PORTMGMT");
        audit.setUserId(userId != null ? userId : "SYSTEM");
        audit.setProgramName(programName);
        audit.setTerminalId("WEB");
        audit.setAuditType(auditType);
        audit.setAuditAction(action);
        audit.setAuditStatus(status);
        audit.setPortfolioId(portfolioId);
        audit.setAccountNo(accountNo);
        audit.setMessage(message);
        auditLogRepository.save(audit);
        log.debug("Audit logged: {} {} {} for portfolio {}", auditType, action, status, portfolioId);
    }

    @Transactional
    public void logTransaction(String userId, String portfolioId, String action, String message) {
        logAudit(userId, "PORTTRAN", CommonConstants.AUDIT_TYPE_TRANSACTION,
                action, CommonConstants.AUDIT_STATUS_SUCCESS, portfolioId, null, message);
    }

    @Transactional
    public void logUserAction(String userId, String action, String message) {
        logAudit(userId, "SECMGR", CommonConstants.AUDIT_TYPE_USER,
                action, CommonConstants.AUDIT_STATUS_SUCCESS, null, null, message);
    }

    @Transactional
    public void logSystemEvent(String programName, String action, String message) {
        logAudit("SYSTEM", programName, CommonConstants.AUDIT_TYPE_SYSTEM,
                action, CommonConstants.AUDIT_STATUS_SUCCESS, null, null, message);
    }

    @Transactional
    public void logError(String userId, String programName, String portfolioId, String message) {
        logAudit(userId, programName, CommonConstants.AUDIT_TYPE_SYSTEM,
                "ERROR", CommonConstants.AUDIT_STATUS_FAILURE, portfolioId, null, message);
    }
}
