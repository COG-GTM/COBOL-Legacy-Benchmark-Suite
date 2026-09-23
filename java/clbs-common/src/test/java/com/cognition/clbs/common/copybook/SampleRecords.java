package com.cognition.clbs.common.copybook;

import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.common.HistoryRecord;
import com.cognition.clbs.common.copybook.common.PortfolioRecord;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.copybook.common.TransactionRecord;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.common.copybook.db2.PositionHistoryRecord;
import java.math.BigDecimal;

/** Representative records shared by the copybook and persistence round-trip tests. */
public final class SampleRecords {

  public static final String TIMESTAMP = "2024-03-15-14.30.45.123456";

  private SampleRecords() {}

  public static PortfolioRecord portfolio() {
    return new PortfolioRecord(
        "PORT0001",
        "ACC0000001",
        "JANE DOE",
        "I",
        20240101,
        20240315,
        "A",
        new BigDecimal("1234567.89"),
        new BigDecimal("-500.25"),
        "BATCHUSR",
        0);
  }

  public static PositionRecord position() {
    return new PositionRecord(
        "PORT0001",
        "20240315",
        "AAPL",
        new BigDecimal("1500.2500"),
        new BigDecimal("150000.00"),
        new BigDecimal("262500.75"),
        "USD",
        "A",
        TIMESTAMP,
        "POSUPDT");
  }

  public static TransactionRecord transaction() {
    return new TransactionRecord(
        "20240315",
        "143045",
        "PORT0001",
        "000042",
        "AAPL",
        "BU",
        new BigDecimal("100.0000"),
        new BigDecimal("175.1234"),
        new BigDecimal("17512.34"),
        "USD",
        "P",
        "",
        "");
  }

  public static HistoryRecord history() {
    return new HistoryRecord(
        "PORT0001",
        "20240315",
        "143045",
        "0001",
        "PO",
        "U",
        "BEFORE".repeat(10),
        "AFTER".repeat(80),
        "TRAN",
        TIMESTAMP,
        "HISTLOAD");
  }

  public static AuditRecord audit() {
    return new AuditRecord(
        TIMESTAMP,
        "CLBS",
        "OPERATOR",
        "PORTUPDT",
        "T001",
        "UPDT",
        "MODIFY",
        "OK",
        "PORT0001",
        "ACC0000001",
        "STATUS=P",
        "STATUS=A",
        "Portfolio activated");
  }

  public static PositionHistoryRecord positionHistory() {
    return new PositionHistoryRecord(
        "ACC00001",
        "PORT000001",
        "2024-03-15",
        "14.30.45",
        "SL",
        "US0378331005",
        new BigDecimal("250.500"),
        new BigDecimal("175.125"),
        new BigDecimal("43868.81"),
        new BigDecimal("9.99"),
        new BigDecimal("43858.82"),
        new BigDecimal("37575.00"),
        new BigDecimal("6283.82"),
        "2024-03-16",
        "02.00.00",
        "HISTLD00",
        "BATCH",
        TIMESTAMP);
  }

  public static ErrorLogRecord errorLog() {
    return new ErrorLogRecord(
        TIMESTAMP,
        "TRNVAL00",
        "D",
        ErrorLogRecord.Severity.ERROR.value(),
        "E001",
        "Invalid transaction type XX",
        "2024-03-15",
        "14.30.45",
        "BATCH",
        "record 42");
  }
}
