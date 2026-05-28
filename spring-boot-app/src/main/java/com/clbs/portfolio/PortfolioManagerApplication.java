package com.clbs.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortfolioManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagerApplication.class, args);
    }
}
