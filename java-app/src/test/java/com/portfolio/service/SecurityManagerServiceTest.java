package com.portfolio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityManagerService.
 * Verifies security logic migrated from SECMGR.cbl.
 */
@ExtendWith(MockitoExtension.class)
class SecurityManagerServiceTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SecurityManagerService securityManagerService;

    @Test
    void validateUser_authenticatedUser_returnsUserId() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("testuser");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        String userId = securityManagerService.validateUser();

        assertEquals("testuser", userId);
    }

    @Test
    void validateUser_noAuthentication_returnsNull() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        String userId = securityManagerService.validateUser();

        assertNull(userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkAuthorization_adminRole_returnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        Collection authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"));
        when(auth.getAuthorities()).thenReturn(authorities);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        boolean result = securityManagerService.checkAuthorization("PORTFOLIO", "WRITE");

        assertTrue(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkAuthorization_insufficientRole_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        Collection authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"));
        when(auth.getAuthorities()).thenReturn(authorities);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        boolean result = securityManagerService.checkAuthorization("PORTFOLIO", "WRITE");

        assertFalse(result);
    }
}
