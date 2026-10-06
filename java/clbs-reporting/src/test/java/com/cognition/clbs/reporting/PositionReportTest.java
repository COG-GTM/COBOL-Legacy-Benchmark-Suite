package com.cognition.clbs.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.reporting.format.ReportLine;
import com.cognition.clbs.testing.golden.GoldenFile;
import com.cognition.clbs.testing.golden.GoldenFileExtension;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code RPTPOS00} parity. The {@code .dat} golden files are produced by the GnuCOBOL reference
 * program {@code src/test/cobol/RPTPOS00R.cbl} over the {@code PORTTEST} data set (see {@code
 * cobol-parity.sh}); the Java report must be byte-for-byte identical.
 */
@ExtendWith(GoldenFileExtension.class)
class PositionReportTest {

  private final PositionReport report =
      new PositionReport(PortTestFixtures.REPORT_DATE, PortTestFixtures.positions());

  @Test
  void porttestReportFile(GoldenFile golden) {
    ReportDocument document = report.document();
    assertThat(document.lines()).hasSize(3 + PortTestFixtures.positions().size());
    assertThat(document.lines()).allSatisfy(line -> assertThat(line).hasSize(ReportLine.WIDTH));
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
  void headingsFollowCobolLayout() {
    List<String> lines = report.document().lines();
    assertThat(lines.get(0)).isEqualTo("*".repeat(132));
    assertThat(lines.get(1)).startsWith(" ".repeat(40) + "DAILY POSITION REPORT");
    assertThat(lines.get(2)).startsWith("REPORT DATE:   " + PortTestFixtures.ACCEPT_DATE + "    ");
    assertThat(report.reportDate()).isEqualTo(PortTestFixtures.REPORT_DATE);
  }

  @Test
  void detailLineColumnsMatchWsPositionDetail() {
    String ibm = report.document().lines().get(3);
    assertThat(ibm.substring(0, 10)).isEqualTo("PORTTEST  ");
    assertThat(ibm.substring(12, 42)).isEqualTo("IBM" + " ".repeat(27));
    assertThat(ibm.substring(44, 58)).isEqualTo("      1,500.00");
    assertThat(ibm.substring(60, 75)).isEqualTo("    $262,500.00");
    assertThat(ibm.substring(77, 84)).isEqualTo("+ 40.00");
    assertThat(ibm.substring(84)).isEqualTo(" ".repeat(48));
  }

  @Test
  void changePercentTruncatesAndHandlesZeroCostBasis() {
    assertThat(
            PositionReport.changePercent(new BigDecimal("105210.00"), new BigDecimal("100200.00")))
        .isEqualByComparingTo("5.00");
    assertThat(PositionReport.changePercent(new BigDecimal("950.00"), new BigDecimal("1000.00")))
        .isEqualByComparingTo("-5.00");
    assertThat(
            PositionReport.changePercent(
                new BigDecimal("1234567.89"), new BigDecimal("1200000.00")))
        .isEqualByComparingTo("2.88");
    assertThat(PositionReport.changePercent(new BigDecimal("5000.00"), BigDecimal.ZERO))
        .isEqualByComparingTo("0.00");
    assertThat(PositionReport.changePercent(new BigDecimal("1.00"), null))
        .isEqualByComparingTo("0");
  }

  @Test
  void rowsExposeMappedFields() {
    PositionRecord first = PortTestFixtures.positions().get(0);
    PositionReport.Row row = report.rows().get(0);
    assertThat(row.portfolioId()).isEqualTo(first.portfolioId());
    assertThat(row.description()).isEqualTo(first.investmentId());
    assertThat(row.quantity()).isEqualByComparingTo(first.quantity());
    assertThat(row.value()).isEqualByComparingTo(first.marketValue());
  }
}
