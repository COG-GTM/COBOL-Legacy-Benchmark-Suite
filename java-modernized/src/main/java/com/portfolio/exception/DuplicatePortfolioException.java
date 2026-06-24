package com.portfolio.exception;

/**
 * Thrown when attempting to create a portfolio with an ID that already exists.
 * Mirrors COBOL VSAM status '22' (duplicate key) handling in PORTMSTR.cbl.
 */
public class DuplicatePortfolioException extends RuntimeException {

    public DuplicatePortfolioException(String portId) {
        super("Portfolio ID already exists: " + portId);
    }
}
