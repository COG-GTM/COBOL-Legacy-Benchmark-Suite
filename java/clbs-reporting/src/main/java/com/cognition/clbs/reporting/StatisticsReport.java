package com.cognition.clbs.reporting;

import com.cognition.clbs.common.copybook.batch.BatchConstants;
import com.cognition.clbs.common.copybook.batch.BatchControlRecord;
import com.cognition.clbs.common.copybook.db2.Db2StatisticsRecord;
import com.cognition.clbs.reporting.format.Csv;
import com.cognition.clbs.reporting.format.Json;
import com.cognition.clbs.reporting.format.NumericPicture;
import com.cognition.clbs.reporting.format.ReportLine;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java port of {@code RPTSTA00} (System Statistics Report Generator).
 *
 * <p>The report is the three heading lines followed by the {@code WS-DB2-DETAIL} and {@code
 * WS-BATCH-DETAIL} lines. Accumulation mirrors {@code WS-PERFORMANCE-METRICS}:
 *
 * <ul>
 *   <li>{@code WS-DB2-CALLS} is the sum of {@link Db2StatisticsRecord#totalCalls()} over every
 *       {@code DB2STATS} record and {@code WS-DB2-ELAPSED} the sum of their elapsed times; the
 *       average response is elapsed / calls truncated to three decimals ({@code 0.000} when there
 *       were no calls);
 *   <li>{@code WS-BATCH-JOBS} counts every {@code BCHSTATS} record, {@code WS-BATCH-SUCCESS} those
 *       with {@code BCT-STATUS-DONE} and {@code WS-BATCH-FAILED} those with {@code
 *       BCT-STATUS-ERROR}; the success rate is success / jobs * 100 truncated to two decimals
 *       ({@code 0.00} when there were no jobs).
 * </ul>
 *
 * The trend-analysis paragraph ({@code 2430-}) is declared but not implemented in COBOL and
 * produces no output.
 */
public final class StatisticsReport {

  static final String TITLE = "SYSTEM STATISTICS AND PERFORMANCE REPORT";
  static final NumericPicture CALLS = NumericPicture.of("ZZZ,ZZZ,ZZ9");
  static final NumericPicture AVG_RESPONSE = NumericPicture.of("ZZ,ZZ9.999");
  static final NumericPicture JOBS = NumericPicture.of("ZZZ,ZZ9");
  static final NumericPicture SUCCESS_RATE = NumericPicture.of("ZZ9.99");

  private static final int LABEL_WIDTH = 20;
  private static final MathContext DIVIDE = new MathContext(38, RoundingMode.DOWN);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final LocalDate reportDate;
  private final Db2Metrics db2;
  private final BatchMetrics batch;

  public StatisticsReport(
      LocalDate reportDate,
      List<Db2StatisticsRecord> db2Statistics,
      List<BatchControlRecord> batchStatistics) {
    this.reportDate = reportDate;
    this.db2 = accumulateDb2(db2Statistics);
    this.batch = accumulateBatch(batchStatistics);
  }

  static Db2Metrics accumulateDb2(List<Db2StatisticsRecord> samples) {
    long calls = 0;
    BigDecimal elapsed = BigDecimal.ZERO.setScale(2);
    BigDecimal cpu = BigDecimal.ZERO.setScale(2);
    for (Db2StatisticsRecord s : samples) {
      calls += s.totalCalls();
      elapsed = elapsed.add(s.elapsedTime());
      cpu = cpu.add(s.cpuTime());
    }
    BigDecimal average =
        calls == 0
            ? BigDecimal.ZERO.setScale(3)
            : elapsed.divide(BigDecimal.valueOf(calls), DIVIDE).setScale(3, RoundingMode.DOWN);
    return new Db2Metrics(calls, elapsed, cpu, average);
  }

  static BatchMetrics accumulateBatch(List<BatchControlRecord> jobs) {
    long total = 0;
    long successful = 0;
    long failed = 0;
    for (BatchControlRecord job : jobs) {
      total++;
      if (BatchConstants.Status.DONE.code().equals(job.status())) {
        successful++;
      } else if (BatchConstants.Status.ERROR.code().equals(job.status())) {
        failed++;
      }
    }
    BigDecimal rate =
        total == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(successful)
                .divide(BigDecimal.valueOf(total), DIVIDE)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.DOWN);
    return new BatchMetrics(total, successful, failed, rate);
  }

  public LocalDate reportDate() {
    return reportDate;
  }

  public Db2Metrics db2() {
    return db2;
  }

  public BatchMetrics batch() {
    return batch;
  }

  /** The fixed-width report, record-equivalent to the COBOL {@code RPTFILE}. */
  public ReportDocument document() {
    List<String> lines = new ArrayList<>(ReportHeading.lines(TITLE, 35, 62, reportDate));
    lines.add(
        new ReportLine()
            .alnum("DB2 CALLS:", LABEL_WIDTH)
            .number(CALLS, db2.calls())
            .spaces(20)
            .alnum("AVG RESPONSE:", LABEL_WIDTH)
            .number(AVG_RESPONSE, db2.averageResponse())
            .build());
    lines.add(
        new ReportLine()
            .alnum("BATCH JOBS:", LABEL_WIDTH)
            .number(JOBS, batch.jobs())
            .spaces(10)
            .alnum("SUCCESS RATE:", LABEL_WIDTH)
            .number(SUCCESS_RATE, batch.successRate())
            .alnum("%", 5)
            .build());
    return new ReportDocument(lines);
  }

  /** CSV with one {@code metric,value} row per accumulated figure. */
  public String csv() {
    return new Csv()
        .header("metric", "value")
        .row("db2_calls", db2.calls())
        .row("db2_elapsed_seconds", db2.elapsed().toPlainString())
        .row("db2_cpu_seconds", db2.cpu().toPlainString())
        .row("db2_avg_response_seconds", db2.averageResponse().toPlainString())
        .row("batch_jobs", batch.jobs())
        .row("batch_successful", batch.successful())
        .row("batch_failed", batch.failed())
        .row("batch_success_rate_pct", batch.successRate().toPlainString())
        .toString();
  }

  /** JSON document with nested {@code db2} and {@code batch} objects. */
  public String json() {
    Map<String, Object> root = Json.object();
    root.put("report", "RPTSTA00");
    root.put("title", TITLE);
    root.put("reportDate", reportDate.toString());
    Map<String, Object> db2Json = Json.object();
    db2Json.put("calls", db2.calls());
    db2Json.put("elapsedSeconds", db2.elapsed());
    db2Json.put("cpuSeconds", db2.cpu());
    db2Json.put("avgResponseSeconds", db2.averageResponse());
    Map<String, Object> batchJson = Json.object();
    batchJson.put("jobs", batch.jobs());
    batchJson.put("successful", batch.successful());
    batchJson.put("failed", batch.failed());
    batchJson.put("successRatePct", batch.successRate());
    root.put("db2", db2Json);
    root.put("batch", batchJson);
    return Json.write(root);
  }

  /** Accumulated DB2 metrics ({@code WS-DB2-METRICS}). */
  public record Db2Metrics(
      long calls, BigDecimal elapsed, BigDecimal cpu, BigDecimal averageResponse) {}

  /** Accumulated batch metrics ({@code WS-BATCH-METRICS}). */
  public record BatchMetrics(long jobs, long successful, long failed, BigDecimal successRate) {}
}
