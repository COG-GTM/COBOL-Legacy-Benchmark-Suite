package com.cognition.clbs.testing.golden;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;

@ExtendWith(GoldenFileExtension.class)
class GoldenFileExtensionTest {

  @AfterEach
  void clearRootOverride() {
    System.clearProperty(GoldenFileExtension.ROOT_PROPERTY);
  }

  @Test
  void resolvesDirectoryFromTestClassName(GoldenFile golden) {
    assertThat(golden.directory())
        .isEqualTo(GoldenFileExtension.DEFAULT_ROOT.resolve("GoldenFileExtensionTest"));
    assertThat(golden.path("x.txt")).isEqualTo(golden.directory().resolve("x.txt"));
  }

  @Test
  void defaultTextFileIsNamedAfterTestMethod(GoldenFile golden) {
    golden.assertText("POSITION REPORT\nACCT 0001  100.00\n");
  }

  @Test
  void defaultRecordFileIsNamedAfterTestMethod(GoldenFile golden) {
    golden.assertRecords("AAAABBBB".getBytes(StandardCharsets.US_ASCII), 4);
  }

  @Test
  void namedFilesAndOptionsAreSupported(GoldenFile golden) {
    golden.assertText("named.txt", "TOTAL\n", TextOptions.defaults().ignoringTrailingWhitespace());
    golden.assertRecords("named.dat", "1234".getBytes(StandardCharsets.US_ASCII), 4);
  }

  @Test
  void mismatchFailsWithGoldenPathInMessage(GoldenFile golden) {
    assertThatThrownBy(() -> golden.assertText("named.txt", "WRONG\n"))
        .isInstanceOf(AssertionFailedError.class)
        .hasMessageContaining(Paths.get("GoldenFileExtensionTest", "named.txt").toString());
  }

  @Test
  void rootCanBeOverriddenWithSystemProperty() {
    System.setProperty(GoldenFileExtension.ROOT_PROPERTY, "/tmp/other-golden");

    assertThat(GoldenFileExtension.goldenRoot()).isEqualTo(Path.of("/tmp/other-golden"));

    System.setProperty(GoldenFileExtension.ROOT_PROPERTY, "  ");
    assertThat(GoldenFileExtension.goldenRoot()).isEqualTo(GoldenFileExtension.DEFAULT_ROOT);
  }
}
