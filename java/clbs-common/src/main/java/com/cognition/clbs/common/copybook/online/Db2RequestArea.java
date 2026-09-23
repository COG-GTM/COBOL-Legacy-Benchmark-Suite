package com.cognition.clbs.common.copybook.online;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code DB2-REQUEST-AREA} from {@code DB2REQ.cpy}: the COMMAREA passed to the CICS DB2 connection
 * manager (105 bytes).
 *
 * @param requestType {@code DB2-REQUEST-TYPE PIC X}, see {@link RequestType}
 * @param responseCode {@code DB2-RESPONSE-CODE PIC S9(8) COMP}
 * @param connectionToken {@code DB2-CONNECTION-TOKEN PIC X(16)}
 * @param sqlCode {@code DB2-SQLCODE PIC S9(9) COMP}
 * @param errorMessage {@code DB2-ERROR-MSG PIC X(80)}
 */
public record Db2RequestArea(
    String requestType, int responseCode, String connectionToken, int sqlCode, String errorMessage)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 105;

  /** {@code DB2-REQUEST-TYPE} condition names. */
  public enum RequestType implements CobolCode {
    CONNECT("C"),
    DISCONNECT("D"),
    STATUS("S");

    private final String code;

    RequestType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a request type. */
    public static RequestType fromCode(String code) {
      return CobolCode.fromCode(RequestType.class, code);
    }
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static Db2RequestArea parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static Db2RequestArea parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, Db2RequestArea::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static Db2RequestArea read(RecordReader in) {
    return new Db2RequestArea(
        in.alnum(1), in.comp(8, true), in.alnum(16), in.comp(9, true), in.alnum(80));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(requestType, 1)
        .comp(responseCode, 8, true)
        .alnum(connectionToken, 16)
        .comp(sqlCode, 9, true)
        .alnum(errorMessage, 80);
  }
}
