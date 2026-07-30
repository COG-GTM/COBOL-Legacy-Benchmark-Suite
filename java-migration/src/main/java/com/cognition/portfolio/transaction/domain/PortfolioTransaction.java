package com.cognition.portfolio.transaction.domain;

import com.cognition.portfolio.traceability.CobolOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Portfolio transaction, migrated one-for-one from {@code 01 TRANSACTION-RECORD} in
 * {@code TRNREC.cpy} (VSAM KSDS {@code TRANHIST}).
 *
 * <p>Every field below names its COBOL field and PIC clause. Byte offsets are listed in
 * MIGRATION-NOTES.md. {@code TRN-FILLER PIC X(50)} is record padding and is deliberately not
 * mapped.
 */
@Entity
@Table(
    name = "portfolio_transaction",
    indexes = {
      @Index(name = "idx_trn_portfolio_id", columnList = "trn_portfolio_id"),
      @Index(name = "idx_trn_status", columnList = "trn_status"),
      @Index(name = "idx_trn_investment_id", columnList = "trn_investment_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@CobolOrigin(program = "TRNREC", paragraph = "01 TRANSACTION-RECORD")
public class PortfolioTransaction {

  /** COBOL: {@code 05 TRN-KEY} — composite VSAM key, bytes 1-28. */
  @EmbeddedId
  @Valid
  @NotNull
  private TransactionKey trnKey;

  /** COBOL: {@code TRN-INVESTMENT-ID PIC X(10)} — investment identifier, bytes 29-38. */
  @Column(name = "trn_investment_id", length = 10, nullable = false)
  @NotNull
  private String trnInvestmentId;

  /** COBOL: {@code TRN-TYPE PIC X(02)} — BU/SL/TR/FE (88-levels), bytes 39-40. */
  @Column(name = "trn_type", length = 2, nullable = false)
  @Convert(converter = TransactionTypeConverter.class)
  @NotNull
  private TransactionType trnType;

  /** COBOL: {@code TRN-QUANTITY PIC S9(11)V9(4) COMP-3} — 15 digits in 8 packed bytes, 41-48. */
  @Column(name = "trn_quantity", precision = 15, scale = 4, nullable = false)
  @NotNull
  private BigDecimal trnQuantity;

  /** COBOL: {@code TRN-PRICE PIC S9(11)V9(4) COMP-3} — 15 digits in 8 packed bytes, 49-56. */
  @Column(name = "trn_price", precision = 15, scale = 4, nullable = false)
  @NotNull
  private BigDecimal trnPrice;

  /** COBOL: {@code TRN-AMOUNT PIC S9(13)V9(2) COMP-3} — 15 digits in 8 packed bytes, 57-64. */
  @Column(name = "trn_amount", precision = 15, scale = 2, nullable = false)
  @NotNull
  private BigDecimal trnAmount;

  /** COBOL: {@code TRN-CURRENCY PIC X(03)} — ISO currency code, bytes 65-67. */
  @Column(name = "trn_currency", length = 3, nullable = false)
  @NotNull
  private String trnCurrency;

  /** COBOL: {@code TRN-STATUS PIC X(01)} — P/D/F/R (88-levels), byte 68. */
  @Column(name = "trn_status", length = 1, nullable = false)
  @Convert(converter = TransactionStatusConverter.class)
  @NotNull
  private TransactionStatus trnStatus;

  /** COBOL: {@code TRN-PROCESS-DATE PIC X(26)} — audit timestamp, bytes 69-94. */
  @Column(name = "trn_process_date", length = 26)
  private String trnProcessDate;

  /** COBOL: {@code TRN-PROCESS-USER PIC X(08)} — audit user id, bytes 95-102. */
  @Column(name = "trn_process_user", length = 8)
  private String trnProcessUser;

  /** Convenience accessor for {@code TRN-PORTFOLIO-ID}, which lives inside {@code TRN-KEY}. */
  public String getPortfolioId() {
    return trnKey == null ? null : trnKey.getTrnPortfolioId();
  }
}
