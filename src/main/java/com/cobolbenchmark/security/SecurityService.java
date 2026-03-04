package com.cobolbenchmark.security;

import com.cobolbenchmark.common.SecurityAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Security Service - migrated from SECMGR.cbl.
 * Three operations: VALIDATE (SEC-VALIDATE), AUTHORIZE (SEC-AUTHORIZE), AUDIT (SEC-AUDIT).
 * Replaces COMMAREA-based request/response with method parameters.
 */
@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private final JdbcTemplate jdbcTemplate;

    public SecurityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Validate user credentials - replaces SEC-VALIDATE operation.
     * From SECMGR.cbl: P200-VALIDATE-USER paragraph.
     */
    public boolean validateUser(String userId, String password) {
        logger.debug("Validating user: {}", userId);
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AUTHFILE WHERE USER_ID = ? AND PASSWORD_HASH = ? AND STATUS = 'A'",
                Integer.class, userId, password
            );
            boolean valid = count != null && count > 0;
            if (!valid) {
                logger.warn("User validation failed for: {}", userId);
            }
            return valid;
        } catch (Exception e) {
            logger.error("Error during user validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Authorize resource access - replaces SEC-AUTHORIZE operation.
     * From SECMGR.cbl: P300-AUTHORIZE-ACCESS paragraph.
     */
    public boolean authorizeAccess(String userId, String resourceId, String resourceType, String accessLevel) {
        logger.debug("Authorizing user {} for resource {} type {} level {}",
                userId, resourceId, resourceType, accessLevel);
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AUTHFILE WHERE USER_ID = ? AND STATUS = 'A'",
                Integer.class, userId
            );
            boolean authorized = count != null && count > 0;
            if (!authorized) {
                throw new SecurityAuthException("AUTHORIZE", userId);
            }
            return true;
        } catch (SecurityAuthException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error during authorization: {}", e.getMessage());
            throw new SecurityAuthException("AUTHORIZE", userId);
        }
    }

    /**
     * Audit log entry - replaces SEC-AUDIT operation.
     * From SECMGR.cbl: P400-AUDIT-LOG paragraph.
     */
    public void auditLog(String userId, String action, String detail) {
        logger.info("Security audit - User: {} Action: {} Detail: {}", userId, action, detail);
        try {
            jdbcTemplate.update(
                "INSERT INTO AUDITLOG (USER_ID, ACTION, DETAIL, AUDIT_TIMESTAMP) VALUES (?, ?, ?, ?)",
                userId, action, detail, Timestamp.from(Instant.now())
            );
        } catch (Exception e) {
            // Audit logging failure should not stop processing
            logger.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Perform full security check: validate → authorize → audit.
     * From INQONLN.cbl: P050-SECURITY-CHECK paragraph.
     * Three chained EXEC CICS LINK PROGRAM('SECMGR') calls.
     */
    public void performSecurityCheck(String userId, String password, String resourceId, String resourceType) {
        // Step 1: Validate user
        if (!validateUser(userId, password)) {
            auditLog(userId, "VALIDATE_FAILED", "Authentication failed");
            throw new SecurityAuthException("VALIDATE", userId);
        }

        // Step 2: Authorize access
        authorizeAccess(userId, resourceId, resourceType, "READ");

        // Step 3: Audit successful access
        auditLog(userId, "ACCESS_GRANTED", "Resource: " + resourceId + " Type: " + resourceType);
    }
}
