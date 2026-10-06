package com.cognition.clbs.reporting;

import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.reporting.format.Csv;
import com.cognition.clbs.reporting.format.Json;
import com.cognition.clbs.reporting.format.ReportLine;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java port of {@code RPTAUD00} (Audit Report Generator).
 *
 * <p>The report is the three heading lines, one {@code WS-AUDIT-DETAIL} line per {@code AUDITLOG}
 * record and then one {@code WS-ERROR-DETAIL} line per {@code ERRLOG} record, each in file order.
 * The error input is the {@code ERROR_LOG} row layout ({@link ErrorLogRecord}); the COBOL program
 * copies {@code ERRHAND} into its FILE SECTION, which only defines constants, so the DB2 error-log
 * record is the only error record layout the system actually writes. Fields wider than their report
 * column ({@code AUD-MESSAGE}, {@code ERR-CODE}, {@code ERR-MESSAGE}) are truncated on the right,
 * as a COBOL {@code MOVE} does. The summary paragraphs ({@code 2120-}, {@code 2220-}, {@code
 * 2300-}) are declared but not implemented in COBOL and produce no output.
 */
public final class AuditReport {

  static final String TITLE = "SYSTEM AUDIT REPORT";

  private static final int TIMESTAMP_WIDTH = 26;
  private static final int PROGRAM_WIDTH = 8;
  private static final int TYPE_WIDTH = 10;
  private static final int CODE_WIDTH = 4;
  private static final int MESSAGE_WIDTH = 80;
  private static final int GAP = 2;

  private final LocalDate reportDate;
  private final List<AuditRow> auditRows;
  private final List<ErrorRow> errorRows;

  public AuditReport(
      LocalDate reportDate, List<AuditRecord> auditTrail, List<ErrorLogRecord> errorLog) {
    this.reportDate = reportDate;
    List<AuditRow> audits = new ArrayList<>(auditTrail.size());
    for (AuditRecord a : auditTrail) {
      audits.add(new AuditRow(a.timestamp(), a.program(), a.type(), a.message()));
    }
    List<ErrorRow> errors = new ArrayList<>(errorLog.size());
    for (ErrorLogRecord e : errorLog) {
      errors.add(new ErrorRow(e.errorTimestamp(), e.programId(), e.errorCode(), e.errorMessage()));
    }
    this.auditRows = List.copyOf(audits);
    this.errorRows = List.copyOf(errors);
  }

  public LocalDate reportDate() {
    return reportDate;
  }

  public List<AuditRow> auditRows() {
    return auditRows;
  }

  public List<ErrorRow> errorRows() {
    return errorRows;
  }

  /** The fixed-width report, record-equivalent to the COBOL {@code RPTFILE}. */
  public ReportDocument document() {
    List<String> lines = new ArrayList<>(ReportHeading.lines(TITLE, 40, 52, reportDate));
    for (AuditRow row : auditRows) {
      lines.add(
          new ReportLine()
              .alnum(row.timestamp(), TIMESTAMP_WIDTH)
              .spaces(GAP)
              .alnum(row.program(), PROGRAM_WIDTH)
              .spaces(GAP)
              .alnum(row.type(), TYPE_WIDTH)
              .spaces(GAP)
              .alnum(row.message(), MESSAGE_WIDTH)
              .build());
    }
    for (ErrorRow row : errorRows) {
      lines.add(
          new ReportLine()
              .alnum(row.timestamp(), TIMESTAMP_WIDTH)
              .spaces(GAP)
              .alnum(row.program(), PROGRAM_WIDTH)
              .spaces(GAP)
              .alnum(row.code(), CODE_WIDTH)
              .spaces(GAP)
              .alnum(row.message(), MESSAGE_WIDTH)
              .build());
    }
    return new ReportDocument(lines);
  }

  /** CSV with a {@code section} column distinguishing audit and error rows. */
  public String csv() {
    Csv csv = new Csv().header("section", "timestamp", "program", "type_or_code", "message");
    for (AuditRow row : auditRows) {
      csv.row(
          "AUDIT",
          row.timestamp().trim(),
          row.program().trim(),
          row.type().trim(),
          row.message().trim());
    }
    for (ErrorRow row : errorRows) {
      csv.row(
          "ERROR",
          row.timestamp().trim(),
          row.program().trim(),
          row.code().trim(),
          row.message().trim());
    }
    return csv.toString();
  }

  /** JSON document with separate {@code audit} and {@code errors} arrays. */
  public String json() {
    Map<String, Object> root = Json.object();
    root.put("report", "RPTAUD00");
    root.put("title", TITLE);
    root.put("reportDate", reportDate.toString());
    List<Object> audit = new ArrayList<>(auditRows.size());
    for (AuditRow row : auditRows) {
      Map<String, Object> o = Json.object();
      o.put("timestamp", row.timestamp().trim());
      o.put("program", row.program().trim());
      o.put("type", row.type().trim());
      o.put("message", row.message().trim());
      audit.add(o);
    }
    List<Object> errors = new ArrayList<>(errorRows.size());
    for (ErrorRow row : errorRows) {
      Map<String, Object> o = Json.object();
      o.put("timestamp", row.timestamp().trim());
      o.put("program", row.program().trim());
      o.put("code", row.code().trim());
      o.put("message", row.message().trim());
      errors.add(o);
    }
    root.put("audit", audit);
    root.put("errors", errors);
    return Json.write(root);
  }

  /** One audit-trail detail line. */
  public record AuditRow(String timestamp, String program, String type, String message) {}

  /** One error-log detail line. */
  public record ErrorRow(String timestamp, String program, String code, String message) {}
}
