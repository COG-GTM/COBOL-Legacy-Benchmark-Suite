package com.cognition.clbs.common.copybook.online;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code ERROR-HANDLING} from {@code online/ERRHND.cpy}: the CICS error block (174 bytes).
 *
 * @param program {@code ERR-PROGRAM PIC X(8)}
 * @param paragraph {@code ERR-PARAGRAPH PIC X(30)}
 * @param sqlCode {@code ERR-SQLCODE PIC S9(9) COMP}
 * @param cicsResponse {@code ERR-CICS-RESP PIC S9(8) COMP}
 * @param cicsResponse2 {@code ERR-CICS-RESP2 PIC S9(8) COMP}
 * @param severity {@code ERR-SEVERITY PIC X}, see {@link Severity}
 * @param message {@code ERR-MESSAGE PIC X(80)}
 * @param action {@code ERR-ACTION PIC X}, see {@link Action}
 * @param traceId {@code ERR-TRACE-ID PIC X(16)}
 * @param timestamp {@code ERR-TIMESTAMP PIC X(26)}
 */
public record OnlineErrorArea(
    String program,
    String paragraph,
    int sqlCode,
    int cicsResponse,
    int cicsResponse2,
    String severity,
    String message,
    String action,
    String traceId,
    String timestamp)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 174;

  /** {@code ERR-SEVERITY} condition names. */
  public enum Severity implements CobolCode {
    FATAL("F"),
    WARNING("W"),
    INFO("I");

    private final String code;

    Severity(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a severity code. */
    public static Severity fromCode(String code) {
      return CobolCode.fromCode(Severity.class, code);
    }
  }

  /** {@code ERR-ACTION} condition names. */
  public enum Action implements CobolCode {
    RETURN("R"),
    CONTINUE("C"),
    ABEND("A");

    private final String code;

    Action(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves an action code. */
    public static Action fromCode(String code) {
      return CobolCode.fromCode(Action.class, code);
    }
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static OnlineErrorArea parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static OnlineErrorArea parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, OnlineErrorArea::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static OnlineErrorArea read(RecordReader in) {
    return new OnlineErrorArea(
        in.alnum(8),
        in.alnum(30),
        in.comp(9, true),
        in.comp(8, true),
        in.comp(8, true),
        in.alnum(1),
        in.alnum(80),
        in.alnum(1),
        in.alnum(16),
        in.alnum(26));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(program, 8)
        .alnum(paragraph, 30)
        .comp(sqlCode, 9, true)
        .comp(cicsResponse, 8, true)
        .comp(cicsResponse2, 8, true)
        .alnum(severity, 1)
        .alnum(message, 80)
        .alnum(action, 1)
        .alnum(traceId, 16)
        .alnum(timestamp, 26);
  }
}
