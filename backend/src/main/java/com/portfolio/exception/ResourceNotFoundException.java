package com.portfolio.exception;

/**
 * Replaces COBOL NOTFND condition from INQPORT.cbl P900-NOT-FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
    }
}
