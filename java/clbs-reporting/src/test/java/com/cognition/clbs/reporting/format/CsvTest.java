package com.cognition.clbs.reporting.format;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

  @Test
  void writesCrlfRowsAndQuotesOnlyWhenNeeded() {
    String csv =
        new Csv()
            .header("a", "b")
            .row("plain", "has,comma")
            .row("has \"quote\"", null)
            .rows(List.<Object[]>of(new Object[] {1, "line\nbreak"}))
            .toString();
    assertThat(csv)
        .isEqualTo(
            "a,b\r\n"
                + "plain,\"has,comma\"\r\n"
                + "\"has \"\"quote\"\"\",\r\n"
                + "1,\"line\nbreak\"\r\n");
  }
}
