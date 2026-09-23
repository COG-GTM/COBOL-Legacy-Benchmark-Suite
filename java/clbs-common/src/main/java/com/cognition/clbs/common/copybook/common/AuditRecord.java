package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code AUDIT-RECORD} from {@code AUDITLOG.cpy}: one line of the sequential audit file written by
 * AUDPROC (392 bytes).
 *
 * @param timestamp {@code AUD-TIMESTAMP PIC X(26)}
 * @param systemId {@code AUD-SYSTEM-ID PIC X(8)}
 * @param userId {@code AUD-USER-ID PIC X(8)}
 * @param program {@code AUD-PROGRAM PIC X(8)}
 * @param terminal {@code AUD-TERMINAL PIC X(8)}
 * @param type {@code AUD-TYPE PIC X(4)}
 * @param action {@code AUD-ACTION PIC X(8)}
 * @param status {@code AUD-STATUS PIC X(4)}
 * @param portfolioId {@code AUD-PORTFOLIO-ID PIC X(8)}
 * @param accountNumber {@code AUD-ACCOUNT-NO PIC X(10)}
 * @param beforeImage {@code AUD-BEFORE-IMAGE PIC X(100)}
 * @param afterImage {@code AUD-AFTER-IMAGE PIC X(100)}
 * @param message {@code AUD-MESSAGE PIC X(100)}
 */
public record AuditRecord(
    String timestamp,
    String systemId,
    String userId,
    String program,
    String terminal,
    String type,
    String action,
    String status,
    String portfolioId,
    String accountNumber,
    String beforeImage,
    String afterImage,
    String message)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 392;

  /** Width of the before/after images and the message. */
  public static final int IMAGE_LENGTH = 100;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static AuditRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static AuditRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, AuditRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static AuditRecord read(RecordReader in) {
    return new AuditRecord(
        in.alnum(26),
        in.alnum(8),
        in.alnum(8),
        in.alnum(8),
        in.alnum(8),
        in.alnum(4),
        in.alnum(8),
        in.alnum(4),
        in.alnum(8),
        in.alnum(10),
        in.alnum(IMAGE_LENGTH),
        in.alnum(IMAGE_LENGTH),
        in.alnum(IMAGE_LENGTH));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(timestamp, 26)
        .alnum(systemId, 8)
        .alnum(userId, 8)
        .alnum(program, 8)
        .alnum(terminal, 8)
        .alnum(type, 4)
        .alnum(action, 8)
        .alnum(status, 4)
        .alnum(portfolioId, 8)
        .alnum(accountNumber, 10)
        .alnum(beforeImage, IMAGE_LENGTH)
        .alnum(afterImage, IMAGE_LENGTH)
        .alnum(message, IMAGE_LENGTH);
  }
}
