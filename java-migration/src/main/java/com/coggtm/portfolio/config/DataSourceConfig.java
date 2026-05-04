package com.coggtm.portfolio.config;

import org.springframework.context.annotation.Configuration;

/**
 * Datasource configuration for DB2 / PostgreSQL connectivity.
 * <p>
 * Replaces the COBOL DB2 BIND PLAN and DBRM configuration found in
 * {@code src/database/db2/PORTPLAN.sql}. Connection pooling and
 * dialect selection are driven by Spring profiles defined in
 * {@code application-*.yml}.
 * </p>
 */
@Configuration
public class DataSourceConfig {
    // TODO: Add custom DataSource beans if multi-datasource routing
    //       (e.g. DB2 + PostgreSQL) is required during migration.
}
