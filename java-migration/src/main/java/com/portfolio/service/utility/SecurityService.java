package com.portfolio.service.utility;

import com.portfolio.model.enums.AuditStatus;
import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    private final AuditService auditService;

    public SecurityService(AuditService auditService) {
        this.auditService = auditService;
    }

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "UNKNOWN";
    }

    public boolean validateUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    public boolean checkAuthorization(String resource, String accessType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }

        boolean authorized = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + accessType));

        logAccess(auth.getName(), resource, authorized);
        return authorized;
    }

    private void logAccess(String userId, String resource, boolean authorized) {
        AuditStatus status = authorized ? AuditStatus.SUCCESS : AuditStatus.FAILURE;
        auditService.logAccess(userId, resource, status);
    }
}
