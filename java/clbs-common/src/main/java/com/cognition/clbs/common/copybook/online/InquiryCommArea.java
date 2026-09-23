package com.cognition.clbs.common.copybook.online;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code INQCOM-AREA} from {@code INQCOM.cpy}: the COMMAREA of the online inquiry transactions (98
 * bytes).
 *
 * @param function {@code INQCOM-FUNCTION PIC X(4)}, see {@link Function}
 * @param accountNumber {@code INQCOM-ACCOUNT-NO PIC X(10)}
 * @param responseCode {@code INQCOM-RESPONSE-CODE PIC S9(8) COMP}
 * @param errorMessage {@code INQCOM-ERROR-MSG PIC X(80)}
 */
public record InquiryCommArea(
    String function, String accountNumber, int responseCode, String errorMessage)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 98;

  /** {@code INQCOM-FUNCTION} condition names. */
  public enum Function implements CobolCode {
    MENU("MENU"),
    PORTFOLIO("INQP"),
    HISTORY("INQH"),
    EXIT("EXIT");

    private final String code;

    Function(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves an inquiry function. */
    public static Function fromCode(String code) {
      return CobolCode.fromCode(Function.class, code);
    }
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static InquiryCommArea parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static InquiryCommArea parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, InquiryCommArea::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static InquiryCommArea read(RecordReader in) {
    return new InquiryCommArea(in.alnum(4), in.alnum(10), in.comp(8, true), in.alnum(80));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(function, 4)
        .alnum(accountNumber, 10)
        .comp(responseCode, 8, true)
        .alnum(errorMessage, 80);
  }
}
