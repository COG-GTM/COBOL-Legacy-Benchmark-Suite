package com.clbs.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** TRNREC.cpy TRANSACTION-RECORD. */
public class TransactionRecord {

    private String date = "";
    private String time = "";
    private String portfolioId = "";
    private String sequenceNo = "";
    private String accountNo = "";
    private String investmentId = "";
    private String type = "";
    private BigDecimal quantity = BigDecimal.ZERO.setScale(4);
    private BigDecimal price = BigDecimal.ZERO.setScale(4);
    private BigDecimal amount = BigDecimal.ZERO.setScale(2);
    private String currency = "USD";
    private char status = 'P';
    private String processDate = "";
    private String processUser = "";

    /** TRN-STATUS 88-levels. */
    public static final char STATUS_PENDING = 'P';
    public static final char STATUS_DONE = 'D';
    public static final char STATUS_FAILED = 'F';
    public static final char STATUS_REVERSED = 'R';

    /** TRN-KEY = TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO. */
    public String key() {
        return pad(date, 8) + pad(time, 6) + pad(portfolioId, 8) + pad(sequenceNo, 6);
    }

    private static String pad(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = scale(quantity, 4);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = scale(price, 4);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = scale(amount, 2);
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
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

    public TransactionRecord copy() {
        TransactionRecord c = new TransactionRecord();
        c.date = date;
        c.time = time;
        c.portfolioId = portfolioId;
        c.sequenceNo = sequenceNo;
        c.accountNo = accountNo;
        c.investmentId = investmentId;
        c.type = type;
        c.quantity = quantity;
        c.price = price;
        c.amount = amount;
        c.currency = currency;
        c.status = status;
        c.processDate = processDate;
        c.processUser = processUser;
        return c;
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO.setScale(scale) : value.setScale(scale, RoundingMode.HALF_UP);
    }
}
