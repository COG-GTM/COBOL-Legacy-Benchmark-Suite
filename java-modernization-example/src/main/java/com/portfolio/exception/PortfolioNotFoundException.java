package com.portfolio.exception;

/**
 * Thrown when a portfolio lookup yields no results.
 * Replaces VSAM status code '23' (ERR-VSAM-NOTFND) from ERRHAND.cpy.
 *
 * In the COBOL system:
 * <pre>
 *     WHEN ERR-VSAM-NOTFND
 *         MOVE ERR-WARNING TO LS-SEVERITY
 *         MOVE ERR-VSAM-23 TO LS-ERROR-TEXT   ('Record not found')
 * </pre>
 *
 * In the Java system, this exception is caught by {@code GlobalExceptionHandler}
 * and mapped to HTTP 404 Not Found.
 */
public class PortfolioNotFoundException extends PortfolioException {

    public PortfolioNotFoundException(String portfolioId) {
        super("Portfolio not found: " + portfolioId, "INQONLN", "VS", "0023");
    }
}
