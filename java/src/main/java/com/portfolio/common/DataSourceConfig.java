package com.portfolio.common;

import org.springframework.context.annotation.Configuration;

/**
 * Migration of {@code src/programs/common/DB2CONN.cbl} (DB2 Connection Manager).
 *
 * <p>DB2CONN implemented CONN/DISC/STAT functions with manual retry
 * (WS-MAX-RETRIES = 3) around {@code EXEC SQL CONNECT}. In Java this entire
 * responsibility moves to the Spring Boot datasource layer:
 *
 * <ul>
 *   <li>CONN (1000-CONNECT, retry loop) → HikariCP connection pool with
 *       acquisition retry/timeout, configured via {@code spring.datasource.*}
 *       properties (H2 for tests/samples, DB2 JDBC URL in production).</li>
 *   <li>DISC (2000-DISCONNECT, COMMIT + CONNECT RESET) → connections returned
 *       to the pool after each transaction commits.</li>
 *   <li>STAT (3000-CHECK-STATUS, SELECT CURRENT SERVER) → pool validation /
 *       {@code connection-test-query}.</li>
 *   <li>Connection-error return codes (LS-SQLCODE, RC 12) → runtime
 *       {@code DataAccessException}s from Spring.</li>
 * </ul>
 *
 * <p>No explicit beans are needed: Spring Boot auto-configures the pooled
 * {@code DataSource}, JPA {@code EntityManagerFactory}, and
 * {@code PlatformTransactionManager}. This class exists as the documented
 * anchor point for datasource customizations in future slices.
 */
@Configuration
public class DataSourceConfig {
}
