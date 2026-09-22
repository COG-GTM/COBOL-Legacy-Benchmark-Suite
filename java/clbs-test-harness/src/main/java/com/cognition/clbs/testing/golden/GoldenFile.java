package com.cognition.clbs.testing.golden;

import java.nio.file.Path;

/**
 * Per-test handle onto the golden directory, injected by {@link GoldenFileExtension}.
 *
 * <p>Files live under {@code <goldenRoot>/<TestClassSimpleName>/} and default to the test method
 * name, so a test {@code RptPos00Test#printsPositionReport} compares against {@code
 * src/test/resources/golden/RptPos00Test/printsPositionReport.txt}.
 */
public final class GoldenFile {

  private final Path directory;
  private final String defaultBaseName;

  GoldenFile(Path directory, String defaultBaseName) {
    this.directory = directory;
    this.defaultBaseName = defaultBaseName;
  }

  /** Directory holding this test class's golden files. */
  public Path directory() {
    return directory;
  }

  /** Path of a named golden file inside {@link #directory()}. */
  public Path path(String fileName) {
    return directory.resolve(fileName);
  }

  public void assertText(String actual) {
    assertText(defaultBaseName + ".txt", actual);
  }

  public void assertText(String fileName, String actual) {
    GoldenFiles.assertMatchesText(path(fileName), actual);
  }

  public void assertText(String fileName, String actual, TextOptions options) {
    GoldenFiles.assertMatchesText(path(fileName), actual, options);
  }

  public void assertRecords(byte[] actual, int recordLength) {
    assertRecords(defaultBaseName + ".dat", actual, recordLength);
  }

  public void assertRecords(String fileName, byte[] actual, int recordLength) {
    GoldenFiles.assertMatchesRecords(path(fileName), actual, recordLength);
  }
}
