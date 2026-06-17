package com.clbs.position.domain;

/**
 * Raised when a SELL would drive the share balance negative. Mirrors the COBOL
 * guard in {@code PORTTRAN.cbl 2220-PROCESS-SELL}
 * ({@code IF PORT-TOTAL-UNITS < TRN-QUANTITY ... 'Insufficient units for sale'})
 * and validation rule 5.2 in the data dictionary
 * ("Share Balance must not go negative", error code {@code E004}).
 */
public class InsufficientPositionException extends RuntimeException {
    public InsufficientPositionException(String message) {
        super(message);
    }
}
