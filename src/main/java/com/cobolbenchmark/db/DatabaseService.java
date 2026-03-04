package com.cobolbenchmark.db;

import com.cobolbenchmark.common.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Service - migrated from DBPROC.cpy.
 * Manages database connections using Spring DataSource.
 * Replaces CONNECT-TO-DB2, DISCONNECT-FROM-DB2, DB2-ERROR-ROUTINE.
 */
@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final DataSource dataSource;

    public DatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Verify database connectivity - replaces CONNECT-TO-DB2.
     * Spring manages connections via DataSource pool.
     */
    public boolean verifyConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Database connection verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get connection status information.
     * Replaces DB2-STATUS operation from DBPROC.cpy.
     */
    public String getConnectionStatus() {
        try (Connection conn = dataSource.getConnection()) {
            return String.format("Connected to %s, catalog: %s",
                    conn.getMetaData().getURL(),
                    conn.getCatalog());
        } catch (SQLException e) {
            return "Disconnected: " + e.getMessage();
        }
    }

    /**
     * Handle database errors - replaces DB2-ERROR-ROUTINE.
     */
    public void handleDatabaseError(String operation, SQLException e) {
        int sqlCode = e.getErrorCode();
        String sqlState = e.getSQLState();
        logger.error("DB2 Error - Operation: {} SQLCODE: {} SQLSTATE: {} Message: {}",
                operation, sqlCode, sqlState, e.getMessage());
        throw new DatabaseException(sqlCode, operation + ": " + e.getMessage());
    }
}
