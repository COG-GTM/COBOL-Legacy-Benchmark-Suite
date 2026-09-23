package com.cognition.clbs.common.copybook.db2;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;

/**
 * {@code POSHIST-RECORD} from {@code DBTBLS.cpy}: host-variable layout for the DB2 {@code POSHIST}
 * table (166 bytes). Dates and times are DB2 character forms ({@code YYYY-MM-DD}, {@code
 * HH.MM.SS}).
 *
 * @param accountNumber {@code PH-ACCOUNT-NO PIC X(8)}
 * @param portfolioId {@code PH-PORTFOLIO-ID PIC X(10)}
 * @param transactionDate {@code PH-TRANS-DATE PIC X(10)}
 * @param transactionTime {@code PH-TRANS-TIME PIC X(8)}
 * @param transactionType {@code PH-TRANS-TYPE PIC X(2)}
 * @param securityId {@code PH-SECURITY-ID PIC X(12)}
 * @param quantity {@code PH-QUANTITY PIC S9(12)V9(3) COMP-3}
 * @param price {@code PH-PRICE PIC S9(12)V9(3) COMP-3}
 * @param amount {@code PH-AMOUNT PIC S9(13)V9(2) COMP-3}
 * @param fees {@code PH-FEES PIC S9(13)V9(2) COMP-3}
 * @param totalAmount {@code PH-TOTAL-AMOUNT PIC S9(13)V9(2) COMP-3}
 * @param costBasis {@code PH-COST-BASIS PIC S9(13)V9(2) COMP-3}
 * @param gainLoss {@code PH-GAIN-LOSS PIC S9(13)V9(2) COMP-3}
 * @param processDate {@code PH-PROCESS-DATE PIC X(10)}
 * @param processTime {@code PH-PROCESS-TIME PIC X(8)}
 * @param programId {@code PH-PROGRAM-ID PIC X(8)}
 * @param userId {@code PH-USER-ID PIC X(8)}
 * @param auditTimestamp {@code PH-AUDIT-TIMESTAMP PIC X(26)}
 */
public record PositionHistoryRecord(
    String accountNumber,
    String portfolioId,
    String transactionDate,
    String transactionTime,
    String transactionType,
    String securityId,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal amount,
    BigDecimal fees,
    BigDecimal totalAmount,
    BigDecimal costBasis,
    BigDecimal gainLoss,
    String processDate,
    String processTime,
    String programId,
    String userId,
    String auditTimestamp)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 166;

  /** Total digits of {@code S9(12)V9(3)}. */
  public static final int QUANTITY_DIGITS = 15;

  /** Scale of {@code S9(12)V9(3)}. */
  public static final int QUANTITY_SCALE = 3;

  /** Total digits of {@code S9(13)V9(2)}. */
  public static final int MONEY_DIGITS = 15;

  /** Scale of {@code S9(13)V9(2)}. */
  public static final int MONEY_SCALE = 2;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static PositionHistoryRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static PositionHistoryRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, PositionHistoryRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static PositionHistoryRecord read(RecordReader in) {
    return new PositionHistoryRecord(
        in.alnum(8),
        in.alnum(10),
        in.alnum(10),
        in.alnum(8),
        in.alnum(2),
        in.alnum(12),
        in.packed(QUANTITY_DIGITS, QUANTITY_SCALE),
        in.packed(QUANTITY_DIGITS, QUANTITY_SCALE),
        in.packed(MONEY_DIGITS, MONEY_SCALE),
        in.packed(MONEY_DIGITS, MONEY_SCALE),
        in.packed(MONEY_DIGITS, MONEY_SCALE),
        in.packed(MONEY_DIGITS, MONEY_SCALE),
        in.packed(MONEY_DIGITS, MONEY_SCALE),
        in.alnum(10),
        in.alnum(8),
        in.alnum(8),
        in.alnum(8),
        in.alnum(26));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(accountNumber, 8)
        .alnum(portfolioId, 10)
        .alnum(transactionDate, 10)
        .alnum(transactionTime, 8)
        .alnum(transactionType, 2)
        .alnum(securityId, 12)
        .packed(quantity, QUANTITY_DIGITS, QUANTITY_SCALE)
        .packed(price, QUANTITY_DIGITS, QUANTITY_SCALE)
        .packed(amount, MONEY_DIGITS, MONEY_SCALE)
        .packed(fees, MONEY_DIGITS, MONEY_SCALE)
        .packed(totalAmount, MONEY_DIGITS, MONEY_SCALE)
        .packed(costBasis, MONEY_DIGITS, MONEY_SCALE)
        .packed(gainLoss, MONEY_DIGITS, MONEY_SCALE)
        .alnum(processDate, 10)
        .alnum(processTime, 8)
        .alnum(programId, 8)
        .alnum(userId, 8)
        .alnum(auditTimestamp, 26);
  }
}
