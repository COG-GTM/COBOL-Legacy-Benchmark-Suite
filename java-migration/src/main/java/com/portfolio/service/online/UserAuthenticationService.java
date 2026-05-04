package com.portfolio.service.online;

import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * User Authentication Service - migrated from COBOL SECMGR.cbl.
 * Replaces: EXEC CICS ASSIGN USERID -> SecurityContextHolder.getContext().getAuthentication()
 * DB2 authorization table checks -> Spring Security roles/authorities.
 */
@Service
public class UserAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationService.class);

    private final AuditService auditService;

    public UserAuthenticationService(AuditService auditService) {
        this.auditService = auditService;
    }

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "UNKNOWN";
    }

    public boolean validateUser(String userId) {
        String currentUser = getCurrentUserId();
        boolean valid = currentUser.equals(userId);

        if (!valid) {
            log.warn("User validation failed: expected={}, actual={}", userId, currentUser);
            auditService.logUserAction(userId, "SECMGR", "LOGIN", "FAIL",
                    "User validation failed");
        }

        return valid;
    }

    public boolean checkAuthorization(String userId, String resourceName, String accessType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }

        boolean authorized = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_" + accessType.toUpperCase()));

        if (!authorized) {
            log.warn("Access denied: user={}, resource={}, access={}", userId, resourceName, accessType);
        }

        return authorized;
    }

    public void logAccess(String userId, String resourceName, String accessType) {
        auditService.logUserAction(userId, "SECMGR", accessType, "SUCC",
                "Access to " + resourceName);
    }
}
