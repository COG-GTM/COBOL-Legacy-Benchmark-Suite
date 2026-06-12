package com.clbs.online;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the online inquiry tier (migration target for the CICS
 * INQONLN / INQPORT / INQHIST programs). Boots the full portfolio domain so the
 * Phase 0 context-load and CRUD integration tests exercise the wiring.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.clbs.portfolio.domain")
@EnableJpaRepositories(basePackages = "com.clbs.portfolio.repository")
public class OnlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineApplication.class, args);
    }
}
