package com.cobolbenchmark.common;

/**
 * Exception for security/authorization failures.
 * Replaces COBOL SECMGR authorization failures.
 */
public class SecurityAuthException extends ApplicationException {

    public SecurityAuthException(String message) {
        super("SECFAIL", message);
    }

    public SecurityAuthException(String operation, String userId) {
        super("SECFAIL", "Security " + operation + " failed for user: " + userId);
    }
}
