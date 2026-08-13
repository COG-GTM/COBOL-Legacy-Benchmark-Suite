package com.ipms.common.db;

import com.ipms.domain.ReturnCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection manager, ported from {@code src/programs/common/DB2CONN.cbl}.
 *
 * <p>DB2CONN handled CONN/DISC/STAT requests with up to 3 connect retries against a named
 * database/plan. Backed by a JDBC {@link DataSource}, the plan concept has no direct
 * equivalent; retry-with-delay behavior and status checking are preserved.
 */
@Component
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    static final int MAX_RETRIES = 3;

    private final DataSource dataSource;
    private final long retryWaitMillis;

    public ConnectionManager(DataSource dataSource) {
        this(dataSource, 1000);
    }

    public ConnectionManager(DataSource dataSource, long retryWaitMillis) {
        this.dataSource = dataSource;
        this.retryWaitMillis = retryWaitMillis;
    }

    /**
     * FUNC-CONN (1000-CONNECT): obtains a connection, retrying up to {@link #MAX_RETRIES}
     * times with a delay between attempts.
     *
     * @throws ConnectionException with return code 12 (RC-SEVERE) when all retries fail
     */
    public Connection connect() {
        SQLException last = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                last = e;
                log.warn("Connection attempt {} of {} failed: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep();
                }
            }
        }
        throw new ConnectionException("Unable to connect after " + MAX_RETRIES + " attempts",
                ReturnCodes.RC_SEVERE, last);
    }

    /** FUNC-DISC (2000-DISCONNECT): commits pending work and releases the connection. */
    public void disconnect(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                connection.close();
            }
        } catch (SQLException e) {
            throw new ConnectionException("Error disconnecting from database",
                    ReturnCodes.RC_ERROR, e);
        }
    }

    /** FUNC-STAT (3000-CHECK-STATUS): verifies the connection is alive with a probe query. */
    public boolean isConnected(Connection connection) {
        if (connection == null) {
            return false;
        }
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            log.warn("Connection status check failed: {}", e.getMessage());
            return false;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(retryWaitMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
