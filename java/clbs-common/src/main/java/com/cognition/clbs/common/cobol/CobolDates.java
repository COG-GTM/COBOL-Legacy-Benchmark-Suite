package com.cognition.clbs.common.cobol;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Conversions between the textual date/time shapes used in the copybooks and {@code java.time}.
 * Blank or all-zero fields (the COBOL "no value" idioms) map to {@code null} and back.
 */
public final class CobolDates {

  /** {@code PIC X(8)} / {@code PIC 9(8)} dates: {@code YYYYMMDD}. */
  public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  /** {@code PIC X(6)} times: {@code HHMMSS}. */
  public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmmss");

  /** {@code PIC X(10)} ISO dates: {@code YYYY-MM-DD} (DB2 DATE host variables). */
  public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  /** {@code PIC X(8)} ISO times: {@code HH.MM.SS} (DB2 TIME host variables). */
  public static final DateTimeFormatter DB2_TIME = DateTimeFormatter.ofPattern("HH.mm.ss");

  /** {@code PIC X(26)} DB2 timestamps: {@code YYYY-MM-DD-HH.MM.SS.NNNNNN}. */
  public static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

  private CobolDates() {}

  /** Parses {@code YYYYMMDD}; blank or zero yields {@code null}. */
  public static LocalDate parseDate(String yyyymmdd) {
    return isEmpty(yyyymmdd) ? null : LocalDate.parse(yyyymmdd.trim(), DATE);
  }

  /** Parses a {@code PIC 9(8)} date held as an int; zero yields {@code null}. */
  public static LocalDate parseDate(int yyyymmdd) {
    return yyyymmdd == 0 ? null : LocalDate.parse(String.format("%08d", yyyymmdd), DATE);
  }

  /** Formats as {@code YYYYMMDD}; {@code null} yields an empty string. */
  public static String formatDate(LocalDate date) {
    return date == null ? "" : DATE.format(date);
  }

  /** Formats as a {@code PIC 9(8)} int; {@code null} yields zero. */
  public static int formatDateNumeric(LocalDate date) {
    return date == null ? 0 : Integer.parseInt(DATE.format(date));
  }

  /** Parses {@code HHMMSS}; blank or zero yields {@code null}. */
  public static LocalTime parseTime(String hhmmss) {
    return isEmpty(hhmmss) ? null : LocalTime.parse(hhmmss.trim(), TIME);
  }

  /** Formats as {@code HHMMSS}; {@code null} yields an empty string. */
  public static String formatTime(LocalTime time) {
    return time == null ? "" : TIME.format(time);
  }

  /** Parses {@code YYYY-MM-DD}; blank yields {@code null}. */
  public static LocalDate parseIsoDate(String isoDate) {
    return isEmpty(isoDate) ? null : LocalDate.parse(isoDate.trim(), ISO_DATE);
  }

  /** Formats as {@code YYYY-MM-DD}; {@code null} yields an empty string. */
  public static String formatIsoDate(LocalDate date) {
    return date == null ? "" : ISO_DATE.format(date);
  }

  /** Parses {@code HH.MM.SS}; blank yields {@code null}. */
  public static LocalTime parseDb2Time(String db2Time) {
    return isEmpty(db2Time) ? null : LocalTime.parse(db2Time.trim(), DB2_TIME);
  }

  /** Formats as {@code HH.MM.SS}; {@code null} yields an empty string. */
  public static String formatDb2Time(LocalTime time) {
    return time == null ? "" : DB2_TIME.format(time);
  }

  /** Parses a 26-byte DB2 timestamp; blank yields {@code null}. */
  public static LocalDateTime parseTimestamp(String db2Timestamp) {
    return isEmpty(db2Timestamp) ? null : LocalDateTime.parse(db2Timestamp.trim(), TIMESTAMP);
  }

  /** Formats as a 26-byte DB2 timestamp; {@code null} yields an empty string. */
  public static String formatTimestamp(LocalDateTime timestamp) {
    return timestamp == null ? "" : TIMESTAMP.format(timestamp);
  }

  private static boolean isEmpty(String value) {
    if (value == null || value.isBlank()) {
      return true;
    }
    for (int i = 0; i < value.length(); i++) {
      if (value.charAt(i) != '0') {
        return false;
      }
    }
    return true;
  }
}
