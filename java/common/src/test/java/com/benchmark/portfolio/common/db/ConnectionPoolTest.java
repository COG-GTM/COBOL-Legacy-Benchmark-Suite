package com.benchmark.portfolio.common.db;

import java.sql.Connection;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional test for the HikariCP pool configured by {@link DataSourceConfig},
 * covering the connection lifecycle that DB2CONN.cbl managed via its
 * CONN/DISC function codes.
 */
class ConnectionPoolTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withConfiguration(UserConfigurations.of(DataSourceConfig.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:pooltest;DB_CLOSE_DELAY=-1",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.hikari.pool-name=PortfolioHikariPool",
                    "spring.datasource.hikari.maximum-pool-size=5",
                    "spring.datasource.hikari.minimum-idle=1",
                    "spring.datasource.hikari.connection-timeout=2000",
                    "spring.datasource.hikari.validation-timeout=1000");

    @Test
    void poolConfigValuesAreApplied() {
        runner.run(context -> {
            HikariDataSource hikari = (HikariDataSource) context.getBean(DataSource.class);
            assertThat(hikari.getPoolName()).isEqualTo("PortfolioHikariPool");
            assertThat(hikari.getMaximumPoolSize()).isEqualTo(5);
            assertThat(hikari.getMinimumIdle()).isEqualTo(1);
            assertThat(hikari.getConnectionTimeout()).isEqualTo(2000);
            assertThat(hikari.getValidationTimeout()).isEqualTo(1000);
        });
    }

    @Test
    void connectionsCanBeAcquiredAndReleased() {
        runner.run(context -> {
            HikariDataSource hikari = (HikariDataSource) context.getBean(DataSource.class);
            try (Connection connection = hikari.getConnection()) {
                assertThat(connection.isValid(1)).isTrue();
                assertThat(hikari.getHikariPoolMXBean().getActiveConnections()).isEqualTo(1);
            }
            assertThat(hikari.getHikariPoolMXBean().getActiveConnections()).isZero();
        });
    }
}
