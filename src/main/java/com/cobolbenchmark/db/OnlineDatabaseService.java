package com.cobolbenchmark.db;

import com.cobolbenchmark.common.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Online Database Service - migrated from DB2ONLN.cbl.
 * Manages connection pool for online (CICS) programs.
 * Operations: DB2-CONNECT, DB2-DISCONNECT, DB2-STATUS.
 * Max 100 connections from DB2ONLN.cbl.
 */
@Service
public class OnlineDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(OnlineDatabaseService.class);
    private static final int MAX_CONNECTIONS = 100;

    private final DataSource dataSource;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public OnlineDatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Connect operation - replaces DB2-CONNECT from DB2ONLN.cbl.
     * Spring manages actual pooling; this tracks logical connections.
     */
    public Connection connect() {
        int current = activeConnections.incrementAndGet();
        if (current > MAX_CONNECTIONS) {
            activeConnections.decrementAndGet();
            throw new DatabaseException("Maximum connection limit (" + MAX_CONNECTIONS + ") exceeded");
        }
        try {
            Connection conn = dataSource.getConnection();
            logger.debug("Online DB2 connection acquired. Active: {}", current);
            return conn;
        } catch (SQLException e) {
            activeConnections.decrementAndGet();
            throw new DatabaseException("Failed to acquire connection: " + e.getMessage());
        }
    }

    /**
     * Disconnect operation - replaces DB2-DISCONNECT from DB2ONLN.cbl.
     */
    public void disconnect(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("Error closing connection: {}", e.getMessage());
            }
            activeConnections.decrementAndGet();
            logger.debug("Online DB2 connection released. Active: {}", activeConnections.get());
        }
    }

    /**
     * Status operation - replaces DB2-STATUS from DB2ONLN.cbl.
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }

    public int getMaxConnections() {
        return MAX_CONNECTIONS;
    }
}
