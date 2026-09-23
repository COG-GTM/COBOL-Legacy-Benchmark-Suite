package com.cognition.clbs.common.cobol;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CobolDatesTest {

  @Test
  void cobolDateAndTimeForms() {
    LocalDate date = LocalDate.of(2024, 3, 31);
    assertThat(CobolDates.parseDate("20240331")).isEqualTo(date);
    assertThat(CobolDates.parseDate(20240331)).isEqualTo(date);
    assertThat(CobolDates.formatDate(date)).isEqualTo("20240331");
    assertThat(CobolDates.formatDateNumeric(date)).isEqualTo(20240331);
    assertThat(CobolDates.parseTime("093015")).isEqualTo(LocalTime.of(9, 30, 15));
    assertThat(CobolDates.formatTime(LocalTime.of(9, 30, 15))).isEqualTo("093015");
  }

  @Test
  void db2Forms() {
    assertThat(CobolDates.parseIsoDate("2024-03-31")).isEqualTo(LocalDate.of(2024, 3, 31));
    assertThat(CobolDates.formatIsoDate(LocalDate.of(2024, 3, 31))).isEqualTo("2024-03-31");
    assertThat(CobolDates.parseDb2Time("09.30.15")).isEqualTo(LocalTime.of(9, 30, 15));
    assertThat(CobolDates.formatDb2Time(LocalTime.of(9, 30, 15))).isEqualTo("09.30.15");
    LocalDateTime ts = LocalDateTime.of(2024, 3, 31, 9, 30, 15, 123456000);
    assertThat(CobolDates.parseTimestamp("2024-03-31-09.30.15.123456")).isEqualTo(ts);
    assertThat(CobolDates.formatTimestamp(ts)).isEqualTo("2024-03-31-09.30.15.123456");
    assertThat(CobolDates.formatTimestamp(ts)).hasSize(26);
  }

  @Test
  void blankAndZeroMeanAbsent() {
    assertThat(CobolDates.parseDate("")).isNull();
    assertThat(CobolDates.parseDate("        ")).isNull();
    assertThat(CobolDates.parseDate("00000000")).isNull();
    assertThat(CobolDates.parseDate(0)).isNull();
    assertThat(CobolDates.parseTime((String) null)).isNull();
    assertThat(CobolDates.parseTime("000000")).isNull();
    assertThat(CobolDates.parseIsoDate("   ")).isNull();
    assertThat(CobolDates.parseDb2Time("")).isNull();
    assertThat(CobolDates.parseTimestamp("")).isNull();
    assertThat(CobolDates.formatDate(null)).isEmpty();
    assertThat(CobolDates.formatDateNumeric(null)).isZero();
    assertThat(CobolDates.formatTime(null)).isEmpty();
    assertThat(CobolDates.formatIsoDate(null)).isEmpty();
    assertThat(CobolDates.formatDb2Time(null)).isEmpty();
    assertThat(CobolDates.formatTimestamp(null)).isEmpty();
  }
}
