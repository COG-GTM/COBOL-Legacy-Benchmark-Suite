package com.cognition.clbs.reporting;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.reporting.format.ReportLine;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * The rendered fixed-width form of a report: an ordered list of {@value ReportLine#WIDTH}-character
 * records exactly as the COBOL program would {@code WRITE} them to {@code RPTFILE}.
 *
 * @param lines report records, each exactly {@link ReportLine#WIDTH} characters
 */
public record ReportDocument(List<String> lines) {

  public ReportDocument {
    lines = List.copyOf(lines);
    for (String line : lines) {
      if (line.length() != ReportLine.WIDTH) {
        throw new IllegalArgumentException(
            "Report line must be " + ReportLine.WIDTH + " characters: '" + line + "'");
      }
    }
  }

  /** Lines joined with {@code \n}, one trailing newline, trailing spaces preserved. */
  public String text() {
    StringBuilder sb = new StringBuilder(lines.size() * (ReportLine.WIDTH + 1));
    for (String line : lines) {
      sb.append(line).append('\n');
    }
    return sb.toString();
  }

  /** Fixed-length ASCII records with no delimiters, byte-equivalent to the COBOL report file. */
  public byte[] records() {
    return records(CobolCharset.ASCII);
  }

  /** Fixed-length records in the given single-byte encoding. */
  public byte[] records(Charset charset) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(lines.size() * ReportLine.WIDTH);
    for (String line : lines) {
      out.writeBytes(line.getBytes(charset));
    }
    return out.toByteArray();
  }
}
