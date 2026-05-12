package com.portfolio.service.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class Db2RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(Db2RecoveryService.class);

    private final DataSource dataSource;

    public Db2RecoveryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Connection recoverConnection() throws SQLException {
        log.info("Attempting to recover database connection...");
        Connection connection = dataSource.getConnection();
        if (connection.isValid(5)) {
            log.info("Database connection recovered successfully");
            return connection;
        }
        throw new SQLException("Connection validation failed");
    }
}
