package com.cognition.clbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the modernised COBOL Legacy Benchmark Suite. Component scanning
 * starts at {@code com.cognition.clbs}, so every domain module's beans are picked up automatically.
 */
@SpringBootApplication
public class ClbsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClbsApplication.class, args);
  }
}
