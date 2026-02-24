package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Investment Portfolio Management System - Migrated from Enterprise COBOL for z/OS.
 *
 * This application replaces the legacy COBOL batch + CICS online system with:
 * - Spring Batch for batch processing (TRNVAL00, POSUPD00, HISTLD00, reports)
 * - Spring MVC REST for online inquiries (INQONLN, INQPORT, INQHIST)
 * - Spring Security with JWT for security (SECMGR)
 * - JPA/Hibernate + PostgreSQL for data (VSAM + DB2)
 */
@SpringBootApplication
public class PortfolioManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagementApplication.class, args);
    }
}
