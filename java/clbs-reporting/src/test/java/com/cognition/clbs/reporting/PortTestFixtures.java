package com.cognition.clbs.reporting;

import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.copybook.batch.BatchControlRecord;
import com.cognition.clbs.common.copybook.batch.BatchControlRecord.Prerequisite;
import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.copybook.db2.Db2StatisticsRecord;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * The {@code PORTTEST} data set: the deterministic report inputs shared by the Java golden tests
 * and the GnuCOBOL reference programs in {@code src/test/cobol}. {@link PortTestFixturesTest} keeps
 * the serialised copies under {@code src/test/resources/porttest} in sync with these definitions so
 * {@code cobol-parity.sh} and the Java tests always read identical bytes.
 */
final class PortTestFixtures {

  static final Path DIRECTORY = Paths.get("src", "test", "resources", "porttest");
  static final LocalDate REPORT_DATE = LocalDate.of(2024, 4, 9);
  static final String ACCEPT_DATE = "240409";

  private PortTestFixtures() {}

  static List<PositionRecord> positions() {
    return List.of(
        position("PORTTEST", "20240409", "IBM", "1500.0000", "187500.00", "262500.00", "A"),
        position("PORTTEST", "20240409", "MSFT", "250.5000", "100200.00", "105210.00", "A"),
        position("PORTTEST", "20240409", "AAPL", "0.0000", "0.00", "0.00", "C"),
        position("PORTTEST", "20240409", "GOOGL", "12345678.9999", "1000.00", "950.00", "A"),
        position("PORTTST2", "20240409", "TBILL2025", "100000.0000", "99000.00", "99000.00", "A"),
        position("PORTTST2", "20240409", "BRK.A", "2.0000", "1200000.00", "1234567.89", "A"),
        position("PORTTST2", "20240409", "XOM", "800.0000", "80000.00", "40000.00", "A"),
        position("PORTTST3", "20240409", "CASHUSD", "5000.0000", "0.00", "5000.00", "A"));
  }

  private static PositionRecord position(
      String portfolio,
      String date,
      String investment,
      String quantity,
      String cost,
      String market,
      String status) {
    return new PositionRecord(
        portfolio,
        date,
        investment,
        new BigDecimal(quantity),
        new BigDecimal(cost),
        new BigDecimal(market),
        "USD",
        status,
        "2024-04-09-06.00.00.000000",
        "BATCHUSR");
  }

  static List<AuditRecord> auditTrail() {
    return List.of(
        audit(
            "2024-04-09-06.00.01.000000",
            "PORTUPDT",
            "PROC",
            "START",
            "OK",
            "Daily position update started"),
        audit(
            "2024-04-09-06.00.45.250000",
            "TRNVAL00",
            "SEC",
            "LOGIN",
            "OK",
            "Operator BATCHUSR authenticated"),
        audit(
            "2024-04-09-06.01.10.500000",
            "POSUPDT",
            "DATA",
            "UPDATE",
            "WARN",
            "Position PORTTEST/GOOGL quantity exceeds 9,999,999 and will be truncated on the"
                + " daily report; verify holding"),
        audit(
            "2024-04-09-06.02.00.000000",
            "PORTUPDT",
            "PROC",
            "END",
            "OK",
            "Daily position update completed, RC=0"));
  }

  private static AuditRecord audit(
      String timestamp, String program, String type, String action, String status, String msg) {
    return new AuditRecord(
        timestamp,
        "CLBSPROD",
        "BATCHUSR",
        program,
        "BATCH",
        type,
        action,
        status,
        "PORTTEST",
        "ACCT000001",
        "",
        "",
        msg);
  }

  static List<ErrorLogRecord> errorLog() {
    return List.of(
        new ErrorLogRecord(
            "2024-04-09-06.00.30.000000",
            "TRNVAL00",
            "D",
            2,
            "VL0001",
            "Invalid transaction type 'XX' on PORTTST2 sequence 000042",
            "2024-04-09",
            "06.00.30",
            "BATCHUSR",
            "Transaction rejected"),
        new ErrorLogRecord(
            "2024-04-09-06.01.05.000000",
            "POSUPDT",
            "S",
            3,
            "SQL-811",
            "SQLCODE -811: more than one row returned for PORTTEST/AAPL in POSITION_HISTORY",
            "2024-04-09",
            "06.01.05",
            "BATCHUSR",
            "Cursor repositioned"),
        new ErrorLogRecord(
            "2024-04-09-06.01.59.000000",
            "RPTPOS00",
            "A",
            1,
            "PR01",
            "Position report: 8 positions printed, 1 with zero cost basis",
            "2024-04-09",
            "06.01.59",
            "BATCHUSR",
            ""));
  }

  static List<Db2StatisticsRecord> db2Statistics() {
    return List.of(
        new Db2StatisticsRecord(
            "PORTUPDT",
            "2024-04-09-06.00.01.000000",
            "2024-04-09-06.00.44.000000",
            12500,
            300,
            1200,
            15,
            120,
            2,
            new BigDecimal("12.75"),
            new BigDecimal("43.00")),
        new Db2StatisticsRecord(
            "POSUPDT",
            "2024-04-09-06.00.45.000000",
            "2024-04-09-06.01.58.000000",
            48000,
            8,
            7992,
            0,
            80,
            1,
            new BigDecimal("31.10"),
            new BigDecimal("73.25")),
        new Db2StatisticsRecord(
            "RPTPOS00",
            "2024-04-09-06.01.59.000000",
            "2024-04-09-06.02.00.000000",
            8,
            0,
            0,
            0,
            1,
            0,
            new BigDecimal("0.20"),
            new BigDecimal("1.00")));
  }

  static List<BatchControlRecord> batchControl() {
    return List.of(
        batch("PORTUPDT", 1, "D", "PORTUPDT", "060001", "060044", 0),
        batch("POSUPDT", 2, "D", "POSUPDT", "060045", "060158", 0),
        batch("TRNVAL", 3, "E", "TRNVAL00", "060030", "060031", 12),
        batch("RPTPOS", 4, "D", "RPTPOS00", "060159", "060200", 0),
        batch("RPTAUD", 5, "A", "RPTAUD00", "060201", "", 0),
        batch("RPTSTA", 6, "W", "RPTSTA00", "", "", 0),
        batch("HISTLD", 7, "R", "HISTLD00", "", "", 0));
  }

  private static BatchControlRecord batch(
      String job, int seq, String status, String program, String start, String end, int rc) {
    return new BatchControlRecord(
        job,
        "20240409",
        seq,
        status,
        "STEP010",
        program,
        start,
        end,
        0,
        Collections.nCopies(BatchControlRecord.PREREQ_OCCURS, Prerequisite.EMPTY),
        rc,
        rc == 0 ? "" : "Abend U0012 in TRNVAL00",
        0,
        "2024-04-09-06.00.00.000000",
        rc == 0 && "D".equals(status) ? "2024-04-09-06.02.00.000000" : "");
  }

  static byte[] serialise(List<? extends CopybookRecord> records) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (CopybookRecord record : records) {
      out.writeBytes(record.toBytes());
    }
    return out.toByteArray();
  }
}
