package com.portfolio.service;

import com.portfolio.model.AuditRecord;
import com.portfolio.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Security Manager Service.
 * Replaces: SECMGR.cbl (lines 42-54) - three functions matching EVALUATE TRUE block:
 * - SEC-VALIDATE -> validateUser()
 * - SEC-AUTHORIZE -> checkAuthorization()
 * - SEC-AUDIT -> logAccess()
 *
 * Integrates with Spring Security's authentication context.
 */
@Service
public class SecurityManagerService {

    private static final Logger log = LoggerFactory.getLogger(SecurityManagerService.class);

    private final AuditRepository auditRepository;

    public SecurityManagerService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Validates the current user.
     * Replaces: SECMGR.cbl P100-VALIDATE-USER (lines 56-76).
     * In the COBOL version, this calls EXEC CICS ASSIGN USERID.
     * In Spring, we validate via SecurityContext.
     *
     * @return the authenticated username, or null if not authenticated
     */
    public String validateUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            log.debug("User validated: {}", auth.getName());
            return auth.getName();
        }
        log.warn("User validation failed - no authenticated user");
        return null;
    }

    /**
     * Checks if the current user has authorization for a resource and access type.
     * Replaces: SECMGR.cbl P200-CHECK-AUTH (lines 78-103).
     * The COBOL version queries DB2 AUTHFILE table.
     * In Spring, we check via Security's role-based authorization.
     *
     * @param resourceName the resource being accessed
     * @param accessType   the type of access (READ, WRITE, etc.)
     * @return true if authorized
     */
    public boolean checkAuthorization(String resourceName, String accessType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        boolean hasAdminRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean hasUserRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));

        // ADMIN has full access; USER has read-only access
        if (hasAdminRole) {
            return true;
        }
        if (hasUserRole && "READ".equalsIgnoreCase(accessType)) {
            return true;
        }

        log.warn("Authorization denied for user {} on resource {} with access {}",
                auth.getName(), resourceName, accessType);
        return false;
    }

    /**
     * Logs access to a resource.
     * Replaces: SECMGR.cbl P300-LOG-ACCESS (lines 105-135).
     * The COBOL version inserts into DB2 AUDITLOG table.
     *
     * @param resourceName the resource accessed
     * @param accessType   the type of access
     */
    public void logAccess(String resourceName, String accessType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null) ? auth.getName() : "UNKNOWN";

        AuditRecord record = new AuditRecord();
        record.setAuditTimestamp(LocalDateTime.now());
        record.setUserId(userId);
        record.setProgramName(resourceName);
        record.setAuditType("USER");
        record.setAction(accessType);
        record.setStatus("SUCC");
        record.setMessage("Access logged for " + resourceName);

        try {
            auditRepository.save(record);
            log.debug("Access logged: user={}, resource={}, access={}",
                    userId, resourceName, accessType);
        } catch (Exception e) {
            // Matches SECMGR behavior: log failure but don't propagate
            log.error("Audit logging failed for user {} resource {}: {}",
                    userId, resourceName, e.getMessage());
        }
    }
}
