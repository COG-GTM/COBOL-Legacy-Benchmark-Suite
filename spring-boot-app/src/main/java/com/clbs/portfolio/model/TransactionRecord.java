package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction record.
 * From COBOL copybook: src/copybook/common/TRNREC.cpy (TRANSACTION-RECORD).
 */
@Entity
@Table(name = "transaction_record")
@IdClass(TransactionRecordId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

    /** TRN-DATE — PIC X(08), YYYYMMDD */
    @Id
    @Column(name = "trn_date", length = 8, nullable = false)
    private String trnDate;

    /** TRN-TIME — PIC X(06), HHMMSS */
    @Id
    @Column(name = "trn_time", length = 6, nullable = false)
    private String trnTime;

    /** TRN-PORTFOLIO-ID — PIC X(08) */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** TRN-SEQUENCE-NO — PIC X(06) */
    @Id
    @Column(name = "sequence_no", length = 6, nullable = false)
    private String sequenceNo;

    /** TRN-INVESTMENT-ID — PIC X(10) */
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    /** TRN-TYPE — PIC X(02): BU=Buy, SL=Sell, TR=Transfer, FE=Fee */
    @Enumerated(EnumType.STRING)
    @Column(name = "trn_type", length = 10, nullable = false)
    private TransactionType type;

    /** TRN-QUANTITY — PIC S9(11)V9(4) COMP-3 */
    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    /** TRN-PRICE — PIC S9(11)V9(4) COMP-3 */
    @Column(name = "price", precision = 15, scale = 4)
    private BigDecimal price;

    /** TRN-AMOUNT — PIC S9(13)V9(2) COMP-3 */
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /** TRN-CURRENCY — PIC X(03) */
    @Column(name = "currency", length = 3)
    private String currency;

    /** TRN-STATUS — PIC X(01): P=Pending, D=Done, F=Failed, R=Reversed */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private TransactionStatus status;

    /** TRN-PROCESS-DATE — PIC X(26) */
    @Column(name = "process_date")
    private LocalDateTime processDate;

    /** TRN-PROCESS-USER — PIC X(08) */
    @Column(name = "process_user", length = 8)
    private String processUser;

    public enum TransactionType {
        BUY,
        SELL,
        TRANSFER,
        FEE
    }

    public enum TransactionStatus {
        PENDING,
        DONE,
        FAILED,
        REVERSED
    }
}
