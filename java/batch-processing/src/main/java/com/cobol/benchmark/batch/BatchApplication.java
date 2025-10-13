package com.cobol.benchmark.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cobol.benchmark")
public class BatchApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
