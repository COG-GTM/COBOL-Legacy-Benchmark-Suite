package com.cognition.clbs.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Applies every migration to an embedded database and validates the JPA mappings against the
 * resulting schema ({@code ddl-auto=validate}), so a drift between DDL and entities fails here.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class FlywayMigrationTest {

  @Autowired private Flyway flyway;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void allMigrationsAppliedCleanly() {
    MigrationInfo[] applied = flyway.info().applied();

    assertThat(applied).hasSize(6);
    assertThat(applied).allMatch(info -> info.getState() == MigrationState.SUCCESS);
    assertThat(flyway.info().pending()).isEmpty();
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
  }

  @Test
  void db2TablesVsamTablesAndViewsExist() throws Exception {
    List<String> tables = new ArrayList<>();
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet rs =
          metaData.getTables(null, null, "%", new String[] {"BASE TABLE", "VIEW"})) {
        while (rs.next()) {
          tables.add(rs.getString("TABLE_NAME").toUpperCase());
        }
      }
    }

    assertThat(tables)
        .contains(
            "PORTFOLIO_MASTER",
            "INVESTMENT_POSITIONS",
            "TRANSACTION_HISTORY",
            "POSHIST",
            "ERRLOG",
            "RTNCODES",
            "PORTFOLIO",
            "POSITION_MASTER",
            "PORTFOLIO_TRANSACTION",
            "CHANGE_HISTORY",
            "AUDIT_LOG",
            "ACTIVE_PORTFOLIOS",
            "CURRENT_POSITIONS");
  }

  @Test
  void viewsAreQueryable() {
    assertThat(jdbc.queryForObject("select count(*) from ACTIVE_PORTFOLIOS", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from CURRENT_POSITIONS", Integer.class))
        .isZero();
  }

  @Test
  void poshistDefaultsMirrorDb2WithDefaultClauses() {
    jdbc.update(
        """
        insert into POSHIST (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME, TRANS_TYPE,
          SECURITY_ID, QUANTITY, PRICE, AMOUNT, TOTAL_AMOUNT, COST_BASIS, GAIN_LOSS,
          PROCESS_DATE, PROCESS_TIME, PROGRAM_ID, USER_ID)
        values ('ACC00001', 'PORT000001', DATE '2024-03-15', TIME '14:30:45', 'BU',
          'US0378331005', 1, 2, 3, 4, 5, 6, DATE '2024-03-16', TIME '02:00:00', 'HISTLD00', 'BATCH')
        """);

    assertThat(jdbc.queryForObject("select FEES from POSHIST", java.math.BigDecimal.class))
        .isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("select AUDIT_TIMESTAMP from POSHIST", java.sql.Timestamp.class))
        .isNotNull();
  }
}
