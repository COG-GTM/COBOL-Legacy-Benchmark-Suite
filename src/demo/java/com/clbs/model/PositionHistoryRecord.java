package com.clbs.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Java equivalent of COBOL POSHIST-RECORD from DBTBLS.cpy
 * 
 * COBOL Original:
 * <pre>
 *  01  POSHIST-RECORD.
 *      05  PH-ACCOUNT-NO        PIC X(8).
 *      05  PH-PORTFOLIO-ID      PIC X(10).
 *      05  PH-TRANS-DATE        PIC X(10).
 *      05  PH-TRANS-TIME        PIC X(8).
 *      05  PH-TRANS-TYPE        PIC X(2).
 *      05  PH-SECURITY-ID       PIC X(12).
 *      05  PH-QUANTITY          PIC S9(12)V9(3) COMP-3.
 *      05  PH-PRICE             PIC S9(12)V9(3) COMP-3.
 *      05  PH-AMOUNT            PIC S9(13)V9(2) COMP-3.
 *      05  PH-FEES              PIC S9(13)V9(2) COMP-3.
 *      05  PH-TOTAL-AMOUNT      PIC S9(13)V9(2) COMP-3.
 *      05  PH-COST-BASIS        PIC S9(13)V9(2) COMP-3.
 *      05  PH-GAIN-LOSS         PIC S9(13)V9(2) COMP-3.
 *      05  PH-PROCESS-DATE      PIC X(10).
 *      05  PH-PROCESS-TIME      PIC X(8).
 *      05  PH-PROGRAM-ID        PIC X(8).
 *      05  PH-USER-ID           PIC X(8).
 *      05  PH-AUDIT-TIMESTAMP   PIC X(26).
 * </pre>
 * 
 * Migration Notes:
 * - COMP-3 (packed decimal) fields converted to BigDecimal for precision
 * - PIC X fields converted to String with length validation
 * - Date/time fields converted to Java 8 date/time types
 */
public class PositionHistoryRecord {
    
    private String accountNo;
    private String portfolioId;
    private LocalDate transDate;
    private LocalTime transTime;
    private String transType;
    private String securityId;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fees;
    private BigDecimal totalAmount;
    private BigDecimal costBasis;
    private BigDecimal gainLoss;
    private LocalDate processDate;
    private LocalTime processTime;
    private String programId;
    private String userId;
    private LocalDateTime auditTimestamp;

    public PositionHistoryRecord() {
        this.quantity = BigDecimal.ZERO;
        this.price = BigDecimal.ZERO;
        this.amount = BigDecimal.ZERO;
        this.fees = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.costBasis = BigDecimal.ZERO;
        this.gainLoss = BigDecimal.ZERO;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = truncateOrPad(accountNo, 8);
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = truncateOrPad(portfolioId, 10);
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

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = truncateOrPad(transType, 2);
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = truncateOrPad(securityId, 12);
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

    public BigDecimal getFees() {
        return fees;
    }

    public void setFees(BigDecimal fees) {
        this.fees = fees;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getGainLoss() {
        return gainLoss;
    }

    public void setGainLoss(BigDecimal gainLoss) {
        this.gainLoss = gainLoss;
    }

    public LocalDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDate processDate) {
        this.processDate = processDate;
    }

    public LocalTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalTime processTime) {
        this.processTime = processTime;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = truncateOrPad(programId, 8);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = truncateOrPad(userId, 8);
    }

    public LocalDateTime getAuditTimestamp() {
        return auditTimestamp;
    }

    public void setAuditTimestamp(LocalDateTime auditTimestamp) {
        this.auditTimestamp = auditTimestamp;
    }

    private String truncateOrPad(String value, int length) {
        if (value == null) {
            return String.format("%-" + length + "s", "");
        }
        if (value.length() > length) {
            return value.substring(0, length);
        }
        return String.format("%-" + length + "s", value);
    }

    @Override
    public String toString() {
        return "PositionHistoryRecord{" +
                "accountNo='" + accountNo + '\'' +
                ", portfolioId='" + portfolioId + '\'' +
                ", transDate=" + transDate +
                ", transType='" + transType + '\'' +
                ", securityId='" + securityId + '\'' +
                ", quantity=" + quantity +
                ", amount=" + amount +
                '}';
    }
}
