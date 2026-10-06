package com.cognition.clbs.reporting;

import com.cognition.clbs.reporting.format.ReportLine;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The three heading records shared by every report program ({@code WS-HEADER1..3}): a row of
 * asterisks, the centred title, and {@code REPORT DATE:} followed by the date obtained with {@code
 * ACCEPT ... FROM DATE}, which yields {@code YYMMDD} and is moved into a {@code PIC X(10)} field.
 */
final class ReportHeading {

  /** {@code ACCEPT FROM DATE} returns a six-digit {@code YYMMDD} value. */
  static final DateTimeFormatter ACCEPT_DATE = DateTimeFormatter.ofPattern("yyMMdd");

  private static final int DATE_LABEL_WIDTH = 15;
  private static final int DATE_WIDTH = 10;

  private ReportHeading() {}

  /**
   * Builds the heading lines.
   *
   * @param title the report title
   * @param leftMargin width of the leading FILLER before the title
   * @param titleWidth width of the title field (left-justified, space padded)
   * @param reportDate the run date
   */
  static List<String> lines(String title, int leftMargin, int titleWidth, LocalDate reportDate) {
    return List.of(
        new ReportLine().fill('*', ReportLine.WIDTH).build(),
        new ReportLine().spaces(leftMargin).alnum(title, titleWidth).build(),
        new ReportLine()
            .alnum("REPORT DATE:", DATE_LABEL_WIDTH)
            .alnum(reportDate.format(ACCEPT_DATE), DATE_WIDTH)
            .build());
  }
}
