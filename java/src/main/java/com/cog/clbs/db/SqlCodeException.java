package com.cog.clbs.db;

import java.sql.SQLException;

/**
 * DB2-style SQL error carrying a SQLCODE.
 *
 * <p>Java equivalent of the SQLCA/SQLCODE checking pattern in
 * {@code src/templates/database/db2-handling.cbl} (9000-CHECK-SQL-STATUS):
 * a negative SQLCODE signals an error condition that triggers rollback.
 * Common SQLCODE conventions:
 * <ul>
 *   <li>0    - success</li>
 *   <li>+100 - row not found / end of cursor</li>
 *   <li>&lt; 0 - error (e.g. -803 duplicate key, -811 multiple rows)</li>
 * </ul>
 */
public class SqlCodeException extends RuntimeException {

    /** SQLCODE +100: row not found / end of cursor. */
    public static final int SQLCODE_NOT_FOUND = 100;
    /** SQLCODE -803: duplicate key on insert. */
    public static final int SQLCODE_DUPLICATE_KEY = -803;

    private final int sqlCode;

    public SqlCodeException(int sqlCode, String message, SQLException cause) {
        super("DB2 ERROR - SQLCODE: " + sqlCode + ", SQLERRM: " + message, cause);
        this.sqlCode = sqlCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }

    /**
     * Maps a JDBC {@link SQLException} to a SQLCODE-carrying exception.
     * DB2 JDBC drivers expose the native SQLCODE via {@code getErrorCode()}.
     */
    public static SqlCodeException from(SQLException e) {
        int code = e.getErrorCode() != 0 ? e.getErrorCode() : -1;
        return new SqlCodeException(code, e.getMessage(), e);
    }
}
