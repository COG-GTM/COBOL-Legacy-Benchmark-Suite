package com.cognition.clbs.testing.golden;

import java.util.List;

/**
 * Outcome of comparing an actual output against a golden baseline.
 *
 * @param unitName what one {@link Difference} position refers to ("line" or "record")
 * @param expectedCount number of units on the expected side
 * @param actualCount number of units on the actual side
 * @param totalDifferences total number of mismatching units found
 * @param reported the first differences up to the configured cap, in position order
 */
public record ComparisonResult(
    String unitName,
    int expectedCount,
    int actualCount,
    int totalDifferences,
    List<Difference> reported) {

  public ComparisonResult {
    reported = List.copyOf(reported);
    if (reported.size() > totalDifferences) {
      throw new IllegalArgumentException("reported differences exceed total");
    }
  }

  public boolean matches() {
    return totalDifferences == 0;
  }

  /** Multi-line, human-readable summary suitable for an assertion message. */
  public String report() {
    String nl = System.lineSeparator();
    StringBuilder sb = new StringBuilder();
    if (matches()) {
      return sb.append("Output matches golden file (")
          .append(expectedCount)
          .append(' ')
          .append(plural(unitName, expectedCount))
          .append(')')
          .toString();
    }
    sb.append(totalDifferences)
        .append(" differing ")
        .append(plural(unitName, totalDifferences))
        .append(" (expected ")
        .append(expectedCount)
        .append(", actual ")
        .append(actualCount)
        .append(' ')
        .append(plural(unitName, actualCount))
        .append(')');
    for (Difference difference : reported) {
      sb.append(nl).append(difference.describe(unitName));
    }
    int omitted = totalDifferences - reported.size();
    if (omitted > 0) {
      sb.append(nl).append("... ").append(omitted).append(" more not shown");
    }
    return sb.toString();
  }

  private static String plural(String unit, int count) {
    return count == 1 ? unit : unit + "s";
  }
}
