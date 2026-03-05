package com.portfolio.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * DB2 Recovery Service.
 * Migrated from COBOL DB2RECV.cbl (lines 19-22):
 *   WS-MAX-RETRIES = 3
 *   WS-RETRY-INTERVAL = 2 seconds
 *
 * Retry parameters are EXTERNALIZED to application.properties:
 *   portfolio.db2.retry.max-attempts=3
 *   portfolio.db2.retry.interval-ms=2000
 *
 * Three recovery modes from DB2RECV:
 *   RECV-CONNECTION ('C') -> P100-RECOVER-CONNECTION
 *   RECV-TRANSACTION ('T') -> P200-RECOVER-TRANSACTION
 *   RECV-CURSOR ('R') -> P300-RECOVER-CURSOR
 */
@Service
public class Db2RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(Db2RecoveryService.class);

    @Value("${portfolio.db2.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${portfolio.db2.retry.interval-ms:2000}")
    private long retryIntervalMs;

    /**
     * Execute a database operation with retry logic.
     * Replaces DB2RECV P100-RECOVER-CONNECTION retry loop.
     *
     * @param operation the operation to execute with retry
     * @param <T> return type
     * @return result of the operation
     */
    @Retryable(
            retryFor = DataAccessException.class,
            maxAttemptsExpression = "${portfolio.db2.retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${portfolio.db2.retry.interval-ms:2000}")
    )
    public <T> T executeWithRetry(RetryableOperation<T> operation) {
        log.debug("Executing DB2 operation with retry (max={}, interval={}ms)",
                maxRetryAttempts, retryIntervalMs);
        return operation.execute();
    }

    /**
     * Functional interface for retryable operations.
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute();
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }
}
