package com.portfolio.audit;

import com.portfolio.entity.AuditAction;
import com.portfolio.entity.AuditLog;
import com.portfolio.entity.AuditStatus;
import com.portfolio.entity.AuditType;
import com.portfolio.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAudit(String systemId, String userId, String program, String terminal,
                         AuditType type, AuditAction action, AuditStatus status,
                         String portfolioId, String accountNo,
                         String beforeImage, String afterImage, String message) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setSystemId(systemId);
        auditLog.setUserId(userId);
        auditLog.setProgram(program);
        auditLog.setTerminal(terminal);
        auditLog.setType(type);
        auditLog.setAction(action);
        auditLog.setStatus(status);
        auditLog.setPortfolioId(portfolioId);
        auditLog.setAccountNo(accountNo);
        auditLog.setBeforeImage(truncate(beforeImage, 100));
        auditLog.setAfterImage(truncate(afterImage, 100));
        auditLog.setMessage(truncate(message, 100));

        auditLogRepository.save(auditLog);

        log.debug("Audit: {} {} {} - Portfolio: {} User: {}",
                type, action, status, portfolioId, userId);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
