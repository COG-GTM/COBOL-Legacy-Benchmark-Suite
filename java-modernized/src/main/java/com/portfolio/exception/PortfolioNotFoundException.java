package com.portfolio.exception;

/**
 * Thrown when a portfolio record cannot be found.
 * Mirrors COBOL VSAM status '23' (record not found) handling in PORTMSTR.cbl.
 */
public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String portId) {
        super("Portfolio not found: " + portId);
    }
}
