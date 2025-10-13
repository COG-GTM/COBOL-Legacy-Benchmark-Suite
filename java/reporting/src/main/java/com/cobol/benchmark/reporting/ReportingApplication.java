package com.cobol.benchmark.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cobol.benchmark")
public class ReportingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReportingApplication.class, args);
    }
}
