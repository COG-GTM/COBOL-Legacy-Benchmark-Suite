package com.portfolio.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for DB2 Recovery Service.
 * Verifies retry logic: 3 retries with 2s interval on simulated DB2 failure.
 * From DB2RECV.cbl lines 19-22: WS-MAX-RETRIES=3, WS-RETRY-INTERVAL=2.
 */
@SpringBootTest
@ActiveProfiles("test")
class Db2RecoveryServiceTest {

    @Autowired
    private Db2RecoveryService recoveryService;

    @Test
    void testRetryConfigurationFromProperties() {
        assertThat(recoveryService.getMaxRetryAttempts()).isEqualTo(3);
        assertThat(recoveryService.getRetryIntervalMs()).isEqualTo(2000);
    }

    @Test
    void testSuccessfulOperationNoRetry() {
        String result = recoveryService.executeWithRetry(() -> "SUCCESS");
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    void testRetryOnTransientFailureThenSuccess() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = recoveryService.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new QueryTimeoutException("Simulated DB2 timeout");
            }
            return "SUCCESS_AFTER_RETRY";
        });

        assertThat(result).isEqualTo("SUCCESS_AFTER_RETRY");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void testExhaustedRetriesThrowsException() {
        assertThatThrownBy(() ->
                recoveryService.executeWithRetry(() -> {
                    throw new QueryTimeoutException("Persistent DB2 failure");
                })
        ).isInstanceOf(DataAccessException.class);
    }
}
