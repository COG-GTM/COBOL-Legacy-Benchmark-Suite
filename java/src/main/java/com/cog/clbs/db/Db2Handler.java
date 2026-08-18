package com.cog.clbs.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database access helper.
 *
 * <p>Java equivalent of {@code src/templates/database/db2-handling.cbl}.
 * The COBOL template demonstrates the standard DB2 interaction patterns;
 * this class maps each to its JDBC counterpart:
 *
 * <pre>
 *   EXEC SQL CONNECT TO ...        -&gt; connect(url, user, password)
 *   host variables (:HV-...)       -&gt; PreparedStatement parameters
 *   DECLARE/OPEN/FETCH/CLOSE cursor-&gt; PreparedStatement + ResultSet
 *   9000-CHECK-SQL-STATUS          -&gt; SqlCodeException (SQLCODE mapping)
 *   COMMIT WORK / ROLLBACK WORK    -&gt; commit() / rollback()
 *   CONNECT RESET                  -&gt; disconnect()
 * </pre>
 *
 * <p>Auto-commit is disabled so that unit-of-work boundaries are explicit,
 * matching the COBOL commit/rollback discipline.
 */
public class Db2Handler implements AutoCloseable {

    private Connection connection;

    /** EXEC SQL CONNECT TO ... END-EXEC. */
    public void connect(String jdbcUrl, String user, String password) {
        try {
            connection = DriverManager.getConnection(jdbcUrl, user, password);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /** Attaches an externally managed connection (e.g. from a DataSource). */
    public void attach(Connection externalConnection) {
        this.connection = externalConnection;
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /**
     * Prepares a statement with host-variable placeholders ('?'),
     * the JDBC equivalent of :HV-... host variables.
     */
    public PreparedStatement prepare(String sql) {
        requireConnected();
        try {
            return connection.prepareStatement(sql);
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /**
     * DECLARE ... CURSOR FOR / OPEN cursor: executes a query and returns
     * the {@link ResultSet}, the cursor equivalent. Callers iterate with
     * {@code rs.next()} (FETCH; false = SQLCODE +100) and close it
     * (CLOSE cursor) when done.
     */
    public ResultSet openCursor(PreparedStatement statement) {
        try {
            return statement.executeQuery();
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /** Executes INSERT/UPDATE/DELETE, returning the affected row count. */
    public int executeUpdate(PreparedStatement statement) {
        try {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /** EXEC SQL COMMIT WORK END-EXEC. */
    public void commit() {
        requireConnected();
        try {
            connection.commit();
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /** EXEC SQL ROLLBACK WORK END-EXEC (9100-ROLLBACK). */
    public void rollback() {
        requireConnected();
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw SqlCodeException.from(e);
        }
    }

    /** EXEC SQL CONNECT RESET END-EXEC. */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw SqlCodeException.from(e);
            } finally {
                connection = null;
            }
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

    @Override
    public void close() {
        disconnect();
    }

    private void requireConnected() {
        if (connection == null) {
            throw new IllegalStateException("Not connected to database");
        }
    }
}
