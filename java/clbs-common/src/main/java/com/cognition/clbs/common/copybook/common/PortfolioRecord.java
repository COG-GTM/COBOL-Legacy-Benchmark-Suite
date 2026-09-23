package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;

/**
 * {@code PORT-RECORD} from {@code PORTFLIO.cpy}: the VSAM portfolio master record (148 bytes, key =
 * {@code PORT-ID + PORT-ACCOUNT-NO}).
 *
 * @param portfolioId {@code PORT-ID PIC X(8)}
 * @param accountNumber {@code PORT-ACCOUNT-NO PIC X(10)}
 * @param clientName {@code PORT-CLIENT-NAME PIC X(30)}
 * @param clientType {@code PORT-CLIENT-TYPE PIC X(1)}
 * @param createDate {@code PORT-CREATE-DATE PIC 9(8)} as {@code YYYYMMDD}
 * @param lastMaintDate {@code PORT-LAST-MAINT PIC 9(8)} as {@code YYYYMMDD}
 * @param status {@code PORT-STATUS PIC X(1)}, see {@link RecordStatus}
 * @param totalValue {@code PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3}
 * @param cashBalance {@code PORT-CASH-BALANCE PIC S9(13)V99 COMP-3}
 * @param lastUser {@code PORT-LAST-USER PIC X(8)}
 * @param lastTransactionDate {@code PORT-LAST-TRANS PIC 9(8)} as {@code YYYYMMDD}
 */
public record PortfolioRecord(
    String portfolioId,
    String accountNumber,
    String clientName,
    String clientType,
    int createDate,
    int lastMaintDate,
    String status,
    BigDecimal totalValue,
    BigDecimal cashBalance,
    String lastUser,
    int lastTransactionDate)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 148;

  /** Key length in bytes ({@code PORT-KEY}). */
  public static final int KEY_LENGTH = 18;

  /** Digits and scale of the {@code S9(13)V99} money fields. */
  public static final int MONEY_DIGITS = 15;

  /** Scale of the {@code S9(13)V99} money fields. */
  public static final int MONEY_SCALE = 2;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static PortfolioRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static PortfolioRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, PortfolioRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static PortfolioRecord read(RecordReader in) {
    PortfolioRecord record =
        new PortfolioRecord(
            in.alnum(8),
            in.alnum(10),
            in.alnum(30),
            in.alnum(1),
            in.display(8),
            in.display(8),
            in.alnum(1),
            in.packed(MONEY_DIGITS, MONEY_SCALE),
            in.packed(MONEY_DIGITS, MONEY_SCALE),
            in.alnum(8),
            in.display(8));
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
        .alnum(accountNumber, 10)
        .alnum(clientName, 30)
        .alnum(clientType, 1)
        .display(createDate, 8)
        .display(lastMaintDate, 8)
        .alnum(status, 1)
        .packed(totalValue, MONEY_DIGITS, MONEY_SCALE)
        .packed(cashBalance, MONEY_DIGITS, MONEY_SCALE)
        .alnum(lastUser, 8)
        .display(lastTransactionDate, 8)
        .filler(50);
  }
}
