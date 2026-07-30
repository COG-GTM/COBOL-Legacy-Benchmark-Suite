package com.cognition.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the migrated portfolio transaction service.
 *
 * <p>Replaces the batch driver {@code PORTTRAN 0000-MAIN} as the runtime host for the
 * business rules extracted from the COBOL portfolio transaction programs.
 */
@SpringBootApplication
public class PortfolioTransactionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PortfolioTransactionServiceApplication.class, args);
  }
}
