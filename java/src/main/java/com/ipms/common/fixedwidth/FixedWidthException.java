package com.ipms.common.fixedwidth;

/** Raised when a fixed-width record cannot be parsed or serialized. */
public class FixedWidthException extends RuntimeException {

    public FixedWidthException(String message) {
        super(message);
    }
}
