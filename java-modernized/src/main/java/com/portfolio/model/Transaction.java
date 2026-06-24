package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook TRNREC.cpy (Transaction Record).
 * <p>
 * COBOL record layout (VSAM KSDS, 300-byte fixed-length record):
 * <pre>
 * 01  TRANSACTION-RECORD.
 *     05  TRN-KEY.
 *         10  TRN-DATE           PIC X(08)   [YYYYMMDD]
 *         10  TRN-TIME           PIC X(06)   [HHMMSS]
 *         10  TRN-PORTFOLIO-ID   PIC X(08)
 *         10  TRN-SEQUENCE-NO    PIC X(06)
 *     05  TRN-DATA.
 *         10  TRN-INVESTMENT-ID  PIC X(10)
 *         10  TRN-TYPE           PIC X(02)   [BU=Buy, SL=Sell, TR=Transfer, FE=Fee]
 *         10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3
 *         10  TRN-PRICE          PIC S9(11)V9(4) COMP-3
 *         10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3
 *         10  TRN-CURRENCY       PIC X(03)
 *         10  TRN-STATUS         PIC X(01)   [P=Pending, D=Done, F=Failed, R=Reversed]
 *     05  TRN-AUDIT.
 *         10  TRN-PROCESS-DATE   PIC X(26)
 *         10  TRN-PROCESS-USER   PIC X(08)
 *     05  TRN-FILLER             PIC X(50)
 * </pre>
 */
@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** TRN-DATE — PIC X(08). Transaction date. */
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** TRN-TIME — PIC X(06). Transaction time (HHMMSS). */
    @Column(name = "transaction_time", length = 6)
    private String transactionTime;

    /** TRN-PORTFOLIO-ID — PIC X(08). Foreign key to portfolio. */
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** TRN-SEQUENCE-NO — PIC X(06). Sequence number within same date/time/portfolio. */
    @Column(name = "sequence_no", length = 6)
    private String sequenceNo;

    /** TRN-INVESTMENT-ID — PIC X(10). Investment instrument identifier. */
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    /** TRN-TYPE — PIC X(02). BU=Buy, SL=Sell, TR=Transfer, FE=Fee. */
    @Column(name = "transaction_type", length = 2, nullable = false)
    private String transactionType;

    /** TRN-QUANTITY — PIC S9(11)V9(4) COMP-3. Number of units. */
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** TRN-PRICE — PIC S9(11)V9(4) COMP-3. Price per unit. */
    @Column(name = "price", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    /** TRN-AMOUNT — PIC S9(13)V9(2) COMP-3. Total transaction amount (quantity * price). */
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** TRN-CURRENCY — PIC X(03). ISO 4217 currency code. */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /** TRN-STATUS — PIC X(01). P=Pending, D=Done, F=Failed, R=Reversed. */
    @Column(name = "status", length = 1, nullable = false)
    private String status;

    /** TRN-PROCESS-DATE — PIC X(26). Processing timestamp. */
    @Column(name = "process_date")
    private LocalDateTime processDate;

    /** TRN-PROCESS-USER — PIC X(08). User who processed the transaction. */
    @Column(name = "process_user", length = 8)
    private String processUser;
}
