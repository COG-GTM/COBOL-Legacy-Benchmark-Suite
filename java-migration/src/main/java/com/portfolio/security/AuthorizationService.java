package com.portfolio.security;
import com.portfolio.domain.*; import com.portfolio.repository.*; import org.springframework.security.access.AccessDeniedException; import org.springframework.stereotype.Service; import java.time.LocalDateTime;
@Service public class AuthorizationService {
 private final AuthEntryRepository entries; private final AuditLogRepository audits; public AuthorizationService(AuthEntryRepository e,AuditLogRepository a){entries=e;audits=a;}
 public boolean isAuthorized(String userId,String resource,String accessType){return entries.countByUserIdAndResourceAndAccessType(userId,resource,accessType)>0;}
 public void requireAccess(String userId,String resource,String accessType){boolean ok=isAuthorized(userId,resource,accessType);logAccess(userId,resource,accessType,ok);if(!ok)throw new AccessDeniedException("Access denied");}
 public void logAccess(String userId,String resource,String accessType,boolean success){AuditLog a=new AuditLog();a.setAudTimestamp(LocalDateTime.now());a.setUserId(userId);a.setProgram(resource);a.setAudType(AuditType.USER);a.setAction("READ".equals(accessType)?AuditAction.INQUIRE:("LOGIN".equals(accessType)?AuditAction.LOGIN:AuditAction.UPDATE));a.setStatus(success?AuditStatus.SUCCESS:AuditStatus.FAILURE);a.setAccessType(accessType);audits.save(a);}
}
