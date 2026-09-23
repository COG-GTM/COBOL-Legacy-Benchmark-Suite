package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code HISTORY-RECORD} from {@code HISTREC.cpy}: the VSAM change-history record (917 bytes, key =
 * portfolio + date + time + sequence) holding 400-byte before/after images of the changed record.
 *
 * @param portfolioId {@code HIST-PORTFOLIO-ID PIC X(08)}
 * @param historyDate {@code HIST-DATE PIC X(08)} as {@code YYYYMMDD}
 * @param historyTime {@code HIST-TIME PIC X(06)} as {@code HHMMSS}
 * @param sequenceNumber {@code HIST-SEQ-NO PIC X(04)}
 * @param recordType {@code HIST-RECORD-TYPE PIC X(02)}
 * @param actionCode {@code HIST-ACTION-CODE PIC X(01)}
 * @param beforeImage {@code HIST-BEFORE-IMAGE PIC X(400)}
 * @param afterImage {@code HIST-AFTER-IMAGE PIC X(400)}
 * @param reasonCode {@code HIST-REASON-CODE PIC X(04)}
 * @param processTimestamp {@code HIST-PROCESS-DATE PIC X(26)} DB2 timestamp
 * @param processUser {@code HIST-PROCESS-USER PIC X(08)}
 */
public record HistoryRecord(
    String portfolioId,
    String historyDate,
    String historyTime,
    String sequenceNumber,
    String recordType,
    String actionCode,
    String beforeImage,
    String afterImage,
    String reasonCode,
    String processTimestamp,
    String processUser)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 917;

  /** Key length in bytes ({@code HIST-KEY}). */
  public static final int KEY_LENGTH = 26;

  /** Width of {@code HIST-BEFORE-IMAGE} / {@code HIST-AFTER-IMAGE}. */
  public static final int IMAGE_LENGTH = 400;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static HistoryRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static HistoryRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, HistoryRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static HistoryRecord read(RecordReader in) {
    HistoryRecord record =
        new HistoryRecord(
            in.alnum(8),
            in.alnum(8),
            in.alnum(6),
            in.alnum(4),
            in.alnum(2),
            in.alnum(1),
            in.alnum(IMAGE_LENGTH),
            in.alnum(IMAGE_LENGTH),
            in.alnum(4),
            in.alnum(26),
            in.alnum(8));
    in.skip(50);
    return record;
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(portfolioId, 8)
        .alnum(historyDate, 8)
        .alnum(historyTime, 6)
        .alnum(sequenceNumber, 4)
        .alnum(recordType, 2)
        .alnum(actionCode, 1)
        .alnum(beforeImage, IMAGE_LENGTH)
        .alnum(afterImage, IMAGE_LENGTH)
        .alnum(reasonCode, 4)
        .alnum(processTimestamp, 26)
        .alnum(processUser, 8)
        .filler(50);
  }
}
