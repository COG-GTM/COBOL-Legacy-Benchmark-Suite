package com.portfolio.security;

import com.portfolio.domain.*;
import com.portfolio.repository.AuditLogRepository;
import java.time.LocalDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {
  private final AuditLogRepository auditLogRepository;

  public AuthenticationAuditListener(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @EventListener
  public void success(AuthenticationSuccessEvent authenticationSuccessEvent) {
    write(authenticationSuccessEvent.getAuthentication().getName(), AuditStatus.SUCCESS);
  }

  @EventListener
  public void failure(AbstractAuthenticationFailureEvent authenticationFailureEvent) {
    write(authenticationFailureEvent.getAuthentication().getName(), AuditStatus.FAILURE);
  }

  private void write(String userId, AuditStatus auditStatus) {
    AuditLog auditLog = new AuditLog();
    auditLog.setAudTimestamp(LocalDateTime.now());
    auditLog.setUserId(userId);
    auditLog.setAudType(AuditType.USER);
    auditLog.setAction(AuditAction.LOGIN);
    auditLog.setStatus(auditStatus);
    auditLogRepository.save(auditLog);
  }
}
