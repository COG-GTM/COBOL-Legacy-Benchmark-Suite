package com.cognition.clbs.testing.golden;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opentest4j.AssertionFailedError;

/**
 * Golden-file assertions: compare an actual output against a baseline file on disk, or (re)generate
 * that baseline when running in update mode.
 *
 * <p>Update mode is enabled with {@code -Dclbs.golden.update=true} or the environment variable
 * {@code CLBS_GOLDEN_UPDATE=true}. In update mode every assertion writes the actual value to the
 * golden path and passes, so regenerated baselines show up as a reviewable diff in git.
 */
public final class GoldenFiles {

  public static final String UPDATE_PROPERTY = "clbs.golden.update";
  public static final String UPDATE_ENV = "CLBS_GOLDEN_UPDATE";

  private GoldenFiles() {}

  public static boolean updateMode() {
    return Boolean.parseBoolean(System.getProperty(UPDATE_PROPERTY))
        || Boolean.parseBoolean(System.getenv(UPDATE_ENV));
  }

  public static void assertMatchesText(Path golden, String actual) {
    assertMatchesText(golden, actual, TextOptions.defaults(), StandardCharsets.UTF_8);
  }

  public static void assertMatchesText(Path golden, String actual, TextOptions options) {
    assertMatchesText(golden, actual, options, StandardCharsets.UTF_8);
  }

  public static void assertMatchesText(
      Path golden, String actual, TextOptions options, Charset charset) {
    if (updateMode()) {
      write(golden, actual.getBytes(charset));
      return;
    }
    String expected = new String(readGolden(golden), charset);
    ComparisonResult result = GoldenComparator.compareText(expected, actual, options);
    fail(golden, result);
  }

  public static void assertMatchesRecords(Path golden, byte[] actual, int recordLength) {
    if (updateMode()) {
      write(golden, actual);
      return;
    }
    ComparisonResult result =
        GoldenComparator.compareRecords(readGolden(golden), actual, recordLength);
    fail(golden, result);
  }

  private static void fail(Path golden, ComparisonResult result) {
    if (!result.matches()) {
      throw new AssertionFailedError(
          "Actual output differs from golden file "
              + golden
              + System.lineSeparator()
              + result.report()
              + System.lineSeparator()
              + "Re-run with -D"
              + UPDATE_PROPERTY
              + "=true to regenerate the baseline.");
    }
  }

  private static byte[] readGolden(Path golden) {
    if (!Files.isRegularFile(golden)) {
      throw new AssertionFailedError(
          "Golden file not found: "
              + golden
              + System.lineSeparator()
              + "Run once with -D"
              + UPDATE_PROPERTY
              + "=true to create it, then review and commit the generated file.");
    }
    try {
      return Files.readAllBytes(golden);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read golden file " + golden, e);
    }
  }

  private static void write(Path golden, byte[] content) {
    try {
      Path parent = golden.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(golden, content);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write golden file " + golden, e);
    }
  }
}
