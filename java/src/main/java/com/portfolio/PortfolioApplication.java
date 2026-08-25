package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java migration of the Enterprise COBOL Investment Portfolio Management System.
 *
 * <p>The CICS online layer (src/programs/online, src/maps) is intentionally NOT
 * migrated here — it is flagged as a separate redesign effort (CICS → REST).
 */
@SpringBootApplication
public class PortfolioApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(PortfolioApplication.class, args)));
    }
}
