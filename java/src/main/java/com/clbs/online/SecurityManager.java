package com.clbs.online;

import com.clbs.db2.AuthorizationRepository;
import com.clbs.domain.AuditRecord;
import com.clbs.common.AuditProcessor;
import org.springframework.stereotype.Service;

/**
 * SECMGR — security manager. The CICS ASSIGN USERID becomes the caller-supplied user id, the
 * AUTHFILE COUNT(*) becomes the authorization repository count, and the AUDITLOG INSERT becomes an
 * AUDPROC audit write.
 */
@Service
public class SecurityManager {

    public static final int OK = 0;
    public static final int DENIED = 8;
    public static final int FAILED = 12;

    public static final String ERR_VALIDATE = "User validation failed";
    public static final String ERR_CREDENTIALS = "Unable to obtain user credentials";
    public static final String ERR_DENIED = "Access denied";
    public static final String ERR_AUTH_FAILED = "Authorization check failed";
    public static final String ERR_AUDIT = "Audit logging failed";

    /**
     * SECURITY-REQUEST-AREA. {@code signedOnUser} carries what CICS ASSIGN USERID would return —
     * the user the session is actually signed on as — so P100-VALIDATE-USER can still compare it
     * with the requested SEC-USER-ID.
     */
    public record SecurityRequest(char requestType, String userId, String resource,
            String accessType, String terminalId, String signedOnUser) {
    }

    /** SEC-RESPONSE-CODE + SEC-ERROR-INFO. */
    public record SecurityResponse(int responseCode, String errorInfo) {
        public boolean ok() {
            return responseCode == OK;
        }
    }

    private final AuthorizationRepository authorizations;
    private final AuditProcessor auditProcessor;

    public SecurityManager(AuthorizationRepository authorizations, AuditProcessor auditProcessor) {
        this.authorizations = authorizations;
        this.auditProcessor = auditProcessor;
    }

    /** PROCEDURE DIVISION EVALUATE on SEC-REQUEST-TYPE. */
    public SecurityResponse execute(SecurityRequest request) {
        return switch (request.requestType()) {
            case 'V' -> validateUser(request);
            case 'A' -> checkAuthorization(request);
            case 'L' -> logAccess(request);
            default -> new SecurityResponse(OK, "");
        };
    }

    /** P100-VALIDATE-USER. */
    public SecurityResponse validateUser(SecurityRequest request) {
        String signedOn = request.signedOnUser();
        if (signedOn == null || signedOn.isBlank()) {
            return new SecurityResponse(FAILED, ERR_CREDENTIALS);
        }
        return signedOn.equals(request.userId())
                ? new SecurityResponse(OK, "")
                : new SecurityResponse(DENIED, ERR_VALIDATE);
    }

    /** P200-CHECK-AUTH. */
    public SecurityResponse checkAuthorization(SecurityRequest request) {
        try {
            long count = authorizations.countByUserIdAndResourceAndAccessType(
                    request.userId(), request.resource(), request.accessType());
            return count > 0 ? new SecurityResponse(OK, "")
                    : new SecurityResponse(DENIED, ERR_DENIED);
        } catch (RuntimeException e) {
            return new SecurityResponse(FAILED, ERR_AUTH_FAILED);
        }
    }

    /** P300-LOG-ACCESS. */
    public SecurityResponse logAccess(SecurityRequest request) {
        AuditRecord audit = new AuditRecord();
        audit.setUserId(request.userId());
        audit.setTerminalId(request.terminalId() == null ? "" : request.terminalId());
        audit.setProgramId(request.resource());
        audit.setType(AuditRecord.TYPE_USER_ACTION);
        audit.setAction(AuditRecord.ACTION_INQUIRE);
        audit.setMessage(request.accessType());
        return auditProcessor.write(audit) == 0
                ? new SecurityResponse(OK, "")
                : new SecurityResponse(FAILED, ERR_AUDIT);
    }
}
