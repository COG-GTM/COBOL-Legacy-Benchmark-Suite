package com.cobolbenchmark.security;

import com.cobolbenchmark.common.SecurityAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityService - from SECMGR.
 * Tests security validation flow (validate → authorize → audit).
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(jdbcTemplate);
    }

    @Test
    void testValidateUser_valid() {
        when(jdbcTemplate.queryForObject(
                contains("AUTHFILE"),
                eq(Integer.class),
                eq("USER01"), eq("PASS01")))
                .thenReturn(1);

        boolean result = securityService.validateUser("USER01", "PASS01");
        assertTrue(result);
    }

    @Test
    void testValidateUser_invalid() {
        when(jdbcTemplate.queryForObject(
                contains("AUTHFILE"),
                eq(Integer.class),
                eq("USER01"), eq("BADPASS")))
                .thenReturn(0);

        boolean result = securityService.validateUser("USER01", "BADPASS");
        assertFalse(result);
    }

    @Test
    void testValidateUser_dbError() {
        when(jdbcTemplate.queryForObject(
                contains("AUTHFILE"),
                eq(Integer.class),
                anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        boolean result = securityService.validateUser("USER01", "PASS01");
        assertFalse(result);
    }

    @Test
    void testAuthorizeAccess_authorized() {
        when(jdbcTemplate.queryForObject(
                contains("AUTHFILE"),
                eq(Integer.class),
                eq("USER01")))
                .thenReturn(1);

        boolean result = securityService.authorizeAccess("USER01", "PORT0001", "PORTFOLIO", "READ");
        assertTrue(result);
    }

    @Test
    void testAuthorizeAccess_unauthorized() {
        when(jdbcTemplate.queryForObject(
                contains("AUTHFILE"),
                eq(Integer.class),
                eq("USER01")))
                .thenReturn(0);

        assertThrows(SecurityAuthException.class, () ->
                securityService.authorizeAccess("USER01", "PORT0001", "PORTFOLIO", "READ"));
    }

    @Test
    void testPerformSecurityCheck_success() {
        // Validate
        when(jdbcTemplate.queryForObject(
                contains("PASSWORD_HASH"),
                eq(Integer.class),
                eq("USER01"), eq("PASS01")))
                .thenReturn(1);
        // Authorize
        when(jdbcTemplate.queryForObject(
                contains("STATUS = 'A'"),
                eq(Integer.class),
                eq("USER01")))
                .thenReturn(1);

        assertDoesNotThrow(() ->
                securityService.performSecurityCheck("USER01", "PASS01", "PORT0001", "PORTFOLIO"));

        // Verify audit log was called (6 params: timestamp, userId, program, accessType, action, detail)
        verify(jdbcTemplate, atLeastOnce()).update(contains("AUDITLOG"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testPerformSecurityCheck_validationFails() {
        when(jdbcTemplate.queryForObject(
                contains("PASSWORD_HASH"),
                eq(Integer.class),
                eq("USER01"), eq("BADPASS")))
                .thenReturn(0);

        assertThrows(SecurityAuthException.class, () ->
                securityService.performSecurityCheck("USER01", "BADPASS", "PORT0001", "PORTFOLIO"));
    }
}
