package com.cognition.clbs.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.reporting.format.ReportLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportingCliTest {

  private static final Path PORTTEST = PortTestFixtures.DIRECTORY;

  @TempDir Path out;
  private ByteArrayOutputStream stderr;
  private ReportingCli cli;

  @BeforeEach
  void setUp() {
    stderr = new ByteArrayOutputStream();
    cli = new ReportingCli(new PrintStream(stderr, true, StandardCharsets.UTF_8));
  }

  @Test
  void rptposWritesReportAndOptionalOutputs() throws IOException {
    int rc =
        cli.run(
            new String[] {
              "rptpos",
              "--posmstre",
              PORTTEST.resolve("POSMSTRE.dat").toString(),
              "--rptfile",
              out.resolve("RPTFILE").toString(),
              "--text",
              out.resolve("report.txt").toString(),
              "--csv",
              out.resolve("report.csv").toString(),
              "--json",
              out.resolve("report.json").toString(),
              "--date",
              PortTestFixtures.REPORT_DATE.toString()
            });
    assertThat(rc).isZero();
    assertThat(Files.readAllBytes(out.resolve("RPTFILE")))
        .isEqualTo(
            new PositionReport(PortTestFixtures.REPORT_DATE, PortTestFixtures.positions())
                .document()
                .records());
    assertThat(Files.readString(out.resolve("report.txt")).split("\n"))
        .hasSize(11)
        .allSatisfy(line -> assertThat(line).hasSize(ReportLine.WIDTH));
    assertThat(Files.readString(out.resolve("report.csv"))).startsWith("portfolio_id,");
    assertThat(Files.readString(out.resolve("report.json"))).startsWith("{\"report\":\"RPTPOS00\"");
  }

  @Test
  void rptaudAndRptstaRunFromPorttestInputs() throws IOException {
    int aud =
        cli.run(
            new String[] {
              "RPTAUD",
              "--auditlog",
              PORTTEST.resolve("AUDITLOG.dat").toString(),
              "--errlog",
              PORTTEST.resolve("ERRLOG.dat").toString(),
              "--rptfile",
              out.resolve("AUD").toString(),
              "--date",
              "2024-04-09"
            });
    int sta =
        cli.run(
            new String[] {
              "rptsta",
              "--db2stats",
              PORTTEST.resolve("DB2STATS.dat").toString(),
              "--bchstats",
              PORTTEST.resolve("BCHSTATS.dat").toString(),
              "--rptfile",
              out.resolve("STA").toString(),
              "--date",
              "2024-04-09"
            });
    assertThat(aud).isZero();
    assertThat(sta).isZero();
    assertThat(Files.size(out.resolve("AUD"))).isEqualTo(10L * ReportLine.WIDTH);
    assertThat(Files.size(out.resolve("STA"))).isEqualTo(5L * ReportLine.WIDTH);
  }

  @Test
  void defaultsToTodayWhenNoDateGiven() throws IOException {
    int rc =
        cli.run(
            new String[] {
              "rptpos",
              "--posmstre",
              PORTTEST.resolve("POSMSTRE.dat").toString(),
              "--rptfile",
              out.resolve("RPTFILE").toString()
            });
    assertThat(rc).isZero();
    assertThat(Files.size(out.resolve("RPTFILE"))).isEqualTo(11L * ReportLine.WIDTH);
  }

  @Test
  void returnsRc12OnUsageErrors() {
    assertThat(cli.run(new String[0])).isEqualTo(ReportingCli.RC_ERROR);
    assertThat(cli.run(new String[] {"rptxyz"})).isEqualTo(ReportingCli.RC_ERROR);
    assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("Unknown report 'rptxyz'");
    assertThat(cli.run(new String[] {"rptpos", "--posmstre"})).isEqualTo(ReportingCli.RC_ERROR);
    assertThat(cli.run(new String[] {"rptpos", "--rptfile", "x"})).isEqualTo(ReportingCli.RC_ERROR);
    assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("--posmstre");
    assertThat(
            cli.run(
                new String[] {
                  "rptpos", "--posmstre", "missing.dat", "--rptfile", out.resolve("r").toString()
                }))
        .isEqualTo(ReportingCli.RC_ERROR);
    assertThat(
            cli.run(
                new String[] {
                  "rptpos",
                  "--posmstre",
                  PORTTEST.resolve("POSMSTRE.dat").toString(),
                  "--rptfile",
                  out.resolve("r").toString(),
                  "--date",
                  "not-a-date"
                }))
        .isEqualTo(ReportingCli.RC_ERROR);
  }
}
