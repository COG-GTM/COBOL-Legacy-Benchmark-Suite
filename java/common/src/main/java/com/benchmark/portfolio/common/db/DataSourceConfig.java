package com.benchmark.portfolio.common.db;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Connection-pool configuration migrated from {@code DB2CONN.cbl} (DB2
 * Connection Manager) and {@code DBPROC.cpy}.
 *
 * <p>In the legacy system every program CALLed DB2CONN with a function code
 * ('CONN', 'DISC', 'STAT') to manage a single DB2 thread per task. In the
 * Java migration that connection lifecycle is delegated to HikariCP:
 *
 * <ul>
 *   <li>{@code 1000-CONNECT} retry loop (DB2-MAX-RETRIES=3, DB2-RETRY-WAIT=100ms
 *       from DBPROC.cpy) maps to Hikari's {@code initializationFailTimeout}
 *       and {@code connectionTimeout} — the pool keeps retrying connection
 *       acquisition until the timeout elapses.</li>
 *   <li>{@code 1100-HANDLE-CONN-ERROR} SQLCODE -30081 ("Maximum connections
 *       exceeded") is prevented by bounding {@code maximumPoolSize}.</li>
 *   <li>{@code 2000-DISCONNECT} (COMMIT WORK + CONNECT RESET) maps to
 *       returning a connection to the pool; transaction commit/rollback is
 *       managed by Spring's transaction manager (see DB2CMT.cbl semantics).</li>
 *   <li>{@code 3000-CHECK-STATUS} (SELECT CURRENT SERVER FROM
 *       SYSIBM.SYSDUMMY1) maps to Hikari connection validation plus the
 *       {@link DatabaseConnectionHealthIndicator} actuator component.</li>
 * </ul>
 *
 * <p>Pool sizing/timeout values are bound from the {@code spring.datasource.hikari.*}
 * properties declared in the shared {@code application.yml}.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
