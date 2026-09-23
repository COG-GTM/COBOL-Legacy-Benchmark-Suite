package com.cognition.clbs.common.copybook.db2;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code DB2-ERROR-HANDLING} from {@code DBPROC.cpy} (118 bytes). The {@code DB2-ERROR-MESSAGE}
 * group interleaves literal FILLER text with the variable parts; {@link #formatMessage()} renders
 * the same 107-byte string the COBOL program moves to {@code ERR-TEXT}. The paragraphs in the
 * copybook (CONNECT-TO-DB2, DB2-ERROR-ROUTINE, ...) are platform glue with no Java counterpart.
 *
 * @param sqlCodeText {@code DB2-SQLCODE-TXT PIC X(6)}
 * @param sqlState {@code DB2-STATE PIC X(5)}
 * @param errorText {@code DB2-ERROR-TEXT PIC X(70)}
 * @param savedStatus {@code DB2-SAVE-STATUS PIC X(5)}
 * @param retryCount {@code DB2-RETRY-COUNT PIC S9(4) COMP VALUE 0}
 * @param maxRetries {@code DB2-MAX-RETRIES PIC S9(4) COMP VALUE 3}
 * @param retryWait {@code DB2-RETRY-WAIT PIC S9(4) COMP VALUE 100}
 */
public record Db2ErrorHandling(
    String sqlCodeText,
    String sqlState,
    String errorText,
    String savedStatus,
    int retryCount,
    int maxRetries,
    int retryWait)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 118;

  /** Width of the composed {@code DB2-ERROR-MESSAGE} group. */
  public static final int MESSAGE_LENGTH = 107;

  /** Copybook default for {@code DB2-MAX-RETRIES}. */
  public static final int DEFAULT_MAX_RETRIES = 3;

  /** Copybook default for {@code DB2-RETRY-WAIT}. */
  public static final int DEFAULT_RETRY_WAIT = 100;

  private static final String SQLCODE_LABEL = "SQLCODE: ";
  private static final String STATE_LABEL = " STATE: ";
  private static final String ERROR_LABEL = " ERROR: ";

  /** Parses a GnuCOBOL (ASCII) record. */
  public static Db2ErrorHandling parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static Db2ErrorHandling parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, Db2ErrorHandling::read);
  }

  /** Reads the fields in copybook order from {@code in}, skipping the literal FILLER text. */
  public static Db2ErrorHandling read(RecordReader in) {
    in.skip(SQLCODE_LABEL.length());
    String sqlCodeText = in.alnum(6);
    in.skip(9);
    String sqlState = in.alnum(5);
    in.skip(ERROR_LABEL.length());
    String errorText = in.alnum(70);
    return new Db2ErrorHandling(
        sqlCodeText,
        sqlState,
        errorText,
        in.alnum(5),
        in.comp(4, true),
        in.comp(4, true),
        in.comp(4, true));
  }

  /** The 107-byte {@code DB2-ERROR-MESSAGE} text, padded exactly as COBOL would store it. */
  public String formatMessage() {
    return SQLCODE_LABEL
        + pad(sqlCodeText, 6)
        + pad(STATE_LABEL, 9)
        + pad(sqlState, 5)
        + ERROR_LABEL
        + pad(errorText, 70);
  }

  private static String pad(String value, int width) {
    String text = value == null ? "" : value;
    if (text.length() >= width) {
      return text.substring(0, width);
    }
    return text + " ".repeat(width - text.length());
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(SQLCODE_LABEL, 9)
        .alnum(sqlCodeText, 6)
        .alnum(STATE_LABEL, 9)
        .alnum(sqlState, 5)
        .alnum(ERROR_LABEL, 8)
        .alnum(errorText, 70)
        .alnum(savedStatus, 5)
        .comp(retryCount, 4, true)
        .comp(maxRetries, 4, true)
        .comp(retryWait, 4, true);
  }
}
