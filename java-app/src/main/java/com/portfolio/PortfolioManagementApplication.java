package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Investment Portfolio Management System.
 * Migrated from COBOL Legacy Benchmark Suite to Java Spring Boot.
 *
 * Replaces: ~30 COBOL programs, VSAM files, DB2 tables,
 * CICS online transactions, BMS screen maps, and JCL batch orchestration.
 */
@SpringBootApplication
@EnableScheduling
public class PortfolioManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagementApplication.class, args);
    }
}
