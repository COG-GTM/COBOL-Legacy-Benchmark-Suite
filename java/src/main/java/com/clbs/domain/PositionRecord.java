package com.clbs.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** POSREC.cpy POSITION-RECORD. */
public class PositionRecord {

    private String portfolioId = "";
    private String date = "";
    private String investmentId = "";
    private BigDecimal quantity = BigDecimal.ZERO.setScale(4);
    private BigDecimal costBasis = BigDecimal.ZERO.setScale(2);
    private BigDecimal marketValue = BigDecimal.ZERO.setScale(2);
    private BigDecimal previousValue = BigDecimal.ZERO.setScale(2);
    private String currency = "USD";
    private char status = 'A';
    private String description = "";

    /** POS-KEY = POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID. */
    public String key() {
        return pad(portfolioId, 8) + pad(date, 8) + pad(investmentId, 10);
    }

    private static String pad(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = scale(quantity, 4);
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = scale(costBasis, 2);
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = scale(marketValue, 2);
    }

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = scale(previousValue, 2);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO.setScale(scale) : value.setScale(scale, RoundingMode.HALF_UP);
    }
}
