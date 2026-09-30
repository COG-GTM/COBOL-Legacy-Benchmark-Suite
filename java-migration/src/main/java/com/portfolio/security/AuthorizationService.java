package com.portfolio.security;

import com.portfolio.domain.AuditAction;
import com.portfolio.domain.AuditLog;
import com.portfolio.domain.AuditStatus;
import com.portfolio.domain.AuditType;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.repository.AuthEntryRepository;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
  private final AuthEntryRepository authEntryRepository;
  private final AuditLogRepository auditLogRepository;

  public AuthorizationService(
      AuthEntryRepository authEntryRepository, AuditLogRepository auditLogRepository) {
    this.authEntryRepository = authEntryRepository;
    this.auditLogRepository = auditLogRepository;
  }

  public boolean isAuthorized(String userId, String resource, String accessType) {
    return authEntryRepository.countByUserIdAndResourceAndAccessType(userId, resource, accessType)
        > 0;
  }

  public void requireAccess(String userId, String resource, String accessType) {
    boolean authorized = isAuthorized(userId, resource, accessType);
    logAccess(userId, resource, accessType, authorized);
    if (!authorized) {
      throw new AccessDeniedException("Access denied");
    }
  }

  public void logAccess(String userId, String resource, String accessType, boolean success) {
    AuditLog auditLog = new AuditLog();
    auditLog.setAudTimestamp(LocalDateTime.now());
    auditLog.setUserId(userId);
    auditLog.setProgram(resource);
    auditLog.setAudType(AuditType.USER);
    auditLog.setAction(
        "READ".equals(accessType)
            ? AuditAction.INQUIRE
            : ("LOGIN".equals(accessType) ? AuditAction.LOGIN : AuditAction.UPDATE));
    auditLog.setStatus(success ? AuditStatus.SUCCESS : AuditStatus.FAILURE);
    auditLog.setAccessType(accessType);
    auditLogRepository.save(auditLog);
  }
}
