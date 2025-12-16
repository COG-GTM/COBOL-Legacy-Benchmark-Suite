package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Portfolio Management Service Application
 * 
 * This is the main entry point for the modernized Investment Portfolio Management System.
 * Migrated from COBOL/CICS/DB2 mainframe architecture to Spring Boot 3.x.
 * 
 * Phase 1 Migration includes:
 * - Spring Data JPA for database access (replacing VSAM and DB2)
 * - Spring Security for authentication (replacing CICS SECMGR)
 * - Spring Batch for batch processing (replacing COBOL batch programs)
 * - Redis caching for performance optimization
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
