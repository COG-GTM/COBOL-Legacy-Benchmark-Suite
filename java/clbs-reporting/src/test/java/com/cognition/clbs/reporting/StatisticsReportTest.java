package com.cognition.clbs.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.common.copybook.batch.BatchControlRecord;
import com.cognition.clbs.common.copybook.db2.Db2StatisticsRecord;
import com.cognition.clbs.reporting.format.ReportLine;
import com.cognition.clbs.testing.golden.GoldenFile;
import com.cognition.clbs.testing.golden.GoldenFileExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code RPTSTA00} parity. The {@code .dat} golden file is produced by {@code
 * src/test/cobol/RPTSTA00R.cbl} over the {@code PORTTEST} data set (see {@code cobol-parity.sh}).
 */
@ExtendWith(GoldenFileExtension.class)
class StatisticsReportTest {

  private final StatisticsReport report =
      new StatisticsReport(
          PortTestFixtures.REPORT_DATE,
          PortTestFixtures.db2Statistics(),
          PortTestFixtures.batchControl());

  @Test
  void porttestReportFile(GoldenFile golden) {
    ReportDocument document = report.document();
    assertThat(document.lines()).hasSize(5);
    golden.assertRecords(document.records(), ReportLine.WIDTH);
  }

  @Test
  void porttestReportText(GoldenFile golden) {
    golden.assertText(report.document().text());
  }

  @Test
  void porttestCsv(GoldenFile golden) {
    golden.assertText("porttestCsv.csv", report.csv());
  }

  @Test
  void porttestJson(GoldenFile golden) {
    golden.assertText("porttestJson.json", report.json());
  }

  @Test
  void accumulatesMetricsLikeWsPerformanceMetrics() {
    StatisticsReport.Db2Metrics db2 = report.db2();
    assertThat(db2.calls()).isEqualTo(14137L + 56081L + 9L);
    assertThat(db2.elapsed()).isEqualByComparingTo("117.25");
    assertThat(db2.cpu()).isEqualByComparingTo("44.05");
    assertThat(db2.averageResponse()).isEqualByComparingTo("0.001");

    StatisticsReport.BatchMetrics batch = report.batch();
    assertThat(batch.jobs()).isEqualTo(7);
    assertThat(batch.successful()).isEqualTo(3);
    assertThat(batch.failed()).isEqualTo(1);
    assertThat(batch.successRate()).isEqualByComparingTo("42.85");
    assertThat(report.reportDate()).isEqualTo(PortTestFixtures.REPORT_DATE);
  }

  @Test
  void detailLinesMatchCobolLayouts() {
    List<String> lines = report.document().lines();
    assertThat(lines.get(1))
        .startsWith(" ".repeat(35) + "SYSTEM STATISTICS AND PERFORMANCE REPORT");
    String db2 = lines.get(3);
    assertThat(db2.substring(0, 20)).isEqualTo("DB2 CALLS:          ");
    assertThat(db2.substring(20, 31)).isEqualTo("     70,227");
    assertThat(db2.substring(51, 71)).isEqualTo("AVG RESPONSE:       ");
    assertThat(db2.substring(71, 81)).isEqualTo("     0.001");
    assertThat(db2.substring(81)).isEqualTo(" ".repeat(51));
    String batch = lines.get(4);
    assertThat(batch.substring(0, 20)).isEqualTo("BATCH JOBS:         ");
    assertThat(batch.substring(20, 27)).isEqualTo("      7");
    assertThat(batch.substring(37, 57)).isEqualTo("SUCCESS RATE:       ");
    assertThat(batch.substring(57, 63)).isEqualTo(" 42.85");
    assertThat(batch.substring(63, 68)).isEqualTo("%    ");
  }

  @Test
  void emptyInputsYieldZeroMetrics() {
    StatisticsReport empty =
        new StatisticsReport(
            PortTestFixtures.REPORT_DATE,
            List.<Db2StatisticsRecord>of(),
            List.<BatchControlRecord>of());
    assertThat(empty.db2().calls()).isZero();
    assertThat(empty.db2().averageResponse()).isEqualByComparingTo("0");
    assertThat(empty.batch().successRate()).isEqualByComparingTo("0");
    assertThat(empty.document().lines().get(3).substring(71, 81)).isEqualTo("     0.000");
    assertThat(empty.document().lines().get(4).substring(57, 63)).isEqualTo("  0.00");
  }
}
