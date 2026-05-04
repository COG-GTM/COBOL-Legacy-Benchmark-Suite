package com.coggtm.portfolio.service;

import com.coggtm.portfolio.domain.AuditRecord;

/**
 * Audit logging — maps to AUDPROC + AUDITLOG.cpy.
 *
 * <p>COBOL source: {@code src/programs/common/AUDPROC.cbl}</p>
 */
public interface AuditService {

    AuditRecord logTransaction(String portfolioId, String action, String beforeImage, String afterImage);

    AuditRecord logUserAction(String userId, String action, String message);

    AuditRecord logSystemEvent(String systemId, String action, String message);
}
