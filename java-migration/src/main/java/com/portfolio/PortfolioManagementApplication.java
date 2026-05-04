package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication
public class PortfolioManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagementApplication.class, args);
    }
}
