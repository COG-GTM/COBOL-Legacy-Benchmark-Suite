package com.cognition.clbs.testing.golden;

import java.util.Objects;

/**
 * One mismatching unit (a line of text or a fixed-width record) between expected and actual.
 *
 * @param position 1-based line or record number
 * @param expected the expected unit rendered for display, or {@code null} if the expected side
 *     ended
 * @param actual the actual unit rendered for display, or {@code null} if the actual side ended
 * @param detail optional extra context (e.g. byte offset of the first mismatch), may be empty
 */
public record Difference(int position, String expected, String actual, String detail) {

  public Difference {
    Objects.requireNonNull(detail, "detail");
  }

  public static Difference of(int position, String expected, String actual) {
    return new Difference(position, expected, actual, "");
  }

  boolean missingInActual() {
    return actual == null;
  }

  boolean missingInExpected() {
    return expected == null;
  }

  String describe(String unitName) {
    StringBuilder sb = new StringBuilder();
    sb.append(unitName).append(' ').append(position);
    if (missingInActual()) {
      sb.append(" missing in actual");
    } else if (missingInExpected()) {
      sb.append(" unexpected in actual");
    }
    if (!detail.isEmpty()) {
      sb.append(" (").append(detail).append(')');
    }
    sb.append(System.lineSeparator());
    sb.append("  expected: ").append(render(expected)).append(System.lineSeparator());
    sb.append("  actual:   ").append(render(actual));
    return sb.toString();
  }

  private static String render(String value) {
    return value == null ? "<none>" : "|" + value + "|";
  }
}
