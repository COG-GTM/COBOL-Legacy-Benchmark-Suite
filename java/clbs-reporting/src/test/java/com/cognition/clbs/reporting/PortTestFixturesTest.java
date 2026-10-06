package com.cognition.clbs.reporting;

import com.cognition.clbs.common.copybook.batch.BatchControlRecord;
import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.copybook.db2.Db2StatisticsRecord;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.testing.golden.GoldenFiles;
import org.junit.jupiter.api.Test;

/**
 * Pins the serialised {@code PORTTEST} input files that {@code cobol-parity.sh} feeds to the
 * GnuCOBOL reference programs. Regenerate with {@code -Dclbs.golden.update=true} after changing
 * {@link PortTestFixtures}.
 */
class PortTestFixturesTest {

  @Test
  void posmstre() {
    GoldenFiles.assertMatchesRecords(
        PortTestFixtures.DIRECTORY.resolve("POSMSTRE.dat"),
        PortTestFixtures.serialise(PortTestFixtures.positions()),
        PositionRecord.LENGTH);
  }

  @Test
  void auditlog() {
    GoldenFiles.assertMatchesRecords(
        PortTestFixtures.DIRECTORY.resolve("AUDITLOG.dat"),
        PortTestFixtures.serialise(PortTestFixtures.auditTrail()),
        AuditRecord.LENGTH);
  }

  @Test
  void errlog() {
    GoldenFiles.assertMatchesRecords(
        PortTestFixtures.DIRECTORY.resolve("ERRLOG.dat"),
        PortTestFixtures.serialise(PortTestFixtures.errorLog()),
        ErrorLogRecord.LENGTH);
  }

  @Test
  void db2stats() {
    GoldenFiles.assertMatchesRecords(
        PortTestFixtures.DIRECTORY.resolve("DB2STATS.dat"),
        PortTestFixtures.serialise(PortTestFixtures.db2Statistics()),
        Db2StatisticsRecord.LENGTH);
  }

  @Test
  void bchstats() {
    GoldenFiles.assertMatchesRecords(
        PortTestFixtures.DIRECTORY.resolve("BCHSTATS.dat"),
        PortTestFixtures.serialise(PortTestFixtures.batchControl()),
        BatchControlRecord.LENGTH);
  }
}
