package com.benchmark.portfolio.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Database connection health check migrated from {@code DB2CONN.cbl}
 * paragraph {@code 3000-CHECK-STATUS}.
 *
 * <p>The COBOL routine probed the connection with
 * {@code SELECT CURRENT SERVER INTO :WS-DB-NAME FROM SYSIBM.SYSDUMMY1} and set
 * the 88-level {@code WS-CONNECTED}/{@code WS-DISCONNECTED} state plus return
 * code 0 (connected) or 4 ("DB2 connection not active"). This indicator runs
 * an equivalent validation query against the pooled DataSource and reports
 * the connection state, the database identity (the CURRENT SERVER analogue),
 * and HikariCP pool statistics.
 */
@Component("databaseConnection")
public class DatabaseConnectionHealthIndicator implements HealthIndicator {

    static final String VALIDATION_QUERY = "SELECT 1";

    private final DataSource dataSource;

    public DatabaseConnectionHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        return checkStatus();
    }

    /** Mirrors DB2CONN 3000-CHECK-STATUS. */
    private Health checkStatus() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(VALIDATION_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            Health.Builder builder = Health.up()
                    .withDetail("connectionState", "CONNECTED")
                    .withDetail("returnCode", 0)
                    .withDetail("database", connection.getMetaData().getDatabaseProductName());
            addPoolStats(builder);
            return builder.build();
        } catch (SQLException e) {
            Health.Builder builder = Health.down(e)
                    .withDetail("connectionState", "DISCONNECTED")
                    .withDetail("returnCode", 4)
                    .withDetail("errorMessage", "DB2 connection not active");
            addPoolStats(builder);
            return builder.build();
        }
    }

    private void addPoolStats(Health.Builder builder) {
        if (dataSource instanceof HikariDataSource hikari) {
            builder.withDetail("poolName", hikari.getPoolName());
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool != null) {
                builder.withDetail("activeConnections", pool.getActiveConnections())
                        .withDetail("idleConnections", pool.getIdleConnections())
                        .withDetail("totalConnections", pool.getTotalConnections())
                        .withDetail("threadsAwaitingConnection", pool.getThreadsAwaitingConnection());
            }
        }
    }
}
