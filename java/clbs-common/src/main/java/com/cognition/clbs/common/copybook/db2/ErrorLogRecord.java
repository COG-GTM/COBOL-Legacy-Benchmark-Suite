package com.cognition.clbs.common.copybook.db2;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code ERRLOG-RECORD} from {@code DBTBLS.cpy}: host-variable layout for the DB2 {@code ERRLOG}
 * table (771 bytes).
 *
 * @param errorTimestamp {@code EL-ERROR-TIMESTAMP PIC X(26)}
 * @param programId {@code EL-PROGRAM-ID PIC X(8)}
 * @param errorType {@code EL-ERROR-TYPE PIC X(1)}, see {@link ErrorType}
 * @param severity {@code EL-ERROR-SEVERITY PIC S9(4) COMP}, see {@link Severity}
 * @param errorCode {@code EL-ERROR-CODE PIC X(8)}
 * @param errorMessage {@code EL-ERROR-MESSAGE PIC X(200)}
 * @param processDate {@code EL-PROCESS-DATE PIC X(10)}
 * @param processTime {@code EL-PROCESS-TIME PIC X(8)}
 * @param userId {@code EL-USER-ID PIC X(8)}
 * @param additionalInfo {@code EL-ADDITIONAL-INFO PIC X(500)}
 */
public record ErrorLogRecord(
    String errorTimestamp,
    String programId,
    String errorType,
    int severity,
    String errorCode,
    String errorMessage,
    String processDate,
    String processTime,
    String userId,
    String additionalInfo)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 771;

  /** {@code EL-ERROR-TYPE} condition names. */
  public enum ErrorType implements CobolCode {
    SYSTEM("S"),
    APPLICATION("A"),
    DATA("D");

    private final String code;

    ErrorType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves an error type. */
    public static ErrorType fromCode(String code) {
      return CobolCode.fromCode(ErrorType.class, code);
    }
  }

  /** {@code EL-ERROR-SEVERITY} condition names. */
  public enum Severity {
    INFO(1),
    WARN(2),
    ERROR(3),
    SEVERE(4);

    private final int value;

    Severity(int value) {
      this.value = value;
    }

    /** The numeric severity. */
    public int value() {
      return value;
    }

    /** Resolves a numeric severity. */
    public static Severity fromValue(int value) {
      for (Severity severity : values()) {
        if (severity.value == value) {
          return severity;
        }
      }
      throw new IllegalArgumentException("Unknown error severity " + value);
    }
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static ErrorLogRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static ErrorLogRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, ErrorLogRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static ErrorLogRecord read(RecordReader in) {
    return new ErrorLogRecord(
        in.alnum(26),
        in.alnum(8),
        in.alnum(1),
        in.comp(4, true),
        in.alnum(8),
        in.alnum(200),
        in.alnum(10),
        in.alnum(8),
        in.alnum(8),
        in.alnum(500));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(errorTimestamp, 26)
        .alnum(programId, 8)
        .alnum(errorType, 1)
        .comp(severity, 4, true)
        .alnum(errorCode, 8)
        .alnum(errorMessage, 200)
        .alnum(processDate, 10)
        .alnum(processTime, 8)
        .alnum(userId, 8)
        .alnum(additionalInfo, 500);
  }
}
