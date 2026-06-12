package com.benchmark.portfolio.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link DatabaseConnectionHealthIndicator}, which replicates
 * DB2CONN.cbl 3000-CHECK-STATUS semantics (return code 0 when connected,
 * 4 with "DB2 connection not active" when not).
 */
class DatabaseConnectionHealthIndicatorTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:healthtest;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setPoolName("PortfolioHikariPool");
        config.setMaximumPoolSize(3);
        config.setConnectionTimeout(1000);
        dataSource = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    void reportsUpWithPoolStatsWhenDatabaseReachable() {
        Health health = new DatabaseConnectionHealthIndicator(dataSource).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("connectionState", "CONNECTED")
                .containsEntry("returnCode", 0)
                .containsEntry("poolName", "PortfolioHikariPool")
                .containsKeys("activeConnections", "idleConnections",
                        "totalConnections", "threadsAwaitingConnection", "database");
    }

    @Test
    void reportsDownWhenDatabaseUnavailable() {
        dataSource.close();

        Health health = new DatabaseConnectionHealthIndicator(dataSource).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("connectionState", "DISCONNECTED")
                .containsEntry("returnCode", 4)
                .containsEntry("errorMessage", "DB2 connection not active");
    }
}
