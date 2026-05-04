package com.portfolio.config;

import org.springframework.context.annotation.Configuration;

/**
 * DataSource Configuration.
 * Spring Boot auto-configures HikariCP connection pool from application.yml.
 * Replaces DB2CONN.cbl connection management.
 * Replaces DB2CMT.cbl commit management (@Transactional handles this).
 */
@Configuration
public class DataSourceConfig {
}
