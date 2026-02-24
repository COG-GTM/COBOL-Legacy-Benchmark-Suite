package com.investment.portfolio.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database Manager - Java equivalent of DBPROC.cpy and DB2CONN.cbl
 *
 * Provides centralized database connection management following the
 * COBOL DB2 connection patterns (CONNECT-TO-DB2, DISCONNECT-FROM-DB2,
 * DB2-ERROR-ROUTINE, CHECK-SQL-STATUS).
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /** Default database name matching COBOL's CONNECT TO POSMVP */
    private static final String DEFAULT_DB_NAME = "POSMVP";

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final ErrorHandler errorHandler;

    private Connection connection;
    private int retryCount;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_WAIT_MS = 100;

    /** SQL status codes matching SQLCA.cpy */
    public static final String SQL_SUCCESS = "00000";
    public static final String SQL_NOT_FOUND = "02000";
    public static final String SQL_DUP_KEY = "23505";
    public static final String SQL_DEADLOCK = "40001";
    public static final String SQL_TIMEOUT = "40003";
    public static final String SQL_CONNECTION_ERROR = "08001";
    public static final String SQL_DB_ERROR = "58004";

    public DatabaseManager(String jdbcUrl, String username, String password,
                           ErrorHandler errorHandler) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.errorHandler = errorHandler;
        this.retryCount = 0;
    }

    /**
     * Connects to the database.
     * Maps to CONNECT-TO-DB2 procedure in DBPROC.cpy.
     */
    public void connect() {
        try {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.setAutoCommit(false);
            LOGGER.info("Connected to database: " + DEFAULT_DB_NAME);
        } catch (SQLException e) {
            errorHandler.handleDatabaseError(e.getErrorCode(), e.getSQLState(),
                    "Connection failed");
            throw new DatabaseException("Failed to connect to database", e);
        }
    }

    /**
     * Commits current transaction.
     * Maps to EXEC SQL COMMIT WORK END-EXEC.
     */
    public void commit() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
            }
        } catch (SQLException e) {
            errorHandler.handleDatabaseError(e.getErrorCode(), e.getSQLState(),
                    "Commit failed");
            throw new DatabaseException("Commit failed", e);
        }
    }

    /**
     * Rolls back current transaction.
     * Maps to EXEC SQL ROLLBACK WORK END-EXEC.
     */
    public void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Rollback failed", e);
        }
    }

    /**
     * Disconnects from the database.
     * Maps to DISCONNECT-FROM-DB2 procedure in DBPROC.cpy.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
                connection.close();
                LOGGER.info("Disconnected from database");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error during disconnect", e);
        }
    }

    /**
     * Returns the active connection.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Checks if the connection is valid.
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Checks SQL status and handles errors with retry logic.
     * Maps to CHECK-SQL-STATUS and DB2-ERROR-ROUTINE in DBPROC.cpy.
     */
    public boolean handleSqlException(SQLException e, String operation) {
        String sqlState = e.getSQLState();

        // Deadlock or timeout - retry
        if (SQL_DEADLOCK.equals(sqlState) || SQL_TIMEOUT.equals(sqlState)) {
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                LOGGER.warning(String.format(
                        "Retryable SQL error (attempt %d/%d): %s - %s",
                        retryCount, MAX_RETRIES, sqlState, operation));
                try {
                    Thread.sleep(RETRY_WAIT_MS * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return true; // Signal: should retry
            }
        }

        // Duplicate key - not necessarily an error
        if (SQL_DUP_KEY.equals(sqlState)) {
            LOGGER.info("Duplicate key encountered: " + operation);
            return false;
        }

        // All other errors
        errorHandler.handleDatabaseError(e.getErrorCode(), sqlState, operation);
        rollback();
        return false;
    }

    public void resetRetryCount() {
        this.retryCount = 0;
    }

    /**
     * Custom unchecked exception for database errors.
     */
    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
