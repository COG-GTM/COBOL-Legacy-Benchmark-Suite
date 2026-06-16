package com.clbs.portfolio.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input transaction record processed by PORTTRAN, mapped from copybook
 * {@code TRNREC} ({@code 01 TRANSACTION-RECORD}). This is the sequential
 * {@code TRANSACTION-FILE} record; it is a transient input message, not a
 * persisted entity.
 *
 * <p>Field mapping (copybook field &rarr; Java):</p>
 * <ul>
 *   <li>{@code TRN-DATE         PIC X(08)}         &rarr; {@link #transactionDate}</li>
 *   <li>{@code TRN-TIME         PIC X(06)}         &rarr; {@link #transactionTime}</li>
 *   <li>{@code TRN-PORTFOLIO-ID PIC X(08)}         &rarr; {@link #portfolioId}</li>
 *   <li>{@code TRN-SEQUENCE-NO  PIC X(06)}         &rarr; {@link #sequenceNo}</li>
 *   <li>{@code TRN-INVESTMENT-ID PIC X(10)}        &rarr; {@link #investmentId}</li>
 *   <li>{@code TRN-TYPE         PIC X(02)}         &rarr; {@link #type} (raw BU/SL/TR/FE)</li>
 *   <li>{@code TRN-QUANTITY     PIC S9(11)V9(4) COMP-3} &rarr; {@link #quantity} (scale 4)</li>
 *   <li>{@code TRN-PRICE        PIC S9(11)V9(4) COMP-3} &rarr; {@link #price} (scale 4)</li>
 *   <li>{@code TRN-AMOUNT       PIC S9(13)V9(2) COMP-3} &rarr; {@link #amount} (scale 2)</li>
 *   <li>{@code TRN-CURRENCY     PIC X(03)}         &rarr; {@link #currency}</li>
 *   <li>{@code TRN-STATUS       PIC X(01)}         &rarr; {@link #status} (P/D/F/R)</li>
 * </ul>
 *
 * <p>The composite COBOL key {@code TRN-KEY = TRN-DATE + TRN-TIME +
 * TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO} uniquely identifies a transaction and is
 * the natural duplicate-detection key (see {@link #naturalKey()}).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTransaction {

    /** {@code TRN-DATE PIC X(08)} — transaction date (YYYYMMDD). */
    private String transactionDate;

    /** {@code TRN-TIME PIC X(06)} — transaction time (HHMMSS). */
    private String transactionTime;

    /** {@code TRN-PORTFOLIO-ID PIC X(08)} — portfolio identifier. */
    private String portfolioId;

    /** {@code TRN-SEQUENCE-NO PIC X(06)} — sequence number for multiple transactions. */
    private String sequenceNo;

    /** {@code TRN-INVESTMENT-ID PIC X(10)} — investment identifier. */
    private String investmentId;

    /** {@code TRN-TYPE PIC X(02)} — raw type code: BU/SL/TR/FE. */
    private String type;

    /** {@code TRN-QUANTITY PIC S9(11)V9(4) COMP-3} — number of units. */
    private BigDecimal quantity;

    /** {@code TRN-PRICE PIC S9(11)V9(4) COMP-3} — unit price. */
    private BigDecimal price;

    /** {@code TRN-AMOUNT PIC S9(13)V9(2) COMP-3} — monetary amount. */
    private BigDecimal amount;

    /** {@code TRN-CURRENCY PIC X(03)} — ISO currency code. */
    private String currency;

    /** {@code TRN-STATUS PIC X(01)} — P=pending, D=done, F=failed, R=reversed. */
    private String status;

    /** @return the resolved {@link TransactionType}, or {@code null} if {@link #type} is invalid. */
    public TransactionType resolvedType() {
        return TransactionType.fromCode(type);
    }

    /**
     * @return the composite COBOL {@code TRN-KEY} used as the natural
     *         duplicate-transaction key.
     */
    public String naturalKey() {
        return String.format("%s|%s|%s|%s",
                transactionDate, transactionTime, portfolioId, sequenceNo);
    }
}
