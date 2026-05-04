package com.portfolio.security;

import com.portfolio.audit.AuditService;
import com.portfolio.entity.AuditAction;
import com.portfolio.entity.AuditStatus;
import com.portfolio.entity.AuditType;
import com.portfolio.exception.AuthorizationException;
import com.portfolio.repository.AuthFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    private final AuthFileRepository authFileRepository;
    private final AuditService auditService;

    public SecurityService(AuthFileRepository authFileRepository, AuditService auditService) {
        this.authFileRepository = authFileRepository;
        this.auditService = auditService;
    }

    public boolean validateUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("User validation failed: empty user ID");
            return false;
        }
        log.debug("User validated: {}", userId);
        return true;
    }

    public boolean checkAuthorization(String userId, String resource, String accessType) {
        long count = authFileRepository.countByUserIdAndResourceAndAccessType(
                userId, resource, accessType);
        if (count == 0) {
            log.warn("Authorization denied: user={} resource={} accessType={}",
                    userId, resource, accessType);
            throw new AuthorizationException(
                    "User " + userId + " not authorized for " + accessType + " on " + resource);
        }
        log.debug("Authorization granted: user={} resource={} accessType={}",
                userId, resource, accessType);
        return true;
    }

    public void logAccess(String userId, String terminalId, String transId,
                          String program, String accessType) {
        AuditAction action = "LOGIN".equalsIgnoreCase(accessType)
                ? AuditAction.LOGIN
                : AuditAction.INQUIRE;

        auditService.logAudit(
                "ONLINE", userId, program, terminalId,
                AuditType.USER_ACTION, action, AuditStatus.SUCCESS,
                null, null, null, null,
                "Access: " + accessType + " Trans: " + transId);
    }
}
