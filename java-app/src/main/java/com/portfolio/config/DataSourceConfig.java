package com.portfolio.config;

import org.springframework.context.annotation.Configuration;

/**
 * DataSource configuration.
 * Replaces: DB2CONN.cbl, DB2ONLN.cbl
 *
 * Spring Boot auto-configures HikariCP connection pool via application.yml.
 * DB2 connection parameters are replaced by PostgreSQL datasource properties.
 * Connection pooling replaces the manual COBOL connection management in DB2ONLN.
 */
@Configuration
public class DataSourceConfig {
    // Spring Boot auto-configuration handles DataSource setup via application.yml.
    // HikariCP pool settings replace DB2CONN.cbl manual connection management.
    // DB2ONLN.cbl online connection pool is replaced by HikariCP's built-in pooling.
}
