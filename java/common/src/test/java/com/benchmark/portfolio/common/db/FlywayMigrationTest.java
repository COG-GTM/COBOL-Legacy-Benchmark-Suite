package com.benchmark.portfolio.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Flyway baseline migration runs cleanly against a real
 * PostgreSQL instance and creates all tables expected from the VSAM copybooks.
 */
@Testcontainers
class FlywayMigrationTest {

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "portfolio_master",
            "portfolio_transaction",
            "portfolio_position",
            "history_record",
            "error_log",
            "audit_log");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migrationsRunCleanlyAndCreateExpectedTables() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(1);

        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            Set<String> tables = publicTables(conn);
            assertThat(tables).containsAll(EXPECTED_TABLES);
            for (String table : EXPECTED_TABLES) {
                assertThat(rowCount(conn, table)).as("table %s should be empty", table).isZero();
            }
        }
    }

    private Set<String> publicTables(Connection conn) throws Exception {
        Set<String> tables = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1).toLowerCase());
            }
        }
        return tables;
    }

    private long rowCount(Connection conn, String table) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
