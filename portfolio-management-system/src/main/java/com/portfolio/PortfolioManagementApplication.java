package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Investment Portfolio Management System
 * Migrated from COBOL/z/OS to Java 17 + Spring Boot 3.x
 *
 * Migration strategy: Strangler Fig + Anti-Corruption Layer (ACL)
 * Source: COBOL programs in src/programs/
 * Target: Spring Boot 3.x with Spring Batch, Spring Security, Spring Data JPA
 */
@SpringBootApplication
@EnableRetry
@EnableScheduling
public class PortfolioManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagementApplication.class, args);
    }
}
