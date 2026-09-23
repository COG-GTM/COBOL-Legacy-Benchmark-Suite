package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code ERR-MESSAGE} from {@code ERRHAND.cpy}: the batch/common error message block (370 bytes).
 *
 * @param errorDate {@code ERR-DATE PIC X(10)} as {@code YYYY-MM-DD}
 * @param errorTime {@code ERR-TIME PIC X(8)} as {@code HH.MM.SS}
 * @param program {@code ERR-PROGRAM PIC X(8)}
 * @param category {@code ERR-CATEGORY PIC X(2)}, see {@link ErrorCodes.Category}
 * @param code {@code ERR-CODE PIC X(4)}, see {@link ErrorCodes.Standard}
 * @param severity {@code ERR-SEVERITY PIC S9(4) COMP}, see {@link ReturnCode}
 * @param text {@code ERR-TEXT PIC X(80)}
 * @param details {@code ERR-DETAILS PIC X(256)}
 */
public record ErrorMessage(
    String errorDate,
    String errorTime,
    String program,
    String category,
    String code,
    int severity,
    String text,
    String details)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 370;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static ErrorMessage parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static ErrorMessage parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, ErrorMessage::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static ErrorMessage read(RecordReader in) {
    return new ErrorMessage(
        in.alnum(10),
        in.alnum(8),
        in.alnum(8),
        in.alnum(2),
        in.alnum(4),
        in.comp(4, true),
        in.alnum(80),
        in.alnum(256));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(errorDate, 10)
        .alnum(errorTime, 8)
        .alnum(program, 8)
        .alnum(category, 2)
        .alnum(code, 4)
        .comp(severity, 4, true)
        .alnum(text, 80)
        .alnum(details, 256);
  }
}
