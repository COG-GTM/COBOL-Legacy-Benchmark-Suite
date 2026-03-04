package com.cobolbenchmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Portfolio Management System - Main Application Entry Point.
 * Migrated from COBOL Legacy Benchmark Suite (CICS/Batch/DB2/VSAM).
 */
@SpringBootApplication
@EnableRetry
public class PortfolioManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagementApplication.class, args);
    }
}
