package com.cognition.clbs.common.copybook.db2;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;

/**
 * {@code WS-STATS-RECORD} from {@code DB2STAT.cbl}: one DB2 statistics sample per program run (96
 * bytes). This is the record layout the {@code DB2STATS} input of {@code RPTSTA00} carries; the
 * program references a {@code DB2STAT} copybook that was never checked in, so the collector's own
 * declaration is the source of truth.
 */
public record Db2StatisticsRecord(
    String programId,
    String startTime,
    String endTime,
    int rowsRead,
    int rowsInserted,
    int rowsUpdated,
    int rowsDeleted,
    int commits,
    int rollbacks,
    BigDecimal cpuTime,
    BigDecimal elapsedTime)
    implements CopybookRecord {

  public static final int LENGTH = 96;
  public static final int TIME_DIGITS = 11;
  public static final int TIME_SCALE = 2;

  /** Parses an ASCII-encoded record. */
  public static Db2StatisticsRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static Db2StatisticsRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, Db2StatisticsRecord::read);
  }

  /** Reads the record fields from a positioned reader. */
  public static Db2StatisticsRecord read(RecordReader in) {
    return new Db2StatisticsRecord(
        in.alnum(8),
        in.alnum(26),
        in.alnum(26),
        in.comp(9, true),
        in.comp(9, true),
        in.comp(9, true),
        in.comp(9, true),
        in.comp(9, true),
        in.comp(9, true),
        in.packed(TIME_DIGITS, TIME_SCALE),
        in.packed(TIME_DIGITS, TIME_SCALE));
  }

  /** Total DB2 calls represented by this sample: every row operation, commit and rollback. */
  public long totalCalls() {
    return (long) rowsRead + rowsInserted + rowsUpdated + rowsDeleted + commits + rollbacks;
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(programId, 8)
        .alnum(startTime, 26)
        .alnum(endTime, 26)
        .comp(rowsRead, 9, true)
        .comp(rowsInserted, 9, true)
        .comp(rowsUpdated, 9, true)
        .comp(rowsDeleted, 9, true)
        .comp(commits, 9, true)
        .comp(rollbacks, 9, true)
        .packed(cpuTime, TIME_DIGITS, TIME_SCALE)
        .packed(elapsedTime, TIME_DIGITS, TIME_SCALE);
  }
}
