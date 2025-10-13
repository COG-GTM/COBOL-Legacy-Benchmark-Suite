package com.cobol.benchmark.utilities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cobol.benchmark")
public class UtilitiesApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UtilitiesApplication.class, args);
    }
}
