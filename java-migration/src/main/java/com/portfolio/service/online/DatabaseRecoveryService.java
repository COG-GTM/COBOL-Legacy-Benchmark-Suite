package com.portfolio.service.online;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Recovery Service - migrated from COBOL DB2RECV.cbl.
 * Retry logic (max 3 retries with wait interval) -> Spring Retry @Retryable.
 */
@Service
public class DatabaseRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRecoveryService.class);

    private final DataSource dataSource;

    public DatabaseRecoveryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Retryable(retryFor = SQLException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public Connection getConnection() throws SQLException {
        log.debug("Attempting to obtain database connection");
        return dataSource.getConnection();
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            log.error("Database connection test failed", e);
            return false;
        }
    }
}
