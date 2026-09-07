package com.clbs.online;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clbs.common.AuditProcessor;
import com.clbs.db2.AuthorizationRepository;
import com.clbs.online.SecurityManager.SecurityRequest;
import com.clbs.online.SecurityManager.SecurityResponse;
import com.clbs.store.DatasetCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityManagerTest {

    private AuthorizationRepository authorizations;
    private SecurityManager security;

    @BeforeEach
    void setUp() {
        authorizations = mock(AuthorizationRepository.class);
        security = new SecurityManager(authorizations, new AuditProcessor(new DatasetCatalog()));
    }

    private static SecurityRequest request(char type, String user, String signedOn) {
        return new SecurityRequest(type, user, "INQONLN", "READ", "TRM1", signedOn);
    }

    @Test
    void validationRequiresTheSignedOnUserToMatch() {
        assertThat(security.validateUser(request('V', "USER0001", "USER0001")).ok()).isTrue();

        SecurityResponse mismatch = security.validateUser(request('V', "USER0001", "USER0002"));
        assertThat(mismatch.responseCode()).isEqualTo(SecurityManager.DENIED);
        assertThat(mismatch.errorInfo()).isEqualTo(SecurityManager.ERR_VALIDATE);

        assertThat(security.validateUser(request('V', "USER0001", "")).responseCode())
                .isEqualTo(SecurityManager.FAILED);
    }

    @Test
    void authorizationUsesTheAuthorizationCount() {
        when(authorizations.countByUserIdAndResourceAndAccessType("USER0001", "INQONLN", "READ"))
                .thenReturn(1L);
        assertThat(security.checkAuthorization(request('A', "USER0001", "USER0001")).ok()).isTrue();

        when(authorizations.countByUserIdAndResourceAndAccessType("USER0002", "INQONLN", "READ"))
                .thenReturn(0L);
        assertThat(security.checkAuthorization(request('A', "USER0002", "USER0002")).errorInfo())
                .isEqualTo(SecurityManager.ERR_DENIED);
    }

    @Test
    void authorizationFailureIsReportedAsFailed() {
        when(authorizations.countByUserIdAndResourceAndAccessType("USER0001", "INQONLN", "READ"))
                .thenThrow(new IllegalStateException("db down"));

        assertThat(security.checkAuthorization(request('A', "USER0001", "USER0001")).responseCode())
                .isEqualTo(SecurityManager.FAILED);
    }

    @Test
    void dispatchRoutesOnRequestType() {
        when(authorizations.countByUserIdAndResourceAndAccessType("USER0001", "INQONLN", "READ"))
                .thenReturn(1L);

        assertThat(security.execute(request('V', "USER0001", "USER0001")).ok()).isTrue();
        assertThat(security.execute(request('A', "USER0001", "USER0001")).ok()).isTrue();
        assertThat(security.execute(request('L', "USER0001", "USER0001")).ok()).isTrue();
        assertThat(security.execute(request('?', "USER0001", "USER0001")).ok()).isTrue();
    }
}
