package com.clbs.batch;

/** Shared fixed-width report formatting for the RPT* programs (REPORT-RECORD PIC X(132/133)). */
final class ReportLines {

    private ReportLines() {
    }

    static String rule(char fill, int width) {
        return String.valueOf(fill).repeat(width);
    }

    /** WS-HEADER2 style: title centred inside a fixed-width line. */
    static String centred(String title, int width) {
        int left = Math.max(0, (width - title.length()) / 2);
        return pad(" ".repeat(left) + title, width);
    }

    static String pad(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }
}
