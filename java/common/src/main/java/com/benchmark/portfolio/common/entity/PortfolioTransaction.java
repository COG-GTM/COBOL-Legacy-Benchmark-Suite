package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portfolio transaction record, migrated from TRNREC.cpy (TRANSACTION-RECORD).
 * VSAM KSDS with RECORD KEY TRN-KEY = TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO,
 * mapped to table PORTFOLIO_TRANSACTION with composite PK
 * (TRANS_DATE, TRANS_TIME, PORTFOLIO_ID, SEQUENCE_NO).
 * TRN-FILLER PIC X(50) is reserved space and is not migrated.
 */
@Entity
@Table(name = "PORTFOLIO_TRANSACTION")
public class PortfolioTransaction {

    /** TRN-KEY = TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO. */
    @EmbeddedId
    private PortfolioTransactionId id;

    /** TRN-INVESTMENT-ID PIC X(10). */
    @Column(name = "INVESTMENT_ID", columnDefinition = "CHAR(10)", length = 10, nullable = false)
    private String investmentId;

    /** TRN-TYPE PIC X(02); 88-levels: 'BU' Buy, 'SL' Sell, 'TR' Transfer, 'FE' Fee. */
    @Column(name = "TRANS_TYPE", columnDefinition = "CHAR(2)", length = 2, nullable = false)
    private String transType;

    /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3. */
    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** TRN-PRICE PIC S9(11)V9(4) COMP-3. */
    @Column(name = "PRICE", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3. */
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** TRN-CURRENCY PIC X(03) (ISO 4217 code). */
    @Column(name = "CURRENCY_CODE", columnDefinition = "CHAR(3)", length = 3, nullable = false)
    private String currencyCode;

    /** TRN-STATUS PIC X(01); 88-levels: 'P' Pending, 'D' Done, 'F' Failed, 'R' Reversed. */
    @Column(name = "STATUS", columnDefinition = "CHAR(1)", length = 1, nullable = false)
    private String status;

    /** TRN-PROCESS-DATE PIC X(26) (DB2 timestamp format). */
    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;

    /** TRN-PROCESS-USER PIC X(08). */
    @Column(name = "PROCESS_USER", length = 8)
    private String processUser;

    public PortfolioTransactionId getId() {
        return id;
    }

    public void setId(PortfolioTransactionId id) {
        this.id = id;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDateTime processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
