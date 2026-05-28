package com.clbs.portfolio.service.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component("customDatabaseHealthIndicator")
public class DatabaseHealthIndicator implements HealthIndicator {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Health health() {
        try {
            long start = System.currentTimeMillis();
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            long duration = System.currentTimeMillis() - start;

            Health.Builder builder;
            if (duration > 5000) {
                builder = Health.down()
                        .withDetail("reason", "Database response time too high");
            } else if (duration > 1000) {
                builder = Health.up()
                        .withDetail("warning", "Database response time elevated");
            } else {
                builder = Health.up();
            }

            return builder
                    .withDetail("responseTimeMs", duration)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("reason", "Database unreachable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
