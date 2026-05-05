package com.portfolio.portmstr.exception;

import java.math.BigDecimal;

/**
 * Thrown when a sell transaction requests more units than available.
 * Equivalent to COBOL PORTTRAN.cbl 2220-PROCESS-SELL insufficient units check.
 */
public class InsufficientUnitsException extends RuntimeException {

    public InsufficientUnitsException(String portfolioId, BigDecimal requested, BigDecimal available) {
        super(String.format(
                "Insufficient units for sale in portfolio %s: requested=%s, available=%s",
                portfolioId, requested, available));
    }
}
