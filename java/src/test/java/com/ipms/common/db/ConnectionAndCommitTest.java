package com.ipms.common.db;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionAndCommitTest {

    private DataSource dataSource;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cmt-test;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        dataSource = ds;
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS T");
            stmt.execute("CREATE TABLE T (ID INT PRIMARY KEY)");
        }
        connection.commit();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    void connectsAndChecksStatus() {
        ConnectionManager manager = new ConnectionManager(dataSource, 0);
        Connection c = manager.connect();
        assertTrue(manager.isConnected(c));
        manager.disconnect(c);
        assertFalse(manager.isConnected(c));
    }

    @Test
    void failingDataSourceExhaustsRetries() {
        JdbcDataSource bad = new JdbcDataSource();
        bad.setURL("jdbc:h2:tcp://localhost:1/nonexistent");
        ConnectionManager manager = new ConnectionManager(bad, 0);
        ConnectionException e = assertThrows(ConnectionException.class, manager::connect);
        assertEquals(12, e.getReturnCode());
    }

    @Test
    void commitsWhenFrequencyReachedOrForced() throws SQLException {
        CommitController controller = new CommitController(connection);
        controller.initialize();

        assertFalse(controller.commitIfDue(5, 10, false));
        assertTrue(controller.commitIfDue(10, 10, false));
        assertTrue(controller.commitIfDue(0, 10, true));
        assertEquals(2, controller.getCommitCount());
    }

    @Test
    void rollbackDiscardsUncommittedWork() throws SQLException {
        CommitController controller = new CommitController(connection);
        controller.initialize();

        insert(1);
        controller.rollback();
        assertEquals(0, count());
        assertEquals(1, controller.getRollbackCount());
    }

    @Test
    void savepointRestoreKeepsEarlierWork() throws SQLException {
        CommitController controller = new CommitController(connection);
        controller.initialize();

        insert(1);
        controller.savepoint("SP1");
        insert(2);
        controller.restore("SP1");
        controller.commit();

        assertEquals(1, count());
        assertEquals(1, controller.getSavepointCount());
    }

    @Test
    void restoringUnknownSavepointFails() {
        CommitController controller = new CommitController(connection);
        ConnectionException e =
                assertThrows(ConnectionException.class, () -> controller.restore("MISSING"));
        assertEquals(8, e.getReturnCode());
    }

    private void insert(int id) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO T VALUES (" + id + ")");
        }
    }

    private int count() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM T")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
