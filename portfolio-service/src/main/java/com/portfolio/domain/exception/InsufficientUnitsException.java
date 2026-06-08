package com.portfolio.domain.exception;

import java.math.BigDecimal;

/**
 * Thrown when a sell transaction requests more units than available.
 */
public class InsufficientUnitsException extends RuntimeException {

    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientUnitsException(BigDecimal requested, BigDecimal available) {
        super("Insufficient units: requested=" + requested + ", available=" + available);
        this.requested = requested;
        this.available = available;
    }

    public BigDecimal getRequested() { return requested; }
    public BigDecimal getAvailable() { return available; }
}
