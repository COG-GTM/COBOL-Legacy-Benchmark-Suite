package com.cognition.clbs.testing.golden;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

class GoldenFilesTest {

  @TempDir Path dir;

  @AfterEach
  void clearUpdateMode() {
    System.clearProperty(GoldenFiles.UPDATE_PROPERTY);
  }

  @Test
  void passesWhenTextMatchesGolden() throws IOException {
    Path golden = Files.writeString(dir.resolve("report.txt"), "HEADER\nLINE 1\n");

    assertThatCode(() -> GoldenFiles.assertMatchesText(golden, "HEADER\nLINE 1\n"))
        .doesNotThrowAnyException();
  }

  @Test
  void failsWithReportAndUpdateHintWhenTextDiffers() throws IOException {
    Path golden = Files.writeString(dir.resolve("report.txt"), "HEADER\nLINE 1\n");

    assertThatThrownBy(() -> GoldenFiles.assertMatchesText(golden, "HEADER\nLINE 2\n"))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining(golden.toString())
        .hasMessageContaining("line 2")
        .hasMessageContaining("expected: |LINE 1|")
        .hasMessageContaining("-Dclbs.golden.update=true");
  }

  @Test
  void failsWithCreationHintWhenGoldenMissing() {
    Path golden = dir.resolve("missing.txt");

    assertThatThrownBy(() -> GoldenFiles.assertMatchesText(golden, "anything"))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining("Golden file not found")
        .hasMessageContaining("-Dclbs.golden.update=true");
  }

  @Test
  void honoursTextOptionsAndCharset() throws IOException {
    Path golden =
        Files.write(dir.resolve("latin1.txt"), "café   \n".getBytes(StandardCharsets.ISO_8859_1));

    assertThatCode(
            () ->
                GoldenFiles.assertMatchesText(
                    golden,
                    "café\n",
                    TextOptions.defaults().ignoringTrailingWhitespace(),
                    StandardCharsets.ISO_8859_1))
        .doesNotThrowAnyException();
  }

  @Test
  void updateModeWritesTextGoldenIncludingParentDirectories() throws IOException {
    System.setProperty(GoldenFiles.UPDATE_PROPERTY, "true");
    Path golden = dir.resolve("nested/deeper/report.txt");

    GoldenFiles.assertMatchesText(golden, "GENERATED\n");

    assertThat(golden).content(StandardCharsets.UTF_8).isEqualTo("GENERATED\n");
  }

  @Test
  void passesWhenRecordsMatchGolden() throws IOException {
    byte[] data = "AAAABBBB".getBytes(StandardCharsets.US_ASCII);
    Path golden = Files.write(dir.resolve("extract.dat"), data);

    assertThatCode(() -> GoldenFiles.assertMatchesRecords(golden, data.clone(), 4))
        .doesNotThrowAnyException();
  }

  @Test
  void failsWhenRecordsDiffer() throws IOException {
    Path golden =
        Files.write(dir.resolve("extract.dat"), "AAAABBBB".getBytes(StandardCharsets.US_ASCII));

    assertThatThrownBy(
            () ->
                GoldenFiles.assertMatchesRecords(
                    golden, "AAAABBBC".getBytes(StandardCharsets.US_ASCII), 4))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining("record 2")
        .hasMessageContaining("first mismatch at offset 3");
  }

  @Test
  void updateModeWritesRecordGolden() {
    System.setProperty(GoldenFiles.UPDATE_PROPERTY, "true");
    Path golden = dir.resolve("extract.dat");
    byte[] data = {1, 2, 3, 4};

    GoldenFiles.assertMatchesRecords(golden, data, 2);

    assertThat(golden).hasBinaryContent(data);
  }

  @Test
  void updateModeIsOffByDefault() {
    assertThat(GoldenFiles.updateMode()).isFalse();
    System.setProperty(GoldenFiles.UPDATE_PROPERTY, "true");
    assertThat(GoldenFiles.updateMode()).isTrue();
  }
}
