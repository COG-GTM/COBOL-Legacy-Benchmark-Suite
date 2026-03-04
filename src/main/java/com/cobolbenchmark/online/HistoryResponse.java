package com.cobolbenchmark.online;

import java.math.BigDecimal;
import java.util.List;

/**
 * History Response DTO - replaces HISMAP BMS map output.
 * Maps to EXEC CICS SEND MAP('HISMAP') output fields.
 */
public class HistoryResponse {

    private String portfolioId;
    private List<HistoryDetail> transactions;
    private int totalRecords;
    private String message;

    public HistoryResponse() {
    }

    public static class HistoryDetail {
        private String transactionDate;
        private String transactionTime;
        private String transactionType;
        private String securityId;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal amount;
        private BigDecimal fees;
        private BigDecimal totalAmount;

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
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public List<HistoryDetail> getTransactions() { return transactions; }
    public void setTransactions(List<HistoryDetail> transactions) { this.transactions = transactions; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
