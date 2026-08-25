package com.portfolio.common;

/**
 * Replaces COBOL SQLCA return-code checks ({@code IF SQLCODE = 0 ... ELSE ...}).
 *
 * <p>Convention: any non-zero SQLCODE branch in COBOL becomes a thrown
 * {@code SqlProcessingException}. The original SQLCODE (when known) is kept in
 * {@link #getSqlCode()} so callers can preserve code-specific behavior — e.g.
 * HISTLD00 ignores duplicate inserts (SQLCODE -803), which in Java becomes an
 * existence check / duplicate-key handling rather than an error.
 */
public class SqlProcessingException extends RuntimeException {

    /** SQLCODE for a duplicate key insert, ignored by HISTLD00 (2200-LOAD-TO-DB2). */
    public static final int SQLCODE_DUPLICATE = -803;

    private final int sqlCode;

    public SqlProcessingException(String message, int sqlCode) {
        super(message);
        this.sqlCode = sqlCode;
    }

    public SqlProcessingException(String message, int sqlCode, Throwable cause) {
        super(message, cause);
        this.sqlCode = sqlCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }
}
