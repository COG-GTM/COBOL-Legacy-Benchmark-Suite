package com.clbs.portfolio.config;

import org.springframework.context.annotation.Configuration;

/**
 * Data source configuration.
 * Spring Boot auto-configuration handles datasource setup based on active profile.
 * - dev/test: H2 in-memory (configured in application-dev.yml)
 * - prod: PostgreSQL (configured in application-prod.yml)
 */
@Configuration
public class DataSourceConfig {
    // Spring Boot auto-configuration handles datasource setup via application YAML profiles.
}
