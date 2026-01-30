package com.portfolio.exception;

public class InsufficientUnitsException extends RuntimeException {

    public InsufficientUnitsException(String message) {
        super(message);
    }

    public InsufficientUnitsException(String portfolioId, java.math.BigDecimal requested, java.math.BigDecimal available) {
        super(String.format("Insufficient units for sale in portfolio %s. Requested: %s, Available: %s",
                portfolioId, requested, available));
    }
}
