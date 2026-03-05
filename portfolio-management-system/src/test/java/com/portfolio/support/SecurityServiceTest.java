package com.portfolio.support;

import com.portfolio.model.SecurityLogRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Security Service.
 * Verifies the three SECMGR entry points:
 *   SEC-VALIDATE -> UserDetailsService.loadUserByUsername()
 *   SEC-AUTHORIZE -> @PreAuthorize (tested in API tests)
 *   SEC-AUDIT -> SecurityLogRepository.save()
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityServiceTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private SecurityLogRepository securityLogRepository;

    @Test
    void testValidateUser_Success() {
        // Mirrors SECMGR P100-VALIDATE-USER
        UserDetails user = userDetailsService.loadUserByUsername("admin");
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getAuthorities()).isNotEmpty();
    }

    @Test
    void testValidateUser_NotFound() {
        // Mirrors SECMGR P100-VALIDATE-USER failure path
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void testAuditLogEntry() {
        // Mirrors SECMGR P300-LOG-ACCESS
        SecurityLogRecord auditLog = new SecurityLogRecord();
        auditLog.setAuditTimestamp(LocalDateTime.now());
        auditLog.setUserId("admin");
        auditLog.setProgram("INQONLN");
        auditLog.setAccessType("READ");
        auditLog.setResourceName("PORTFOLIO");
        auditLog.setResponseCode(0);

        SecurityLogRecord saved = securityLogRepository.save(auditLog);
        assertThat(saved.getAuditId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("admin");
    }

    @Test
    void testAuthorizeUser_AdminHasRoles() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");
        assertThat(admin.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))).isTrue();
        assertThat(admin.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();
    }

    @Test
    void testAuthorizeUser_RegularUserRoles() {
        UserDetails user = userDetailsService.loadUserByUsername("user");
        assertThat(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();
        assertThat(user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))).isFalse();
    }
}
