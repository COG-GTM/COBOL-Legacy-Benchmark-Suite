package com.cognition.clbs.reporting.format;

import java.math.BigDecimal;

/**
 * Builds one fixed-width report record the way a COBOL {@code 01} group with {@code FILLER} and
 * {@code PIC X}/numeric-edited elementary items does: alphanumeric fields are left-justified and
 * space-padded or truncated to their width, and the finished record is space-filled to the report
 * width.
 */
public final class ReportLine {

  /** Record length of every report file produced by RPTPOS00, RPTAUD00 and RPTSTA00. */
  public static final int WIDTH = 132;

  private final StringBuilder text = new StringBuilder(WIDTH);
  private final int width;

  public ReportLine() {
    this(WIDTH);
  }

  public ReportLine(int width) {
    this.width = width;
  }

  /** Appends a {@code PIC X(length)} field. */
  public ReportLine alnum(String value, int length) {
    String v = value == null ? "" : value;
    if (v.length() >= length) {
      text.append(v, 0, length);
    } else {
      text.append(v);
      spaces(length - v.length());
    }
    return this;
  }

  /** Appends a {@code FILLER PIC X(length) VALUE SPACES}. */
  public ReportLine spaces(int length) {
    text.append(" ".repeat(length));
    return this;
  }

  /** Appends a {@code FILLER PIC X(length) VALUE ALL 'c'}. */
  public ReportLine fill(char c, int length) {
    text.append(String.valueOf(c).repeat(length));
    return this;
  }

  /** Appends a numeric-edited field. */
  public ReportLine number(NumericPicture picture, BigDecimal value) {
    text.append(picture.format(value));
    return this;
  }

  /** Appends a numeric-edited field from an integral value. */
  public ReportLine number(NumericPicture picture, long value) {
    text.append(picture.format(value));
    return this;
  }

  /** The record, space-padded to the report width. */
  public String build() {
    if (text.length() > width) {
      throw new IllegalStateException(
          "Report line is " + text.length() + " characters, limit is " + width + ": " + text);
    }
    return text.toString() + " ".repeat(width - text.length());
  }

  @Override
  public String toString() {
    return build();
  }
}
