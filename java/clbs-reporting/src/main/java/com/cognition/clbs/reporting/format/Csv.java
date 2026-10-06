package com.cognition.clbs.reporting.format;

import java.util.List;

/** Minimal RFC 4180 CSV writer for the optional machine-readable report outputs. */
public final class Csv {

  private final StringBuilder out = new StringBuilder();

  public Csv header(String... columns) {
    return row((Object[]) columns);
  }

  public Csv row(Object... cells) {
    for (int i = 0; i < cells.length; i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(quote(cells[i]));
    }
    out.append("\r\n");
    return this;
  }

  public Csv rows(List<? extends Object[]> rows) {
    rows.forEach(this::row);
    return this;
  }

  @Override
  public String toString() {
    return out.toString();
  }

  static String quote(Object cell) {
    String s = cell == null ? "" : cell.toString();
    if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0) {
      return s;
    }
    return '"' + s.replace("\"", "\"\"") + '"';
  }
}
