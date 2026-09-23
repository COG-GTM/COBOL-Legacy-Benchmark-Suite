package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code COMMON-DATETIME} from {@code COMMON.cpy}: the split {@code CURRENT-DATE} / {@code
 * CURRENT-TIME} working area (16 bytes).
 *
 * @param year {@code CURR-YEAR PIC X(04)}
 * @param month {@code CURR-MONTH PIC X(02)}
 * @param day {@code CURR-DAY PIC X(02)}
 * @param hour {@code CURR-HOUR PIC X(02)}
 * @param minute {@code CURR-MINUTE PIC X(02)}
 * @param second {@code CURR-SECOND PIC X(02)}
 * @param millisecond {@code CURR-MSEC PIC X(02)}
 */
public record CommonDateTime(
    String year,
    String month,
    String day,
    String hour,
    String minute,
    String second,
    String millisecond)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 16;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static CommonDateTime parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static CommonDateTime parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, CommonDateTime::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static CommonDateTime read(RecordReader in) {
    return new CommonDateTime(
        in.alnum(4), in.alnum(2), in.alnum(2), in.alnum(2), in.alnum(2), in.alnum(2), in.alnum(2));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(year, 4)
        .alnum(month, 2)
        .alnum(day, 2)
        .alnum(hour, 2)
        .alnum(minute, 2)
        .alnum(second, 2)
        .alnum(millisecond, 2);
  }
}
