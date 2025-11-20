package com.portfolio.batch.trnval.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Transaction Record - Java representation of COBOL TRNREC copybook
 * 
 * Corresponds to COBOL structure:
 * 01  TRANSACTION-RECORD.
 *     05  TRN-KEY.
 *         10  TRN-DATE           PIC X(08).
 *         10  TRN-TIME           PIC X(06).
 *         10  TRN-PORTFOLIO-ID   PIC X(08).
 *         10  TRN-SEQUENCE-NO    PIC X(06).
 *     05  TRN-DATA.
 *         10  TRN-INVESTMENT-ID  PIC X(10).
 *         10  TRN-TYPE           PIC X(02).
 *         10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *         10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
 *         10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
 *         10  TRN-CURRENCY       PIC X(03).
 *         10  TRN-STATUS         PIC X(01).
 *     05  TRN-AUDIT.
 *         10  TRN-PROCESS-DATE   PIC X(26).
 *         10  TRN-PROCESS-USER   PIC X(08).
 */
public class TransactionRecord {
    
    private String date;
    private String time;
    private String portfolioId;
    private String sequenceNo;
    
    private String investmentId;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    
    private String processDate;
    private String processUser;
    
    private int lineNumber;
    
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
            if (code == null) {
                return null;
            }
            for (TransactionType type : values()) {
                if (type.code.equals(code.trim())) {
                    return type;
                }
            }
            return null;
        }
    }
    
    public enum TransactionStatus {
        PENDING("P"),
        DONE("D"),
        FAILED("F"),
        REVERSED("R");
        
        private final String code;
        
        TransactionStatus(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
        
        public static TransactionStatus fromCode(String code) {
            if (code == null) {
                return null;
            }
            for (TransactionStatus status : values()) {
                if (status.code.equals(code.trim())) {
                    return status;
                }
            }
            return null;
        }
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
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
    
    public String getProcessDate() {
        return processDate;
    }
    
    public void setProcessDate(String processDate) {
        this.processDate = processDate;
    }
    
    public String getProcessUser() {
        return processUser;
    }
    
    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public String getTransactionKey() {
        return String.format("%s-%s-%s-%s", date, time, portfolioId, sequenceNo);
    }
    
    @Override
    public String toString() {
        return String.format("TransactionRecord[key=%s, type=%s, investmentId=%s, amount=%s, status=%s]",
                getTransactionKey(), type, investmentId, amount, status);
    }
}
