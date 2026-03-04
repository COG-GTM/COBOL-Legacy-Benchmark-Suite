package com.cobolbenchmark.db;

import com.cobolbenchmark.common.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Recovery Service - migrated from DB2RECV.cbl.
 * Handles connection recovery with retry logic.
 * RECV-CONNECTION, RECV-TRANSACTION, RECV-CURSOR recovery types.
 * Max 3 retries, 2 second interval from DB2RECV.cbl.
 */
@Service
public class DatabaseRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRecoveryService.class);

    @Value("${app.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${app.retry.interval-ms:2000}")
    private long retryInterval;

    private final DataSource dataSource;

    public DatabaseRecoveryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Recover connection - replaces RECV-CONNECTION from DB2RECV.cbl.
     * Uses Spring @Retryable for automatic retry with backoff.
     */
    @Retryable(
        retryFor = DatabaseException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public Connection recoverConnection() {
        logger.info("Attempting database connection recovery...");
        try {
            Connection conn = dataSource.getConnection();
            if (conn.isValid(5)) {
                logger.info("Database connection recovered successfully");
                return conn;
            }
            throw new DatabaseException("Connection recovered but not valid");
        } catch (SQLException e) {
            logger.warn("Connection recovery attempt failed: {}", e.getMessage());
            throw new DatabaseException("Connection recovery failed: " + e.getMessage());
        }
    }

    /**
     * Recover transaction - replaces RECV-TRANSACTION from DB2RECV.cbl.
     * Attempts to rollback and re-establish clean transaction state.
     */
    @Retryable(
        retryFor = DatabaseException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public void recoverTransaction(Connection conn) {
        logger.info("Attempting transaction recovery...");
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                logger.info("Transaction rolled back successfully");
            } else {
                throw new DatabaseException("Connection is closed, cannot recover transaction");
            }
        } catch (SQLException e) {
            logger.warn("Transaction recovery attempt failed: {}", e.getMessage());
            throw new DatabaseException("Transaction recovery failed: " + e.getMessage());
        }
    }

    /**
     * Recover cursor - replaces RECV-CURSOR from DB2RECV.cbl.
     * Verifies connection is valid for cursor operations.
     */
    @Retryable(
        retryFor = DatabaseException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public boolean recoverCursor(Connection conn) {
        logger.info("Attempting cursor recovery...");
        try {
            if (conn != null && conn.isValid(5)) {
                logger.info("Connection valid for cursor recovery");
                return true;
            }
            throw new DatabaseException("Connection not valid for cursor recovery");
        } catch (SQLException e) {
            logger.warn("Cursor recovery attempt failed: {}", e.getMessage());
            throw new DatabaseException("Cursor recovery failed: " + e.getMessage());
        }
    }
}
