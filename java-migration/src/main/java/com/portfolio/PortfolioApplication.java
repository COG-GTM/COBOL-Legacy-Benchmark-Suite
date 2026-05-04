package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Investment Portfolio Management System - Spring Boot Application.
 * Migrated from COBOL Legacy Benchmark Suite.
 */
@SpringBootApplication
@EnableRetry
@EnableScheduling
public class PortfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApplication.class, args);
    }
}
