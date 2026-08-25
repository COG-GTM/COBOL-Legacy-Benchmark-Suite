package com.portfolio.model.copybook;

import java.math.BigDecimal;

/**
 * Migrated from copybook {@code src/copybook/common/TRNREC.cpy} (01 TRANSACTION-RECORD).
 *
 * <p>Key = TRN-KEY (date + time + portfolio id + sequence no).
 * Packed-decimal (COMP-3) financial fields are mapped to {@link BigDecimal}.
 */
public class TransactionRecord {

    /** TRN-DATE PIC X(08) — YYYYMMDD. */
    private String date;

    /** TRN-TIME PIC X(06) — HHMMSS. */
    private String time;

    /** TRN-PORTFOLIO-ID PIC X(08). */
    private String portfolioId;

    /** TRN-SEQUENCE-NO PIC X(06). */
    private String sequenceNo;

    /** TRN-INVESTMENT-ID PIC X(10). */
    private String investmentId;

    /** TRN-TYPE PIC X(02) — BU=Buy, SL=Sell, TR=Transfer, FE=Fee (level-88s). */
    private String type;

    /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3. */
    private BigDecimal quantity;

    /** TRN-PRICE PIC S9(11)V9(4) COMP-3. */
    private BigDecimal price;

    /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3. */
    private BigDecimal amount;

    /** TRN-CURRENCY PIC X(03). */
    private String currency;

    /** TRN-STATUS PIC X(01) — P=Pending, D=Done, F=Failed, R=Reversed (level-88s). */
    private String status;

    /** TRN-PROCESS-DATE PIC X(26). */
    private String processDate;

    /** TRN-PROCESS-USER PIC X(08). */
    private String processUser;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessDate() { return processDate; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }
    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }
}
