package com.portfolio.exception;

public class InsufficientUnitsException extends RuntimeException {

    public InsufficientUnitsException(String message) {
        super(message);
    }
}
