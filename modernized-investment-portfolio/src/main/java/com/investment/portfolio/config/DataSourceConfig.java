package com.investment.portfolio.config;

import org.springframework.context.annotation.Configuration;

/**
 * DataSource / HikariCP Configuration.
 *
 * HikariCP is auto-configured by Spring Boot via application.properties.
 * Key settings (matching the original DB2 connection pool):
 *   - Maximum pool size: 100 connections
 *   - Connection timeout: 300,000 ms (300 seconds / 5 minutes)
 *
 * These values mirror the original DB2 plan configuration from PORTPLAN.sql:
 *   BIND PLAN PORTPLAN ... ISOLATION(CS) ACQUIRE(USE) RELEASE(COMMIT)
 *
 * The HikariCP pool replaces the DB2 connection pooling with equivalent
 * capacity and timeout characteristics.
 *
 * @see application.properties for HikariCP configuration values
 */
@Configuration
public class DataSourceConfig {
    // HikariCP is configured entirely via application.properties.
    // This class serves as documentation of the configuration rationale
    // and can be extended for programmatic DataSource customization if needed.
}
