package com.portfolio.service;

import com.portfolio.entity.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Audit service - replaces SECMGR P300-LOG-ACCESS.
 * Source: src/programs/online/SECMGR.cbl
 *
 * Logs security events to the audit_log table, replacing the COBOL
 * INSERT INTO AUDITLOG statement.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAccess(String userId, String resourceName, String accessType, String action, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAuditTimestamp(LocalDateTime.now());
            auditLog.setUserId(userId);
            auditLog.setResourceName(resourceName);
            auditLog.setAccessType(accessType);
            auditLog.setAction(action);
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log entry: {}", e.getMessage());
        }
    }

    @Transactional
    public void logLogin(String userId, String ipAddress, boolean success) {
        logAccess(userId, "AUTH", "LOGIN", success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
                success ? "User authenticated successfully" : "Authentication failed", ipAddress);
    }

    @Transactional
    public void logPortfolioAccess(String userId, String portfolioId, String ipAddress) {
        logAccess(userId, "PORTFOLIO", "READ", "PORTFOLIO_INQUIRY",
                "Accessed portfolio: " + portfolioId, ipAddress);
    }
}
