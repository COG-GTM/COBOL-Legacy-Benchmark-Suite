package com.investment.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Record - Java equivalent of TRNREC.cpy
 * Maps the COBOL TRANSACTION-RECORD copybook structure.
 */
public class TransactionRecord {

    /** Key fields */
    private String transactionDate;    // TRN-DATE: PIC X(08) YYYYMMDD
    private String transactionTime;    // TRN-TIME: PIC X(06) HHMMSS
    private String portfolioId;        // TRN-PORTFOLIO-ID: PIC X(08)
    private String sequenceNumber;     // TRN-SEQUENCE-NO: PIC X(06)

    /** Transaction data */
    private String investmentId;       // TRN-INVESTMENT-ID: PIC X(10)
    private TransactionType type;      // TRN-TYPE: PIC X(02)
    private BigDecimal quantity;        // TRN-QUANTITY: PIC S9(11)V9(4) COMP-3
    private BigDecimal price;           // TRN-PRICE: PIC S9(11)V9(4) COMP-3
    private BigDecimal amount;          // TRN-AMOUNT: PIC S9(13)V9(2) COMP-3
    private String currency;            // TRN-CURRENCY: PIC X(03)
    private TransactionStatus status;   // TRN-STATUS: PIC X(01)

    /** Audit fields */
    private LocalDateTime processDate;  // TRN-PROCESS-DATE: PIC X(26)
    private String processUser;         // TRN-PROCESS-USER: PIC X(08)

    public enum TransactionType {
        BUY("BU"),
        SELL("SL"),
        TRANSFER("TR"),
        FEE("FE");

        private final String code;

        TransactionType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static TransactionType fromCode(String code) {
            for (TransactionType t : values()) {
                if (t.code.equals(code)) return t;
            }
            throw new IllegalArgumentException("Invalid transaction type: " + code);
        }
    }

    public enum TransactionStatus {
        PENDING('P'),
        DONE('D'),
        FAILED('F'),
        REVERSED('R');

        private final char code;

        TransactionStatus(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static TransactionStatus fromCode(char code) {
            for (TransactionStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Invalid transaction status: " + code);
        }
    }

    // --- Getters and Setters ---

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(String sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getProcessDate() { return processDate; }
    public void setProcessDate(LocalDateTime processDate) { this.processDate = processDate; }

    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }

    /**
     * Builds the composite key (date + time + portfolioId + sequence)
     * corresponding to TRN-KEY in the COBOL copybook.
     */
    public String getCompositeKey() {
        return transactionDate + transactionTime + portfolioId + sequenceNumber;
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "portfolioId='" + portfolioId + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
