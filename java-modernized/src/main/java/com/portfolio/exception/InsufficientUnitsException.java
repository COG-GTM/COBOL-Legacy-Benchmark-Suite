package com.portfolio.exception;

/**
 * Thrown when a sell transaction exceeds available units.
 * Mirrors PORTTRAN.cbl paragraph 2220-PROCESS-SELL:
 * "IF PORT-TOTAL-UNITS < TRN-QUANTITY ... 'Insufficient units for sale'"
 */
public class InsufficientUnitsException extends RuntimeException {

    public InsufficientUnitsException(String portfolioId) {
        super("Insufficient units for sale in portfolio: " + portfolioId);
    }
}
