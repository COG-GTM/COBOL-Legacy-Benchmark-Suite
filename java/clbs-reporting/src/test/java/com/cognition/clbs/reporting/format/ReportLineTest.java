package com.cognition.clbs.reporting.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReportLineTest {

  @Test
  void padsAndTruncatesAlphanumericFields() {
    String line = new ReportLine(12).alnum("ABC", 5).spaces(2).alnum("TOOLONG", 4).build();
    assertThat(line).isEqualTo("ABC    TOOL ");
  }

  @Test
  void nullAlnumIsSpaces() {
    assertThat(new ReportLine(4).alnum(null, 3).build()).isEqualTo("    ");
  }

  @Test
  void fillsToReportWidth() {
    String line = new ReportLine().fill('*', 10).build();
    assertThat(line).hasSize(ReportLine.WIDTH).startsWith("**********  ");
    assertThat(new ReportLine().build()).isEqualTo(" ".repeat(ReportLine.WIDTH));
  }

  @Test
  void appendsNumericEditedFields() {
    String line =
        new ReportLine(20)
            .number(NumericPicture.of("ZZ9.99"), new BigDecimal("3.5"))
            .number(NumericPicture.of("ZZ9"), 42)
            .build();
    assertThat(line).isEqualTo("  3.50 42           ");
    assertThat(new ReportLine(6).number(NumericPicture.of("ZZ9.99"), new BigDecimal("3.5")))
        .hasToString("  3.50");
  }

  @Test
  void rejectsOverflow() {
    assertThatThrownBy(() -> new ReportLine(3).alnum("ABCD", 4).build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("limit is 3");
  }
}
