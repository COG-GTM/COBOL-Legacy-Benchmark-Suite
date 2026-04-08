package com.portfolio.model;

import com.portfolio.model.enums.TransactionStatus;
import com.portfolio.model.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * JPA entity mapping the TRNREC.cpy copybook record structure.
 *
 * Original COBOL layout (src/copybook/common/TRNREC.cpy):
 * <pre>
 *  01  TRANSACTION-RECORD.
 *      05  TRN-KEY.
 *          10  TRN-DATE           PIC X(08).   -- YYYYMMDD
 *          10  TRN-TIME           PIC X(06).   -- HHMMSS
 *          10  TRN-PORTFOLIO-ID   PIC X(08).
 *          10  TRN-SEQUENCE-NO    PIC X(06).
 *      05  TRN-DATA.
 *          10  TRN-INVESTMENT-ID  PIC X(10).
 *          10  TRN-TYPE           PIC X(02).   -- BU/SL/TR/FE
 *          10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *          10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
 *          10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
 *          10  TRN-CURRENCY       PIC X(03).
 *          10  TRN-STATUS         PIC X(01).   -- P/D/F/R
 *      05  TRN-AUDIT.
 *          10  TRN-PROCESS-DATE   PIC X(26).   -- ISO timestamp
 *          10  TRN-PROCESS-USER   PIC X(08).
 * </pre>
 */
@Entity
@Table(name = "transaction_record")
@IdClass(TransactionRecordKey.class)
public class TransactionRecord {

    /** TRN-DATE PIC X(08) - YYYYMMDD */
    @Id
    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    /** TRN-TIME PIC X(06) - HHMMSS */
    @Id
    @Column(name = "trans_time", nullable = false)
    private LocalTime transTime;

    /** TRN-PORTFOLIO-ID PIC X(08) */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** TRN-SEQUENCE-NO PIC X(06) */
    @Id
    @Column(name = "sequence_no", length = 6, nullable = false)
    private String sequenceNo;

    /** TRN-INVESTMENT-ID PIC X(10) */
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    /** TRN-TYPE PIC X(02) with 88-levels: BU=Buy, SL=Sell, TR=Transfer, FE=Fee */
    @Column(name = "trans_type", length = 2)
    private TransactionType type;

    /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3 */
    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    /** TRN-PRICE PIC S9(11)V9(4) COMP-3 */
    @Column(name = "price", precision = 15, scale = 4)
    private BigDecimal price;

    /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3 */
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /** TRN-CURRENCY PIC X(03) */
    @Column(name = "currency", length = 3)
    private String currency;

    /** TRN-STATUS PIC X(01) with 88-levels: P=Pending, D=Done, F=Failed, R=Reversed */
    @Column(name = "trans_status", length = 1)
    private TransactionStatus status;

    /** TRN-PROCESS-DATE PIC X(26) - ISO timestamp */
    @Column(name = "process_timestamp")
    private Instant processTimestamp;

    /** TRN-PROCESS-USER PIC X(08) */
    @Column(name = "process_user", length = 8)
    private String processUser;

    public TransactionRecord() {
    }

    public LocalDate getTransDate() {
        return transDate;
    }

    public void setTransDate(LocalDate transDate) {
        this.transDate = transDate;
    }

    public LocalTime getTransTime() {
        return transTime;
    }

    public void setTransTime(LocalTime transTime) {
        this.transTime = transTime;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getProcessTimestamp() {
        return processTimestamp;
    }

    public void setProcessTimestamp(Instant processTimestamp) {
        this.processTimestamp = processTimestamp;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
