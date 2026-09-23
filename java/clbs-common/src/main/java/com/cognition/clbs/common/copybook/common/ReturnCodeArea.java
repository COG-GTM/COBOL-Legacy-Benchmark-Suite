package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code RETURN-CODE-AREA} from {@code RTNCODE.cpy}: the request/response area passed to the
 * RTNCODE return-code manager (165 bytes).
 *
 * @param requestType {@code RC-REQUEST-TYPE PIC X}, see {@link RequestType}
 * @param programId {@code RC-PROGRAM-ID PIC X(8)}
 * @param currentCode {@code RC-CURRENT-CODE PIC S9(4) COMP}
 * @param highestCode {@code RC-HIGHEST-CODE PIC S9(4) COMP}
 * @param newCode {@code RC-NEW-CODE PIC S9(4) COMP}
 * @param status {@code RC-STATUS PIC X}, see {@link Status}
 * @param message {@code RC-MESSAGE PIC X(80)}
 * @param responseCode {@code RC-RESPONSE-CODE PIC S9(8) COMP}
 * @param startTime {@code RC-START-TIME PIC X(26)}
 * @param endTime {@code RC-END-TIME PIC X(26)}
 * @param totalCodes {@code RC-TOTAL-CODES PIC S9(8) COMP}
 * @param maxCode {@code RC-MAX-CODE PIC S9(4) COMP}
 * @param minCode {@code RC-MIN-CODE PIC S9(4) COMP}
 * @param returnValue {@code RC-RETURN-VALUE PIC S9(4) COMP}
 * @param highestReturn {@code RC-HIGHEST-RETURN PIC S9(4) COMP}
 * @param returnStatus {@code RC-RETURN-STATUS PIC X}
 */
public record ReturnCodeArea(
    String requestType,
    String programId,
    int currentCode,
    int highestCode,
    int newCode,
    String status,
    String message,
    int responseCode,
    String startTime,
    String endTime,
    int totalCodes,
    int maxCode,
    int minCode,
    int returnValue,
    int highestReturn,
    String returnStatus)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 165;

  /** {@code RC-REQUEST-TYPE} condition names. */
  public enum RequestType implements CobolCode {
    INITIALIZE("I"),
    SET_CODE("S"),
    GET_CODE("G"),
    LOG_CODE("L"),
    ANALYZE("A");

    private final String code;

    RequestType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a request type code. */
    public static RequestType fromCode(String code) {
      return CobolCode.fromCode(RequestType.class, code);
    }
  }

  /** {@code RC-STATUS} condition names. */
  public enum Status implements CobolCode {
    SUCCESS("S"),
    WARNING("W"),
    ERROR("E"),
    SEVERE("F");

    private final String code;

    Status(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a status code. */
    public static Status fromCode(String code) {
      return CobolCode.fromCode(Status.class, code);
    }
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static ReturnCodeArea parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static ReturnCodeArea parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, ReturnCodeArea::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static ReturnCodeArea read(RecordReader in) {
    return new ReturnCodeArea(
        in.alnum(1),
        in.alnum(8),
        in.comp(4, true),
        in.comp(4, true),
        in.comp(4, true),
        in.alnum(1),
        in.alnum(80),
        in.comp(8, true),
        in.alnum(26),
        in.alnum(26),
        in.comp(8, true),
        in.comp(4, true),
        in.comp(4, true),
        in.comp(4, true),
        in.comp(4, true),
        in.alnum(1));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(requestType, 1)
        .alnum(programId, 8)
        .comp(currentCode, 4, true)
        .comp(highestCode, 4, true)
        .comp(newCode, 4, true)
        .alnum(status, 1)
        .alnum(message, 80)
        .comp(responseCode, 8, true)
        .alnum(startTime, 26)
        .alnum(endTime, 26)
        .comp(totalCodes, 8, true)
        .comp(maxCode, 4, true)
        .comp(minCode, 4, true)
        .comp(returnValue, 4, true)
        .comp(highestReturn, 4, true)
        .alnum(returnStatus, 1);
  }
}
