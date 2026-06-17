package com.clbs.position;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the modernized position-update microservice.
 * Replaces the COBOL batch program {@code POSUPDT} (process id {@code POSUPD00})
 * and exposes a REST API for position queries.
 */
@SpringBootApplication
public class PositionUpdateApplication {

    public static void main(String[] args) {
        SpringApplication.run(PositionUpdateApplication.class, args);
    }
}
