package com.cognition.clbs.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.reporting.format.ReportLine;
import com.cognition.clbs.testing.golden.GoldenFile;
import com.cognition.clbs.testing.golden.GoldenFileExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code RPTAUD00} parity. The {@code .dat} golden file is produced by {@code
 * src/test/cobol/RPTAUD00R.cbl} over the {@code PORTTEST} data set (see {@code cobol-parity.sh}).
 */
@ExtendWith(GoldenFileExtension.class)
class AuditReportTest {

  private final AuditReport report =
      new AuditReport(
          PortTestFixtures.REPORT_DATE, PortTestFixtures.auditTrail(), PortTestFixtures.errorLog());

  @Test
  void porttestReportFile(GoldenFile golden) {
    ReportDocument document = report.document();
    assertThat(document.lines())
        .hasSize(3 + PortTestFixtures.auditTrail().size() + PortTestFixtures.errorLog().size());
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
  void detailColumnsMatchCobolLayouts() {
    List<String> lines = report.document().lines();
    assertThat(lines.get(1)).startsWith(" ".repeat(40) + "SYSTEM AUDIT REPORT");
    String audit = lines.get(3);
    assertThat(audit.substring(0, 26)).isEqualTo("2024-04-09-06.00.01.000000");
    assertThat(audit.substring(28, 36)).isEqualTo("PORTUPDT");
    assertThat(audit.substring(38, 48)).isEqualTo("PROC      ");
    assertThat(audit.substring(50, 130)).startsWith("Daily position update started");
    assertThat(audit.substring(130)).isEqualTo("  ");

    String truncatedAudit = lines.get(5);
    assertThat(truncatedAudit.substring(50, 130))
        .isEqualTo(PortTestFixtures.auditTrail().get(2).message().substring(0, 80));

    String error = lines.get(8);
    assertThat(error.substring(28, 36)).isEqualTo("POSUPDT ");
    assertThat(error.substring(38, 42)).isEqualTo("SQL-");
    assertThat(error.substring(44, 124)).startsWith("SQLCODE -811");
    assertThat(report.reportDate()).isEqualTo(PortTestFixtures.REPORT_DATE);
    assertThat(report.auditRows()).hasSize(4);
    assertThat(report.errorRows()).hasSize(3);
  }
}
