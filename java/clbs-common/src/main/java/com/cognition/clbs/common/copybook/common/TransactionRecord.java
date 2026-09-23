package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;

/**
 * {@code TRANSACTION-RECORD} from {@code TRNREC.cpy}: the VSAM transaction record (152 bytes, key =
 * date + time + portfolio + sequence).
 *
 * @param transactionDate {@code TRN-DATE PIC X(08)} as {@code YYYYMMDD}
 * @param transactionTime {@code TRN-TIME PIC X(06)} as {@code HHMMSS}
 * @param portfolioId {@code TRN-PORTFOLIO-ID PIC X(08)}
 * @param sequenceNumber {@code TRN-SEQUENCE-NO PIC X(06)}
 * @param investmentId {@code TRN-INVESTMENT-ID PIC X(10)}
 * @param type {@code TRN-TYPE PIC X(02)}, see {@link TransactionType}
 * @param quantity {@code TRN-QUANTITY PIC S9(11)V9(4) COMP-3}
 * @param price {@code TRN-PRICE PIC S9(11)V9(4) COMP-3}
 * @param amount {@code TRN-AMOUNT PIC S9(13)V9(2) COMP-3}
 * @param currency {@code TRN-CURRENCY PIC X(03)}, see {@link CurrencyCode}
 * @param status {@code TRN-STATUS PIC X(01)}, see {@link RecordStatus}
 * @param processTimestamp {@code TRN-PROCESS-DATE PIC X(26)} DB2 timestamp
 * @param processUser {@code TRN-PROCESS-USER PIC X(08)}
 */
public record TransactionRecord(
    String transactionDate,
    String transactionTime,
    String portfolioId,
    String sequenceNumber,
    String investmentId,
    String type,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal amount,
    String currency,
    String status,
    String processTimestamp,
    String processUser)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 152;

  /** Key length in bytes ({@code TRN-KEY}). */
  public static final int KEY_LENGTH = 28;

  /** Total digits of {@code S9(11)V9(4)}. */
  public static final int QUANTITY_DIGITS = 15;

  /** Scale of {@code S9(11)V9(4)}. */
  public static final int QUANTITY_SCALE = 4;

  /** Total digits of {@code S9(13)V9(2)}. */
  public static final int MONEY_DIGITS = 15;

  /** Scale of {@code S9(13)V9(2)}. */
  public static final int MONEY_SCALE = 2;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static TransactionRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static TransactionRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, TransactionRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static TransactionRecord read(RecordReader in) {
    TransactionRecord record =
        new TransactionRecord(
            in.alnum(8),
            in.alnum(6),
            in.alnum(8),
            in.alnum(6),
            in.alnum(10),
            in.alnum(2),
            in.packed(QUANTITY_DIGITS, QUANTITY_SCALE),
            in.packed(QUANTITY_DIGITS, QUANTITY_SCALE),
            in.packed(MONEY_DIGITS, MONEY_SCALE),
            in.alnum(3),
            in.alnum(1),
            in.alnum(26),
            in.alnum(8));
    in.skip(50);
    return record;
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(transactionDate, 8)
        .alnum(transactionTime, 6)
        .alnum(portfolioId, 8)
        .alnum(sequenceNumber, 6)
        .alnum(investmentId, 10)
        .alnum(type, 2)
        .packed(quantity, QUANTITY_DIGITS, QUANTITY_SCALE)
        .packed(price, QUANTITY_DIGITS, QUANTITY_SCALE)
        .packed(amount, MONEY_DIGITS, MONEY_SCALE)
        .alnum(currency, 3)
        .alnum(status, 1)
        .alnum(processTimestamp, 26)
        .alnum(processUser, 8)
        .filler(50);
  }
}
