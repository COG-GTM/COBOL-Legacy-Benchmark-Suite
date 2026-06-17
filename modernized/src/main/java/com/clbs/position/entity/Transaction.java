package com.clbs.position.entity;

import com.clbs.position.domain.TradeInput;
import com.clbs.position.domain.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Transaction entity &mdash; the modern table replacement for the input
 * transaction file processed by the position-update job, whose record layout is
 * copybook {@code src/copybook/common/TRNREC.cpy}:
 *
 * <pre>
 *   01  TRANSACTION-RECORD.
 *       05  TRN-KEY.
 *           10  TRN-DATE           PIC X(08).
 *           10  TRN-TIME           PIC X(06).
 *           10  TRN-PORTFOLIO-ID   PIC X(08).
 *           10  TRN-SEQUENCE-NO    PIC X(06).
 *       05  TRN-DATA.
 *           10  TRN-INVESTMENT-ID  PIC X(10).
 *           10  TRN-TYPE           PIC X(02).
 *           10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *           10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
 *           10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
 *           10  TRN-CURRENCY       PIC X(03).
 *           10  TRN-STATUS         PIC X(01).
 *       05  TRN-AUDIT.
 *           10  TRN-PROCESS-DATE   PIC X(26).
 *           10  TRN-PROCESS-USER   PIC X(08).
 * </pre>
 *
 * <p>{@code TRN-FILLER PIC X(50)} is padding and is not mapped. {@code TRN-STATUS}
 * values: P=Pending, D=Done, F=Failed, R=Reversed.</p>
 */
@Entity
@Table(name = "txn",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_transaction_key",
                columnNames = {"trn_date", "trn_time", "portfolio_id", "sequence_no"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code TRN-DATE PIC X(08)} &mdash; transaction date (YYYYMMDD). */
    @Column(name = "trn_date", length = 8, nullable = false)
    private String trnDate;

    /** {@code TRN-TIME PIC X(06)} &mdash; transaction time (HHMMSS). */
    @Column(name = "trn_time", length = 6, nullable = false)
    private String trnTime;

    /** {@code TRN-PORTFOLIO-ID PIC X(08)} &mdash; portfolio identifier. */
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** {@code TRN-SEQUENCE-NO PIC X(06)} &mdash; sequence within a date/time. */
    @Column(name = "sequence_no", length = 6, nullable = false)
    private String sequenceNo;

    /** {@code TRN-INVESTMENT-ID PIC X(10)} &mdash; investment identifier. */
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    /** {@code TRN-TYPE PIC X(02)} &mdash; BU=Buy, SL=Sell, TR=Transfer, FE=Fee. */
    @Column(name = "trn_type", length = 2, nullable = false)
    private String type;

    /** {@code TRN-QUANTITY PIC S9(11)V9(4) COMP-3} &mdash; transaction quantity. */
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** {@code TRN-PRICE PIC S9(11)V9(4) COMP-3} &mdash; unit price. */
    @Column(name = "price", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    /** {@code TRN-AMOUNT PIC S9(13)V9(2) COMP-3} &mdash; gross amount. */
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** {@code TRN-CURRENCY PIC X(03)} &mdash; ISO currency code. */
    @Column(name = "currency", length = 3)
    private String currency;

    /** {@code TRN-STATUS PIC X(01)} &mdash; P=Pending, D=Done, F=Failed, R=Reversed. */
    @Column(name = "status", length = 1, nullable = false)
    private String status;

    /** {@code TRN-PROCESS-DATE PIC X(26)} &mdash; processing timestamp. */
    @Column(name = "process_date", length = 26)
    private String processDate;

    /** {@code TRN-PROCESS-USER PIC X(08)} &mdash; processing user. */
    @Column(name = "process_user", length = 8)
    private String processUser;

    public TransactionType typeEnum() {
        return TransactionType.fromCode(type);
    }

    /** Calculation-facing view of the transaction. */
    public TradeInput toTradeInput() {
        return new TradeInput(typeEnum(), quantity, price, amount);
    }
}
