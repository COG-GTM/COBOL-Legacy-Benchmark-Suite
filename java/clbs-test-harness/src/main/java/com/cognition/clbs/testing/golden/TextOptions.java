package com.cognition.clbs.testing.golden;

/**
 * Normalisation applied to both sides of a text comparison before lines are diffed.
 *
 * <p>Report programs such as RPTPOS00 emit fixed-width lines padded with spaces, so trailing
 * whitespace is significant by default; opt out with {@link #ignoringTrailingWhitespace()} when a
 * Java writer legitimately trims.
 *
 * @param normalizeLineEndings treat {@code \r\n} and {@code \r} as {@code \n}
 * @param ignoreTrailingWhitespace strip trailing spaces and tabs from every line
 * @param ignoreTrailingBlankLines ignore empty lines at the end of either side
 * @param maxReportedDifferences cap on differences listed in the report (all are still counted)
 */
public record TextOptions(
    boolean normalizeLineEndings,
    boolean ignoreTrailingWhitespace,
    boolean ignoreTrailingBlankLines,
    int maxReportedDifferences) {

  public static final int DEFAULT_MAX_REPORTED_DIFFERENCES = 25;

  public TextOptions {
    if (maxReportedDifferences < 1) {
      throw new IllegalArgumentException("maxReportedDifferences must be >= 1");
    }
  }

  /** Line endings normalised, trailing blank lines ignored, trailing whitespace significant. */
  public static TextOptions defaults() {
    return new TextOptions(true, false, true, DEFAULT_MAX_REPORTED_DIFFERENCES);
  }

  /** Byte-for-byte line semantics: nothing is normalised. */
  public static TextOptions strict() {
    return new TextOptions(false, false, false, DEFAULT_MAX_REPORTED_DIFFERENCES);
  }

  public TextOptions ignoringTrailingWhitespace() {
    return new TextOptions(
        normalizeLineEndings, true, ignoreTrailingBlankLines, maxReportedDifferences);
  }

  public TextOptions withMaxReportedDifferences(int max) {
    return new TextOptions(
        normalizeLineEndings, ignoreTrailingWhitespace, ignoreTrailingBlankLines, max);
  }
}
