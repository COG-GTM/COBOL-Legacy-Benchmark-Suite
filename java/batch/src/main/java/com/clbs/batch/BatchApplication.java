package com.clbs.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the batch tier. COBOL batch programs (e.g. POSUPDT,
 * HISTLD00, RTNCDE00) will be migrated to Spring Batch jobs here in later phases.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.clbs.portfolio.domain")
@EnableJpaRepositories(basePackages = "com.clbs.portfolio.repository")
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
