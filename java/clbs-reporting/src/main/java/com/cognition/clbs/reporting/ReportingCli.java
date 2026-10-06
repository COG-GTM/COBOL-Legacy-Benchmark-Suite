package com.cognition.clbs.reporting;

import com.cognition.clbs.common.copybook.batch.BatchControlRecord;
import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.copybook.db2.Db2StatisticsRecord;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.reporting.io.FixedRecordFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Command-line entry point that stands in for the {@code RPTPOS}, {@code RPTAUD} and {@code RPTSTA}
 * job steps. Options are named after the JCL DD statements they replace:
 *
 * <pre>
 * rptpos --posmstre positions.dat --rptfile report.dat [--csv out.csv] [--json out.json]
 * rptaud --auditlog audit.dat --errlog errors.dat --rptfile report.dat [--csv ...] [--json ...]
 * rptsta --db2stats db2.dat --bchstats batch.dat --rptfile report.dat [--csv ...] [--json ...]
 * </pre>
 *
 * {@code --date yyyy-MM-dd} overrides the run date (default: today). {@code --rptfile} receives the
 * fixed-width 132-byte records; {@code --text} additionally writes a newline-delimited copy.
 */
public final class ReportingCli {

  /** Return code used by the COBOL programs' {@code 9999-ERROR-HANDLER}. */
  static final int RC_ERROR = 12;

  private final PrintStream err;

  ReportingCli(PrintStream err) {
    this.err = err;
  }

  public static void main(String[] args) {
    System.exit(new ReportingCli(System.err).run(args));
  }

  int run(String[] args) {
    if (args.length == 0) {
      err.println(usage());
      return RC_ERROR;
    }
    try {
      Map<String, String> options = parseOptions(args);
      LocalDate date =
          options.containsKey("date") ? LocalDate.parse(options.get("date")) : LocalDate.now();
      ReportDocument document;
      String csv;
      String json;
      switch (args[0].toLowerCase()) {
        case "rptpos" -> {
          PositionReport report =
              new PositionReport(
                  date,
                  FixedRecordFile.readAll(
                      required(options, "posmstre"), PositionRecord.LENGTH, PositionRecord::parse));
          document = report.document();
          csv = report.csv();
          json = report.json();
        }
        case "rptaud" -> {
          AuditReport report =
              new AuditReport(
                  date,
                  FixedRecordFile.readAll(
                      required(options, "auditlog"), AuditRecord.LENGTH, AuditRecord::parse),
                  FixedRecordFile.readAll(
                      required(options, "errlog"), ErrorLogRecord.LENGTH, ErrorLogRecord::parse));
          document = report.document();
          csv = report.csv();
          json = report.json();
        }
        case "rptsta" -> {
          StatisticsReport report =
              new StatisticsReport(
                  date,
                  FixedRecordFile.readAll(
                      required(options, "db2stats"),
                      Db2StatisticsRecord.LENGTH,
                      Db2StatisticsRecord::parse),
                  FixedRecordFile.readAll(
                      required(options, "bchstats"),
                      BatchControlRecord.LENGTH,
                      BatchControlRecord::parse));
          document = report.document();
          csv = report.csv();
          json = report.json();
        }
        default -> {
          err.println("Unknown report '" + args[0] + "'. " + usage());
          return RC_ERROR;
        }
      }
      Files.write(required(options, "rptfile"), document.records());
      if (options.containsKey("text")) {
        Files.writeString(Path.of(options.get("text")), document.text(), StandardCharsets.UTF_8);
      }
      if (options.containsKey("csv")) {
        Files.writeString(Path.of(options.get("csv")), csv, StandardCharsets.UTF_8);
      }
      if (options.containsKey("json")) {
        Files.writeString(Path.of(options.get("json")), json, StandardCharsets.UTF_8);
      }
      return 0;
    } catch (IOException | IllegalArgumentException | java.time.DateTimeException e) {
      err.println(e.getMessage());
      return RC_ERROR;
    }
  }

  private static Map<String, String> parseOptions(String[] args) {
    Map<String, String> options = new HashMap<>();
    for (int i = 1; i < args.length; i++) {
      String arg = args[i];
      if (!arg.startsWith("--") || i + 1 >= args.length) {
        throw new IllegalArgumentException(
            "Expected --option value, got '" + arg + "'. " + usage());
      }
      options.put(arg.substring(2).toLowerCase(), args[++i]);
    }
    return options;
  }

  private static Path required(Map<String, String> options, String name) {
    String value = options.get(name);
    if (value == null) {
      throw new IllegalArgumentException("Missing required option --" + name + ". " + usage());
    }
    return Path.of(value);
  }

  static String usage() {
    return "Usage: rptpos --posmstre F --rptfile F | rptaud --auditlog F --errlog F --rptfile F"
        + " | rptsta --db2stats F --bchstats F --rptfile F [--date yyyy-MM-dd] [--text F]"
        + " [--csv F] [--json F]";
  }
}
