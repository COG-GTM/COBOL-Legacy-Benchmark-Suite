package com.cognition.clbs.common.cobol;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Sequential builder for one fixed-length COBOL record; the mirror image of {@link RecordReader}.
 * {@link #toBytes()} refuses to emit a partially written record so layout drift is caught in tests.
 */
public final class RecordWriter {

  private final byte[] data;
  private final Charset charset;
  private final byte space;
  private int position;

  /** Creates a writer for a record of {@code length} bytes, initially all spaces. */
  public RecordWriter(int length, Charset charset) {
    this.data = new byte[length];
    this.charset = charset;
    byte[] spaceBytes = " ".getBytes(charset);
    if (spaceBytes.length != 1) {
      throw new IllegalArgumentException("Single-byte charset required, got " + charset);
    }
    this.space = spaceBytes[0];
    Arrays.fill(data, space);
  }

  /** {@code PIC X(length)}: space-padded on the right; longer values are truncated (COBOL MOVE). */
  public RecordWriter alnum(String value, int length) {
    byte[] bytes = (value == null ? "" : value).getBytes(charset);
    int start = reserve(length);
    System.arraycopy(bytes, 0, data, start, Math.min(bytes.length, length));
    return this;
  }

  /** {@code PIC 9(digits)} DISPLAY: zero-padded unsigned digits. */
  public RecordWriter display(long value, int digits) {
    if (value < 0 || value > BinaryField.maxMagnitude(digits)) {
      throw new IllegalArgumentException("Value " + value + " does not fit PIC 9(" + digits + ")");
    }
    String text = String.format("%0" + digits + "d", value);
    int start = reserve(digits);
    System.arraycopy(text.getBytes(charset), 0, data, start, digits);
    return this;
  }

  /** {@code PIC S9(digits - scale)V9(scale) COMP-3}; {@code null} is written as zero. */
  public RecordWriter packed(BigDecimal value, int digits, int scale) {
    byte[] bytes = PackedDecimal.encode(value == null ? BigDecimal.ZERO : value, digits, scale);
    int start = reserve(bytes.length);
    System.arraycopy(bytes, 0, data, start, bytes.length);
    return this;
  }

  /** {@code PIC [S]9(digits) COMP}. */
  public RecordWriter comp(long value, int digits, boolean signed) {
    byte[] bytes = BinaryField.encode(value, digits, signed);
    int start = reserve(bytes.length);
    System.arraycopy(bytes, 0, data, start, bytes.length);
    return this;
  }

  /** {@code FILLER PIC X(length)}: left as spaces. */
  public RecordWriter filler(int length) {
    reserve(length);
    return this;
  }

  /** Returns the record, failing if any byte has not been written. */
  public byte[] toBytes() {
    if (position != data.length) {
      throw new IllegalStateException(
          "Record has " + data.length + " bytes but only " + position + " were written");
    }
    return data.clone();
  }

  private int reserve(int length) {
    if (length < 0 || position + length > data.length) {
      throw new IllegalArgumentException(
          "Field of "
              + length
              + " bytes at offset "
              + position
              + " exceeds record of "
              + data.length);
    }
    int start = position;
    position += length;
    return start;
  }
}
