package com.portfolio.common;
public final class SqlState {
    private SqlState() {}
    public static final String SUCCESS="00000", NOT_FOUND="02000", DUPLICATE="23505", DEADLOCK="40001",
            TIMEOUT="40003", CONNECTION_ERROR="08001", DATABASE_ERROR="58004";
}
