package com.cognition.clbs.reporting.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FixedRecordFileTest {

  @Test
  void splitsFileIntoFixedLengthRecords(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("in.dat");
    Files.writeString(file, "ABCDEFGH", StandardCharsets.US_ASCII);
    List<String> records =
        FixedRecordFile.readAll(file, 4, (bytes, cs) -> new String(bytes, (Charset) cs));
    assertThat(records).containsExactly("ABCD", "EFGH");
    Path empty = Files.createFile(dir.resolve("empty.dat"));
    assertThat(FixedRecordFile.readAll(empty, 4, (b, c) -> "x")).isEmpty();
  }

  @Test
  void rejectsPartialTrailingRecord(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("in.dat");
    Files.writeString(file, "ABCDE", StandardCharsets.US_ASCII);
    assertThatThrownBy(() -> FixedRecordFile.readAll(file, 4, (b, c) -> "x"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("not a multiple");
  }
}
