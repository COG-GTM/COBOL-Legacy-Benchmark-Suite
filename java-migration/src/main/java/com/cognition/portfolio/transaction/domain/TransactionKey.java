package com.cognition.portfolio.transaction.domain;

import com.cognition.portfolio.traceability.CobolOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Composite primary key, from {@code 05 TRN-KEY} in {@code TRNREC.cpy} — the 28-byte VSAM KSDS key
 * of the {@code TRANHIST} file.
 *
 * <p>All four components stay character fields (not {@code LocalDate}/{@code Long}) so that the
 * JVM sort order is byte-for-byte identical to the VSAM key sequence the batch programs rely on
 * ({@code PORTTRAN 2000-PROCESS-TRANSACTIONS} reads the file in key order).
 */
@Embeddable
@CobolOrigin(program = "TRNREC", paragraph = "05 TRN-KEY", rules = {"BR-21"})
public class TransactionKey implements Serializable, Comparable<TransactionKey> {

  private static final long serialVersionUID = 1L;

  /** Byte length of {@code TRN-KEY}: 8 + 6 + 8 + 6. */
  public static final int KEY_LENGTH = 28;

  private static final DateTimeFormatter COBOL_DATE =
      DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter COBOL_TIME =
      DateTimeFormatter.ofPattern("HHmmss").withResolverStyle(ResolverStyle.STRICT);

  private static final Comparator<TransactionKey> VSAM_ORDER =
      Comparator.comparing(TransactionKey::getTrnDate)
          .thenComparing(TransactionKey::getTrnTime)
          .thenComparing(TransactionKey::getTrnPortfolioId)
          .thenComparing(TransactionKey::getTrnSequenceNo);

  /** COBOL: {@code TRN-DATE PIC X(08)} — transaction date, {@code YYYYMMDD}, offset 1-8. */
  @Column(name = "trn_date", length = 8, nullable = false)
  @Pattern(regexp = "\\d{8}", message = "TRN-DATE must be 8 digits (YYYYMMDD)")
  private String trnDate;

  /** COBOL: {@code TRN-TIME PIC X(06)} — transaction time, {@code HHMMSS}, offset 9-14. */
  @Column(name = "trn_time", length = 6, nullable = false)
  @Pattern(regexp = "\\d{6}", message = "TRN-TIME must be 6 digits (HHMMSS)")
  private String trnTime;

  /** COBOL: {@code TRN-PORTFOLIO-ID PIC X(08)} — portfolio identifier, offset 15-22. */
  @Column(name = "trn_portfolio_id", length = 8, nullable = false)
  private String trnPortfolioId;

  /** COBOL: {@code TRN-SEQUENCE-NO PIC X(06)} — sequence within date/portfolio, offset 23-28. */
  @Column(name = "trn_sequence_no", length = 6, nullable = false)
  @Pattern(regexp = "\\d{6}", message = "TRN-SEQUENCE-NO must be 6 digits")
  private String trnSequenceNo;

  protected TransactionKey() {
    // JPA
  }

  public TransactionKey(String trnDate, String trnTime, String trnPortfolioId, String trnSequenceNo) {
    this.trnDate = trnDate;
    this.trnTime = trnTime;
    this.trnPortfolioId = trnPortfolioId;
    this.trnSequenceNo = trnSequenceNo;
  }

  public String getTrnDate() {
    return trnDate;
  }

  public String getTrnTime() {
    return trnTime;
  }

  public String getTrnPortfolioId() {
    return trnPortfolioId;
  }

  public String getTrnSequenceNo() {
    return trnSequenceNo;
  }

  /**
   * {@code TRN-DATE} as a date; the copybook documents the field as {@code YYYYMMDD} but declares
   * it {@code PIC X(08)}, so a stored value need not be a real calendar date (OQ-11). Empty for
   * anything that does not parse, so a read path never fails on legacy data.
   */
  public Optional<LocalDate> getTransactionDate() {
    try {
      return Optional.of(LocalDate.parse(trnDate, COBOL_DATE));
    } catch (DateTimeParseException | NullPointerException e) {
      return Optional.empty();
    }
  }

  /** {@code TRN-TIME} as a time; {@code PIC X(06)}, so the same caveat as {@code TRN-DATE} applies. */
  public Optional<LocalTime> getTransactionTime() {
    try {
      return Optional.of(LocalTime.parse(trnTime, COBOL_TIME));
    } catch (DateTimeParseException | NullPointerException e) {
      return Optional.empty();
    }
  }

  /**
   * The concatenated 28-character VSAM key, exactly as {@code TRN-KEY} is laid out on the
   * {@code TRANHIST} KSDS. Used as the path variable for keyed reads.
   */
  @CobolOrigin(program = "TRNREC", paragraph = "05 TRN-KEY", rules = {"BR-21"})
  public String toKeyString() {
    return trnDate + trnTime + trnPortfolioId + trnSequenceNo;
  }

  /**
   * Parses the 28-character VSAM key layout ({@code TRN-DATE(8) TRN-TIME(6) TRN-PORTFOLIO-ID(8)
   * TRN-SEQUENCE-NO(6)}).
   *
   * @throws IllegalArgumentException when the value is not exactly 28 characters
   */
  @CobolOrigin(program = "TRNREC", paragraph = "05 TRN-KEY", rules = {"BR-21"})
  public static TransactionKey parse(String key) {
    if (key == null || key.length() != KEY_LENGTH) {
      throw new IllegalArgumentException(
          "TRN-KEY must be exactly " + KEY_LENGTH + " characters (YYYYMMDDHHMMSS + 8 char portfolio id + 6 digit sequence)");
    }
    return new TransactionKey(
        key.substring(0, 8), key.substring(8, 14), key.substring(14, 22), key.substring(22, 28));
  }

  /** VSAM key sequence: date, time, portfolio id, sequence number. */
  @Override
  @CobolOrigin(program = "PORTTRAN", paragraph = "2000-PROCESS-TRANSACTIONS", rules = {"BR-21"})
  public int compareTo(TransactionKey other) {
    return VSAM_ORDER.compare(this, other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TransactionKey other)) {
      return false;
    }
    return Objects.equals(trnDate, other.trnDate)
        && Objects.equals(trnTime, other.trnTime)
        && Objects.equals(trnPortfolioId, other.trnPortfolioId)
        && Objects.equals(trnSequenceNo, other.trnSequenceNo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trnDate, trnTime, trnPortfolioId, trnSequenceNo);
  }

  @Override
  public String toString() {
    return toKeyString();
  }
}
