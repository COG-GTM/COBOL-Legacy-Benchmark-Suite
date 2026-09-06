package com.cog.portfolio.service;

import com.cog.portfolio.domain.AuditAction;
import com.cog.portfolio.domain.AuditLog;
import com.cog.portfolio.domain.AuditStatus;
import com.cog.portfolio.domain.AuditType;
import com.cog.portfolio.repository.AuditLogRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUDPROC.cbl: builds an AUDIT-RECORD (who/when/what, before/after image,
 * action, status, message) from the caller's request and persists it. The
 * sequential AUDFILE I/O is replaced by the AUDIT_LOG table.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String SYSTEM_ID = "PORTSYS";
    private static final int IMAGE_LENGTH = 100;

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public record AuditRequest(
            String program,
            AuditType type,
            AuditAction action,
            AuditStatus status,
            String portfolioId,
            String accountNo,
            String beforeImage,
            String afterImage,
            String message) {
    }

    /** Runs in its own transaction so the audit trail survives a business rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(AuditRequest request) {
        AuditLog entry = new AuditLog(LocalDateTime.now(), request.type(), request.action(), request.status());
        entry.setSystemId(SYSTEM_ID);
        entry.setUserId(currentUser());
        entry.setProgram(request.program());
        entry.setTerminal("WEB");
        entry.setPortfolioId(request.portfolioId());
        entry.setAccountNo(request.accountNo());
        entry.setBeforeImage(truncate(request.beforeImage()));
        entry.setAfterImage(truncate(request.afterImage()));
        entry.setMessage(truncate(request.message()));
        AuditLog saved = repository.save(entry);
        log.debug("Audit {} {} {} {}", request.program(), request.action(), request.status(), request.portfolioId());
        return saved;
    }

    public AuditLog recordSuccess(String program, AuditAction action, String portfolioId, String accountNo,
                                  String before, String after, String message) {
        return record(new AuditRequest(program, AuditType.TRANSACTION, action, AuditStatus.SUCCESS,
                portfolioId, accountNo, before, after, message));
    }

    public AuditLog recordFailure(String program, AuditAction action, String portfolioId, String accountNo,
                                  String message) {
        return record(new AuditRequest(program, AuditType.TRANSACTION, action, AuditStatus.FAILURE,
                portfolioId, accountNo, null, null, message));
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "BATCH";
        }
        String name = auth.getName();
        return name.length() > 8 ? name.substring(0, 8) : name;
    }

    /** AUD-BEFORE-IMAGE / AUD-AFTER-IMAGE / AUD-MESSAGE are PIC X(100). */
    static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > IMAGE_LENGTH ? value.substring(0, IMAGE_LENGTH) : value;
    }
}
