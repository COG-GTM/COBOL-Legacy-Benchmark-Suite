package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;

/**
 * {@code POSITION-RECORD} from {@code POSREC.cpy}: the VSAM position master record (138 bytes, key
 * = portfolio + date + investment).
 *
 * @param portfolioId {@code POS-PORTFOLIO-ID PIC X(08)}
 * @param positionDate {@code POS-DATE PIC X(08)} as {@code YYYYMMDD}
 * @param investmentId {@code POS-INVESTMENT-ID PIC X(10)}
 * @param quantity {@code POS-QUANTITY PIC S9(11)V9(4) COMP-3}
 * @param costBasis {@code POS-COST-BASIS PIC S9(13)V9(2) COMP-3}
 * @param marketValue {@code POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3}
 * @param currency {@code POS-CURRENCY PIC X(03)}, see {@link CurrencyCode}
 * @param status {@code POS-STATUS PIC X(01)}, see {@link RecordStatus}
 * @param lastMaintTimestamp {@code POS-LAST-MAINT-DATE PIC X(26)} DB2 timestamp
 * @param lastMaintUser {@code POS-LAST-MAINT-USER PIC X(08)}
 */
public record PositionRecord(
    String portfolioId,
    String positionDate,
    String investmentId,
    BigDecimal quantity,
    BigDecimal costBasis,
    BigDecimal marketValue,
    String currency,
    String status,
    String lastMaintTimestamp,
    String lastMaintUser)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 138;

  /** Key length in bytes ({@code POS-KEY}). */
  public static final int KEY_LENGTH = 26;

  /** Total digits of {@code S9(11)V9(4)}. */
  public static final int QUANTITY_DIGITS = 15;

  /** Scale of {@code S9(11)V9(4)}. */
  public static final int QUANTITY_SCALE = 4;

  /** Total digits of {@code S9(13)V9(2)}. */
  public static final int MONEY_DIGITS = 15;

  /** Scale of {@code S9(13)V9(2)}. */
  public static final int MONEY_SCALE = 2;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static PositionRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static PositionRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, PositionRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static PositionRecord read(RecordReader in) {
    PositionRecord record =
        new PositionRecord(
            in.alnum(8),
            in.alnum(8),
            in.alnum(10),
            in.packed(QUANTITY_DIGITS, QUANTITY_SCALE),
            in.packed(MONEY_DIGITS, MONEY_SCALE),
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
    out.alnum(portfolioId, 8)
        .alnum(positionDate, 8)
        .alnum(investmentId, 10)
        .packed(quantity, QUANTITY_DIGITS, QUANTITY_SCALE)
        .packed(costBasis, MONEY_DIGITS, MONEY_SCALE)
        .packed(marketValue, MONEY_DIGITS, MONEY_SCALE)
        .alnum(currency, 3)
        .alnum(status, 1)
        .alnum(lastMaintTimestamp, 26)
        .alnum(lastMaintUser, 8)
        .filler(50);
  }
}
