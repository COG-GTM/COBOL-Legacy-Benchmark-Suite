package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point.
 *
 * Replaces the CICS transaction definition (PINQ) from PORTDFN.csd
 * (src/cics/PORTDFN.csd) that mapped the transaction ID to INQONLN.cbl.
 *
 * In the mainframe world, a user typed a transaction code (e.g. PINQ) on a
 * 3270 terminal, CICS looked up the program in the CSD, and loaded INQONLN.
 * In the Spring Boot world, the application starts an embedded Tomcat server
 * and the InquiryController handles HTTP requests at /api/inquiry/*.
 */
@SpringBootApplication
public class InquiryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InquiryApplication.class, args);
    }
}
