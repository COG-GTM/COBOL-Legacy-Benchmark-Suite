package com.investment.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position History DB2 Record - Java equivalent of DBTBLS.cpy (POSHIST-RECORD).
 * Maps the DB2 POSHIST table host variable structure.
 */
public class PositionHistoryDbRecord {

    private String accountNumber;       // PH-ACCOUNT-NO: PIC X(8)
    private String portfolioId;         // PH-PORTFOLIO-ID: PIC X(10)
    private String transactionDate;     // PH-TRANS-DATE: PIC X(10)
    private String transactionTime;     // PH-TRANS-TIME: PIC X(8)
    private String transactionType;     // PH-TRANS-TYPE: PIC X(2)
    private String securityId;          // PH-SECURITY-ID: PIC X(12)
    private BigDecimal quantity;         // PH-QUANTITY: PIC S9(12)V9(3) COMP-3
    private BigDecimal price;            // PH-PRICE: PIC S9(12)V9(3) COMP-3
    private BigDecimal amount;           // PH-AMOUNT: PIC S9(13)V9(2) COMP-3
    private BigDecimal fees;             // PH-FEES: PIC S9(13)V9(2) COMP-3
    private BigDecimal totalAmount;      // PH-TOTAL-AMOUNT: PIC S9(13)V9(2) COMP-3
    private BigDecimal costBasis;        // PH-COST-BASIS: PIC S9(13)V9(2) COMP-3
    private BigDecimal gainLoss;         // PH-GAIN-LOSS: PIC S9(13)V9(2) COMP-3
    private String processDate;          // PH-PROCESS-DATE: PIC X(10)
    private String processTime;          // PH-PROCESS-TIME: PIC X(8)
    private String programId;            // PH-PROGRAM-ID: PIC X(8)
    private String userId;               // PH-USER-ID: PIC X(8)
    private LocalDateTime auditTimestamp; // PH-AUDIT-TIMESTAMP: PIC X(26)

    // --- Getters and Setters ---

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

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

    public String getProcessDate() { return processDate; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }

    public String getProcessTime() { return processTime; }
    public void setProcessTime(String processTime) { this.processTime = processTime; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(LocalDateTime auditTimestamp) { this.auditTimestamp = auditTimestamp; }

    @Override
    public String toString() {
        return "PositionHistoryDbRecord{" +
                "accountNumber='" + accountNumber + '\'' +
                ", portfolioId='" + portfolioId + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                '}';
    }
}
