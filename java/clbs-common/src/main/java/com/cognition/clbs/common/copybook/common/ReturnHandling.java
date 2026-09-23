package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code RETURN-HANDLING} from {@code RETHND.cpy}: the standard operation-result block returned by
 * called modules (218 bytes).
 *
 * @param returnCode {@code RETURN-CODE PIC S9(4) COMP}, see {@link ReturnCode}
 * @param reasonCode {@code REASON-CODE PIC S9(4) COMP}
 * @param moduleId {@code MODULE-ID PIC X(8)}
 * @param functionId {@code FUNCTION-ID PIC X(8)}
 * @param programName {@code PROGRAM-NAME PIC X(8)}
 * @param paragraphName {@code PARAGRAPH-NAME PIC X(8)}
 * @param errorRoutine {@code ERROR-ROUTINE PIC X(8)}
 * @param errorType {@code ERROR-TYPE PIC X(1)}, see {@link ErrorType}
 * @param errorCode {@code ERROR-CODE PIC X(4)}
 * @param errorText {@code ERROR-TEXT PIC X(80)}
 * @param systemCode {@code SYSTEM-CODE PIC X(4)}
 * @param systemMessage {@code SYSTEM-MSG PIC X(80)}
 * @param actionFlag {@code ACTION-FLAG PIC X(1)}, see {@link Action}
 * @param retryCount {@code RETRY-COUNT PIC 9(2) COMP}
 * @param maxRetries {@code MAX-RETRIES PIC 9(2) COMP VALUE 3}
 */
public record ReturnHandling(
    int returnCode,
    int reasonCode,
    String moduleId,
    String functionId,
    String programName,
    String paragraphName,
    String errorRoutine,
    String errorType,
    String errorCode,
    String errorText,
    String systemCode,
    String systemMessage,
    String actionFlag,
    int retryCount,
    int maxRetries)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 218;

  /** Copybook default for {@code MAX-RETRIES}. */
  public static final int DEFAULT_MAX_RETRIES = 3;

  /** {@code ERROR-TYPE} condition names. */
  public enum ErrorType implements CobolCode {
    VALIDATION("V"),
    PROCESSING("P"),
    DATABASE("D"),
    FILE("F"),
    SECURITY("S");

    private final String code;

    ErrorType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves an error type code. */
    public static ErrorType fromCode(String code) {
      return CobolCode.fromCode(ErrorType.class, code);
    }
  }

  /** {@code ACTION-FLAG} condition names. */
  public enum Action implements CobolCode {
    CONTINUE("C"),
    ABORT("A"),
    RETRY("R");

    private final String code;

    Action(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves an action flag. */
    public static Action fromCode(String code) {
      return CobolCode.fromCode(Action.class, code);
    }
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static ReturnHandling parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static ReturnHandling parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, ReturnHandling::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static ReturnHandling read(RecordReader in) {
    return new ReturnHandling(
        in.comp(4, true),
        in.comp(4, true),
        in.alnum(8),
        in.alnum(8),
        in.alnum(8),
        in.alnum(8),
        in.alnum(8),
        in.alnum(1),
        in.alnum(4),
        in.alnum(80),
        in.alnum(4),
        in.alnum(80),
        in.alnum(1),
        in.comp(2, false),
        in.comp(2, false));
  }

  /** The {@link ReturnCode} constant for {@link #returnCode()}. */
  public ReturnCode returnCodeValue() {
    return ReturnCode.fromValue(returnCode);
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.comp(returnCode, 4, true)
        .comp(reasonCode, 4, true)
        .alnum(moduleId, 8)
        .alnum(functionId, 8)
        .alnum(programName, 8)
        .alnum(paragraphName, 8)
        .alnum(errorRoutine, 8)
        .alnum(errorType, 1)
        .alnum(errorCode, 4)
        .alnum(errorText, 80)
        .alnum(systemCode, 4)
        .alnum(systemMessage, 80)
        .alnum(actionFlag, 1)
        .comp(retryCount, 2, false)
        .comp(maxRetries, 2, false);
  }
}
