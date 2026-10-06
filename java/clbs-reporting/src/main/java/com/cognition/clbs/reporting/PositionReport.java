package com.cognition.clbs.reporting;

import com.cognition.clbs.common.copybook.common.PositionRecord;
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
 * Java port of {@code RPTPOS00} (Daily Position Report Generator).
 *
 * <p>The report is the three heading lines followed by one {@code WS-POSITION-DETAIL} line per
 * {@code POSITION-MASTER} record, in file order. The COBOL source names three fields the {@code
 * POSREC} copybook does not define, so the port maps them to the closest real field and documents
 * the choice:
 *
 * <ul>
 *   <li>{@code POS-DESCRIPTION} → {@link PositionRecord#investmentId()} (the only descriptive field
 *       on the record);
 *   <li>{@code POS-CURRENT-VALUE} → {@link PositionRecord#marketValue()};
 *   <li>{@code POS-PREVIOUS-VALUE} → {@link PositionRecord#costBasis()}, so the change column is
 *       the unrealised gain/loss percentage {@code (market - cost) / cost * 100}, truncated to two
 *       decimals; a zero cost basis yields {@code 0.00} rather than a size error.
 * </ul>
 *
 * The transaction-history input and the summary paragraphs ({@code 2200-}, {@code 2300-}) are
 * declared but have no implementation in the COBOL program, so they produce no output here either.
 */
public final class PositionReport {

  static final String TITLE = "DAILY POSITION REPORT";
  static final NumericPicture QUANTITY = NumericPicture.of("ZZZ,ZZZ,ZZ9.99");
  static final NumericPicture VALUE = NumericPicture.of("$$$$,$$$,$$9.99");
  static final NumericPicture CHANGE_PCT = NumericPicture.of("+ZZ9.99");

  private static final int PORTFOLIO_WIDTH = 10;
  private static final int DESCRIPTION_WIDTH = 30;
  private static final int GAP = 2;
  private static final MathContext DIVIDE = new MathContext(38, RoundingMode.DOWN);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final LocalDate reportDate;
  private final List<Row> rows;

  public PositionReport(LocalDate reportDate, List<PositionRecord> positions) {
    this.reportDate = reportDate;
    List<Row> built = new ArrayList<>(positions.size());
    for (PositionRecord p : positions) {
      built.add(
          new Row(
              p.portfolioId(),
              p.investmentId(),
              p.quantity(),
              p.marketValue(),
              changePercent(p.marketValue(), p.costBasis())));
    }
    this.rows = List.copyOf(built);
  }

  static BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.signum() == 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return current
        .subtract(previous)
        .divide(previous, DIVIDE)
        .multiply(HUNDRED)
        .setScale(2, RoundingMode.DOWN);
  }

  public LocalDate reportDate() {
    return reportDate;
  }

  public List<Row> rows() {
    return rows;
  }

  /** The fixed-width report, record-equivalent to the COBOL {@code RPTFILE}. */
  public ReportDocument document() {
    List<String> lines = new ArrayList<>(ReportHeading.lines(TITLE, 40, 52, reportDate));
    for (Row row : rows) {
      lines.add(
          new ReportLine()
              .alnum(row.portfolioId(), PORTFOLIO_WIDTH)
              .spaces(GAP)
              .alnum(row.description(), DESCRIPTION_WIDTH)
              .spaces(GAP)
              .number(QUANTITY, row.quantity())
              .spaces(GAP)
              .number(VALUE, row.value())
              .spaces(GAP)
              .number(CHANGE_PCT, row.changePercent())
              .build());
    }
    return new ReportDocument(lines);
  }

  /** CSV with one row per position detail line. */
  public String csv() {
    Csv csv = new Csv().header("portfolio_id", "description", "quantity", "value", "change_pct");
    for (Row row : rows) {
      csv.row(
          row.portfolioId().trim(),
          row.description().trim(),
          row.quantity().toPlainString(),
          row.value().toPlainString(),
          row.changePercent().toPlainString());
    }
    return csv.toString();
  }

  /** JSON document with the report metadata and detail rows. */
  public String json() {
    Map<String, Object> root = Json.object();
    root.put("report", "RPTPOS00");
    root.put("title", TITLE);
    root.put("reportDate", reportDate.toString());
    List<Object> positions = new ArrayList<>(rows.size());
    for (Row row : rows) {
      Map<String, Object> o = Json.object();
      o.put("portfolioId", row.portfolioId().trim());
      o.put("description", row.description().trim());
      o.put("quantity", row.quantity());
      o.put("value", row.value());
      o.put("changePct", row.changePercent());
      positions.add(o);
    }
    root.put("positions", positions);
    return Json.write(root);
  }

  /** One detail line of the report. */
  public record Row(
      String portfolioId,
      String description,
      BigDecimal quantity,
      BigDecimal value,
      BigDecimal changePercent) {}
}
