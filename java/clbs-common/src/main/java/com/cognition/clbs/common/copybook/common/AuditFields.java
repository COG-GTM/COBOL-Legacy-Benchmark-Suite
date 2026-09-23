package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code AUDIT-FIELDS} from {@code COMMON.cpy} (50 bytes): who/when/where stamp shared by every
 * maintained record.
 *
 * @param timestamp {@code AUDIT-TIMESTAMP PIC X(26)}
 * @param user {@code AUDIT-USER PIC X(08)}
 * @param terminal {@code AUDIT-TERMINAL PIC X(08)}
 * @param program {@code AUDIT-PROGRAM PIC X(08)}
 */
public record AuditFields(String timestamp, String user, String terminal, String program)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 50;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static AuditFields parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static AuditFields parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, AuditFields::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static AuditFields read(RecordReader in) {
    return new AuditFields(in.alnum(26), in.alnum(8), in.alnum(8), in.alnum(8));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(timestamp, 26).alnum(user, 8).alnum(terminal, 8).alnum(program, 8);
  }
}
