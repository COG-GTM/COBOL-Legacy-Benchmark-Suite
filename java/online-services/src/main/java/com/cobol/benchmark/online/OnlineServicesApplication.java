package com.cobol.benchmark.online;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cobol.benchmark")
public class OnlineServicesApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OnlineServicesApplication.class, args);
    }
}
