package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code ERROR-HANDLING} from {@code COMMON.cpy} (100 bytes).
 *
 * @param errorCode {@code ERROR-CODE PIC X(04)}
 * @param module {@code ERROR-MODULE PIC X(08)}
 * @param routine {@code ERROR-ROUTINE PIC X(08)}
 * @param message {@code ERROR-MESSAGE PIC X(80)}
 */
public record ErrorHandlingArea(String errorCode, String module, String routine, String message)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 100;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static ErrorHandlingArea parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static ErrorHandlingArea parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, ErrorHandlingArea::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static ErrorHandlingArea read(RecordReader in) {
    return new ErrorHandlingArea(in.alnum(4), in.alnum(8), in.alnum(8), in.alnum(80));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(errorCode, 4).alnum(module, 8).alnum(routine, 8).alnum(message, 80);
  }
}
