package com.cognition.clbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CLBS portfolio application, the Java target of the
 * COBOL Investment Portfolio Management System migration.
 */
@SpringBootApplication
public class ClbsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClbsApplication.class, args);
    }
}
