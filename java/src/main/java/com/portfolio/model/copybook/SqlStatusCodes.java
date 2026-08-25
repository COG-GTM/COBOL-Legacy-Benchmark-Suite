package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/db2/SQLCA.cpy} (01 SQL-STATUS-CODES).
 *
 * <p>The SQLCA itself (SQLCODE/SQLSTATE communication area) has no direct Java
 * equivalent: SQL error signalling is replaced by exceptions
 * ({@code DataAccessException} hierarchy / {@link com.portfolio.common.SqlProcessingException}).
 * The well-known SQLSTATE values checked by the COBOL programs are preserved here.
 */
public final class SqlStatusCodes {

    private SqlStatusCodes() {}

    /** SQL-SUCCESS PIC X(5) VALUE '00000'. */
    public static final String SUCCESS = "00000";
    /** SQL-NOT-FOUND PIC X(5) VALUE '02000'. */
    public static final String NOT_FOUND = "02000";
    /** SQL-DUP-KEY PIC X(5) VALUE '23505'. */
    public static final String DUPLICATE_KEY = "23505";
    /** SQL-DEADLOCK PIC X(5) VALUE '40001'. */
    public static final String DEADLOCK = "40001";
    /** SQL-TIMEOUT PIC X(5) VALUE '40003'. */
    public static final String TIMEOUT = "40003";
    /** SQL-CONNECTION-ERROR PIC X(5) VALUE '08001'. */
    public static final String CONNECTION_ERROR = "08001";
    /** SQL-DB-ERROR PIC X(5) VALUE '58004'. */
    public static final String DB_ERROR = "58004";

    /** DB2 SQLCODE -803: duplicate key on insert (tolerated by HISTLD00). */
    public static final int SQLCODE_DUPLICATE = -803;
}
