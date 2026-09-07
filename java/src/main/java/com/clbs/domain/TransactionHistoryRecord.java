package com.clbs.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TRANHIST record as HISTLD00 actually uses it.
 *
 * <p>HISTLD00's FD copies HISTREC (HIST-* before/after images) but its 2200-LOAD-TO-DB2 moves
 * TH-ACCOUNT-NO, TH-QUANTITY, TH-FEES, TH-TOTAL-AMOUNT, TH-COST-BASIS and TH-GAIN-LOSS — fields no
 * copybook in the repository defines. This class carries the TH-* layout implied by that paragraph
 * and by the POSHIST columns it feeds; precisions follow DBTBLS.cpy POSHIST-RECORD.
 */
public class TransactionHistoryRecord {

    private String accountNo = "";
    private String portfolioId = "";
    private String transDate = "";
    private String transTime = "";
    private String transType = "";
    private String securityId = "";
    private BigDecimal quantity = BigDecimal.ZERO.setScale(3);
    private BigDecimal price = BigDecimal.ZERO.setScale(3);
    private BigDecimal amount = BigDecimal.ZERO.setScale(2);
    private BigDecimal fees = BigDecimal.ZERO.setScale(2);
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);
    private BigDecimal costBasis = BigDecimal.ZERO.setScale(2);
    private BigDecimal gainLoss = BigDecimal.ZERO.setScale(2);

    /** TH-KEY, following the HISTREC key order the FD declares. */
    public String key() {
        return pad(portfolioId, 8) + pad(transDate, 10) + pad(transTime, 8) + pad(securityId, 12);
    }

    private static String pad(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO.setScale(scale) : value.setScale(scale, RoundingMode.HALF_UP);
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getTransTime() {
        return transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = scale(quantity, 3);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = scale(price, 3);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = scale(amount, 2);
    }

    public BigDecimal getFees() {
        return fees;
    }

    public void setFees(BigDecimal fees) {
        this.fees = scale(fees, 2);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = scale(totalAmount, 2);
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = scale(costBasis, 2);
    }

    public BigDecimal getGainLoss() {
        return gainLoss;
    }

    public void setGainLoss(BigDecimal gainLoss) {
        this.gainLoss = scale(gainLoss, 2);
    }
}
