package com.portfolio.service;

import com.portfolio.audit.AuditService;
import com.portfolio.exception.AuthorizationException;
import com.portfolio.repository.AuthFileRepository;
import com.portfolio.security.SecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private AuthFileRepository authFileRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SecurityService securityService;

    @Test
    void validateUser_validUser_returnsTrue() {
        assertTrue(securityService.validateUser("TESTUSER"));
    }

    @Test
    void validateUser_emptyUser_returnsFalse() {
        assertFalse(securityService.validateUser(""));
    }

    @Test
    void validateUser_nullUser_returnsFalse() {
        assertFalse(securityService.validateUser(null));
    }

    @Test
    void checkAuthorization_authorized_returnsTrue() {
        when(authFileRepository.countByUserIdAndResourceAndAccessType(
                "USER1", "PORTFILE", "READ")).thenReturn(1L);

        assertTrue(securityService.checkAuthorization("USER1", "PORTFILE", "READ"));
    }

    @Test
    void checkAuthorization_notAuthorized_throwsException() {
        when(authFileRepository.countByUserIdAndResourceAndAccessType(
                "USER1", "PORTFILE", "WRITE")).thenReturn(0L);

        assertThrows(AuthorizationException.class,
                () -> securityService.checkAuthorization("USER1", "PORTFILE", "WRITE"));
    }
}
