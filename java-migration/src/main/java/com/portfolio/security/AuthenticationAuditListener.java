package com.portfolio.security;
import com.portfolio.domain.*; import com.portfolio.repository.AuditLogRepository; import org.springframework.context.event.EventListener; import org.springframework.security.authentication.event.*; import org.springframework.stereotype.Component; import java.time.LocalDateTime;
@Component public class AuthenticationAuditListener {
 private final AuditLogRepository audits; public AuthenticationAuditListener(AuditLogRepository a){audits=a;}
 @EventListener public void success(AuthenticationSuccessEvent e){write(e.getAuthentication().getName(),AuditStatus.SUCCESS);}
 @EventListener public void failure(AbstractAuthenticationFailureEvent e){write(e.getAuthentication().getName(),AuditStatus.FAILURE);}
 private void write(String user,AuditStatus status){AuditLog a=new AuditLog();a.setAudTimestamp(LocalDateTime.now());a.setUserId(user);a.setAudType(AuditType.USER);a.setAction(AuditAction.LOGIN);a.setStatus(status);audits.save(a);}
}
