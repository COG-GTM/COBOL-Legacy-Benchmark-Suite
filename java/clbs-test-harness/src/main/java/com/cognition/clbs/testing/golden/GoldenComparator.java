package com.cognition.clbs.testing.golden;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure comparison functions used by the golden-file assertions. Two flavours are supported,
 * matching how COBOL outputs are produced:
 *
 * <ul>
 *   <li>{@link #compareText} for line-sequential files and printed reports
 *   <li>{@link #compareRecords} for fixed-length (LRECL) record files such as VSAM extracts
 * </ul>
 */
public final class GoldenComparator {

  private static final String TEXT_UNIT = "line";
  private static final String RECORD_UNIT = "record";

  private GoldenComparator() {}

  public static ComparisonResult compareText(String expected, String actual) {
    return compareText(expected, actual, TextOptions.defaults());
  }

  public static ComparisonResult compareText(String expected, String actual, TextOptions options) {
    List<String> expectedLines = splitLines(expected, options);
    List<String> actualLines = splitLines(actual, options);
    int max = Math.max(expectedLines.size(), actualLines.size());
    List<Difference> reported = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < max; i++) {
      String e = i < expectedLines.size() ? expectedLines.get(i) : null;
      String a = i < actualLines.size() ? actualLines.get(i) : null;
      if (e == null || !e.equals(a)) {
        total++;
        if (reported.size() < options.maxReportedDifferences()) {
          reported.add(Difference.of(i + 1, e, a));
        }
      }
    }
    return new ComparisonResult(
        TEXT_UNIT, expectedLines.size(), actualLines.size(), total, reported);
  }

  /**
   * Compares two byte arrays as sequences of fixed-length records.
   *
   * <p>A trailing partial record on either side is reported as a difference so that truncated
   * output never passes silently.
   */
  public static ComparisonResult compareRecords(byte[] expected, byte[] actual, int recordLength) {
    return compareRecords(
        expected, actual, recordLength, TextOptions.DEFAULT_MAX_REPORTED_DIFFERENCES);
  }

  public static ComparisonResult compareRecords(
      byte[] expected, byte[] actual, int recordLength, int maxReportedDifferences) {
    if (recordLength < 1) {
      throw new IllegalArgumentException("recordLength must be >= 1");
    }
    if (maxReportedDifferences < 1) {
      throw new IllegalArgumentException("maxReportedDifferences must be >= 1");
    }
    int expectedCount = recordCount(expected, recordLength);
    int actualCount = recordCount(actual, recordLength);
    int max = Math.max(expectedCount, actualCount);
    List<Difference> reported = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < max; i++) {
      byte[] e = record(expected, i, recordLength);
      byte[] a = record(actual, i, recordLength);
      if (e == null || e.length != recordLength || !Arrays.equals(e, a)) {
        total++;
        if (reported.size() < maxReportedDifferences) {
          reported.add(recordDifference(i, e, a, recordLength));
        }
      }
    }
    return new ComparisonResult(RECORD_UNIT, expectedCount, actualCount, total, reported);
  }

  private static Difference recordDifference(int index, byte[] e, byte[] a, int recordLength) {
    String detail = "";
    if (e != null && a != null) {
      int offset = Arrays.mismatch(e, a);
      if (e.length != recordLength || a.length != recordLength) {
        detail = "partial record: expected " + e.length + " bytes, actual " + a.length;
      } else {
        detail =
            "first mismatch at offset "
                + offset
                + ": expected 0x"
                + hex(e[offset])
                + ", actual 0x"
                + hex(a[offset]);
      }
    } else if (e != null && e.length != recordLength) {
      detail = "partial record: " + e.length + " bytes";
    } else if (a != null && a.length != recordLength) {
      detail = "partial record: " + a.length + " bytes";
    }
    return new Difference(index + 1, printable(e), printable(a), detail);
  }

  private static int recordCount(byte[] data, int recordLength) {
    return (data.length + recordLength - 1) / recordLength;
  }

  private static byte[] record(byte[] data, int index, int recordLength) {
    int from = index * recordLength;
    if (from >= data.length) {
      return null;
    }
    return Arrays.copyOfRange(data, from, Math.min(from + recordLength, data.length));
  }

  private static String hex(byte b) {
    return String.format("%02X", b);
  }

  /** Renders bytes as ISO-8859-1 text with control characters shown as '.'. */
  static String printable(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    String raw = new String(bytes, StandardCharsets.ISO_8859_1);
    StringBuilder sb = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      sb.append(c < 0x20 || c == 0x7F ? '.' : c);
    }
    return sb.toString();
  }

  private static List<String> splitLines(String text, TextOptions options) {
    String normalised =
        options.normalizeLineEndings() ? text.replace("\r\n", "\n").replace('\r', '\n') : text;
    List<String> lines = new ArrayList<>(Arrays.asList(normalised.split("\n", -1)));
    if (options.ignoreTrailingWhitespace()) {
      lines.replaceAll(GoldenComparator::stripTrailingWhitespace);
    }
    if (options.ignoreTrailingBlankLines()) {
      while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
        lines.remove(lines.size() - 1);
      }
    } else if (!lines.isEmpty() && normalised.endsWith("\n")) {
      // A terminating newline does not start a new line.
      lines.remove(lines.size() - 1);
    }
    return lines;
  }

  private static String stripTrailingWhitespace(String line) {
    int end = line.length();
    while (end > 0 && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t')) {
      end--;
    }
    return line.substring(0, end);
  }
}
