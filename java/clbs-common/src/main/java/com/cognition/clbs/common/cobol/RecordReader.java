package com.cognition.clbs.common.cobol;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Sequential cursor over one fixed-length COBOL record. Each method consumes exactly the storage
 * the corresponding PICTURE clause occupies, so a record's {@code read} method is a line-by-line
 * transliteration of its copybook.
 */
public final class RecordReader {

  private final byte[] data;
  private final Charset charset;
  private int position;

  private RecordReader(byte[] data, Charset charset) {
    this.data = data;
    this.charset = charset;
  }

  /**
   * Parses one record of exactly {@code expectedLength} bytes with {@code parser}, verifying the
   * parser consumed the whole record.
   */
  public static <T> T parse(
      byte[] bytes, Charset charset, int expectedLength, Function<RecordReader, T> parser) {
    if (bytes.length != expectedLength) {
      throw new IllegalArgumentException(
          "Record is " + bytes.length + " bytes, copybook defines " + expectedLength);
    }
    RecordReader reader = new RecordReader(bytes, charset);
    T result = parser.apply(reader);
    if (reader.position != expectedLength) {
      throw new IllegalStateException(
          "Parser consumed " + reader.position + " of " + expectedLength + " bytes");
    }
    return result;
  }

  /** {@code PIC X(length)}: decodes and strips trailing spaces. */
  public String alnum(int length) {
    byte[] slice = take(length);
    String text = new String(slice, charset);
    int end = text.length();
    while (end > 0 && text.charAt(end - 1) == ' ') {
      end--;
    }
    return text.substring(0, end);
  }

  /** {@code PIC 9(digits)} DISPLAY (unsigned zoned decimal). An all-space field reads as zero. */
  public int display(int digits) {
    String text = new String(take(digits), charset);
    if (text.isBlank()) {
      return 0;
    }
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) < '0' || text.charAt(i) > '9') {
        throw new IllegalArgumentException(
            "Non-numeric DISPLAY field '" + text + "' at offset " + (position - digits));
      }
    }
    return Integer.parseInt(text);
  }

  /** {@code PIC S9(digits - scale)V9(scale) COMP-3}. */
  public BigDecimal packed(int digits, int scale) {
    return PackedDecimal.decode(take(PackedDecimal.byteLength(digits)), scale);
  }

  /** {@code PIC [S]9(digits) COMP} with at most nine digits. */
  public int comp(int digits, boolean signed) {
    if (digits > 9) {
      throw new IllegalArgumentException("Use compLong for more than nine digits");
    }
    return (int) BinaryField.decode(take(BinaryField.byteLength(digits)), signed);
  }

  /** {@code PIC [S]9(digits) COMP} with up to eighteen digits. */
  public long compLong(int digits, boolean signed) {
    return BinaryField.decode(take(BinaryField.byteLength(digits)), signed);
  }

  /** {@code FILLER PIC X(length)}: skipped. */
  public void skip(int length) {
    take(length);
  }

  /** Bytes consumed so far. */
  public int position() {
    return position;
  }

  private byte[] take(int length) {
    if (length < 0 || position + length > data.length) {
      throw new IllegalArgumentException(
          "Field of "
              + length
              + " bytes at offset "
              + position
              + " exceeds record of "
              + data.length);
    }
    byte[] slice = Arrays.copyOfRange(data, position, position + length);
    position += length;
    return slice;
  }
}
