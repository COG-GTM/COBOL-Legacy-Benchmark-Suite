package com.portfolio.service.common;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Db2HealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public Db2HealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            return Health.up()
                    .withDetail("database", "available")
                    .withDetail("status", result)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "unavailable")
                    .withException(e)
                    .build();
        }
    }
}
