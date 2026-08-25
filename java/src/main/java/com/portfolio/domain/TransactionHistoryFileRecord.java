package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Relational model of the VSAM KSDS TRANHIST file
 * ({@code src/database/vsam/vsam-definitions.txt}), the input file read
 * sequentially by HISTLD00 (SELECT TRANSACTION-HISTORY ... RECORD KEY IS TH-KEY).
 *
 * <p>VSAM→table convention: the KSDS is modeled as a table whose composite
 * primary key is the COBOL RECORD KEY — here (TRANS_DATE, TRANS_TIME,
 * PORTFOLIO_ID, SEQUENCE_NO) per the TRANHIST key structure. The data fields
 * carry the TH-* fields that HISTLD00 maps into POSHIST columns.
 */
@Entity
@Table(name = "VSAM_TRANHIST")
public class TransactionHistoryFileRecord {

    @EmbeddedId
    private Key key;

    /** TH-ACCOUNT-NO PIC X(8). */
    @Column(name = "ACCOUNT_NO", length = 8, nullable = false)
    private String accountNo;

    /** TH-TRANS-TYPE PIC X(2) — BU/SL/TR/FE. */
    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String transType;

    /** TH-SECURITY-ID PIC X(12). */
    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    /** TH-QUANTITY PIC S9(12)V9(3) COMP-3. */
    @Column(name = "QUANTITY", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    /** TH-PRICE PIC S9(12)V9(3) COMP-3. */
    @Column(name = "PRICE", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    /** TH-AMOUNT PIC S9(13)V9(2) COMP-3. */
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** TH-FEES PIC S9(13)V9(2) COMP-3. */
    @Column(name = "FEES", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees = BigDecimal.ZERO;

    /** TH-TOTAL-AMOUNT PIC S9(13)V9(2) COMP-3. */
    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** TH-COST-BASIS PIC S9(13)V9(2) COMP-3. */
    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** TH-GAIN-LOSS PIC S9(13)V9(2) COMP-3. */
    @Column(name = "GAIN_LOSS", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    /**
     * Composite primary key = VSAM TRANHIST record key:
     * Transaction Date (8) + Transaction Time (6) + Portfolio ID (8) + Sequence No (6).
     */
    @Embeddable
    public static class Key implements Serializable {

        /** Transaction date component of TH-KEY (YYYYMMDD in VSAM). */
        @Column(name = "TRANS_DATE", nullable = false)
        private LocalDate transDate;

        /** Transaction time component of TH-KEY (HHMMSS in VSAM). */
        @Column(name = "TRANS_TIME", nullable = false)
        private LocalTime transTime;

        /** Portfolio ID component of TH-KEY PIC X(8); widened to X(10) to match POSHIST. */
        @Column(name = "PORTFOLIO_ID", length = 10, nullable = false)
        private String portfolioId;

        /** Sequence number component of TH-KEY PIC X(6). */
        @Column(name = "SEQUENCE_NO", length = 6, nullable = false)
        private String sequenceNo;

        public Key() {}

        public Key(LocalDate transDate, LocalTime transTime, String portfolioId, String sequenceNo) {
            this.transDate = transDate;
            this.transTime = transTime;
            this.portfolioId = portfolioId;
            this.sequenceNo = sequenceNo;
        }

        public LocalDate getTransDate() { return transDate; }
        public void setTransDate(LocalDate transDate) { this.transDate = transDate; }
        public LocalTime getTransTime() { return transTime; }
        public void setTransTime(LocalTime transTime) { this.transTime = transTime; }
        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
        public String getSequenceNo() { return sequenceNo; }
        public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(transDate, key.transDate)
                    && Objects.equals(transTime, key.transTime)
                    && Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(sequenceNo, key.sequenceNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transDate, transTime, portfolioId, sequenceNo);
        }
    }

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getTransType() { return transType; }
    public void setTransType(String transType) { this.transType = transType; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public BigDecimal getGainLoss() { return gainLoss; }
    public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
}
