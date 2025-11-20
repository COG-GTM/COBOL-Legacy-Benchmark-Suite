package com.portfolio.batch.trnval.model;

import java.math.BigDecimal;

/**
 * Position Record - Java representation of COBOL POSREC copybook
 * 
 * Corresponds to COBOL structure:
 * 01  POSITION-RECORD.
 *     05  POS-KEY.
 *         10  POS-PORTFOLIO-ID   PIC X(08).
 *         10  POS-DATE           PIC X(08).
 *         10  POS-INVESTMENT-ID  PIC X(10).
 *     05  POS-DATA.
 *         10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *         10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
 *         10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
 *         10  POS-CURRENCY       PIC X(03).
 *         10  POS-STATUS         PIC X(01).
 *     05  POS-AUDIT.
 *         10  POS-LAST-MAINT-DATE   PIC X(26).
 *         10  POS-LAST-MAINT-USER   PIC X(08).
 */
public class PositionRecord {
    
    private String portfolioId;
    private String date;
    private String investmentId;
    
    private BigDecimal quantity;
    private BigDecimal costBasis;
    private BigDecimal marketValue;
    private String currency;
    private PositionStatus status;
    
    private String lastMaintDate;
    private String lastMaintUser;
    
    public enum PositionStatus {
        ACTIVE("A"),
        CLOSED("C"),
        PENDING("P");
        
        private final String code;
        
        PositionStatus(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
        
        public static PositionStatus fromCode(String code) {
            if (code == null) {
                return null;
            }
            for (PositionStatus status : values()) {
                if (status.code.equals(code.trim())) {
                    return status;
                }
            }
            return null;
        }
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
        this.quantity = quantity;
    }
    
    public BigDecimal getCostBasis() {
        return costBasis;
    }
    
    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }
    
    public BigDecimal getMarketValue() {
        return marketValue;
    }
    
    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public PositionStatus getStatus() {
        return status;
    }
    
    public void setStatus(PositionStatus status) {
        this.status = status;
    }
    
    public String getLastMaintDate() {
        return lastMaintDate;
    }
    
    public void setLastMaintDate(String lastMaintDate) {
        this.lastMaintDate = lastMaintDate;
    }
    
    public String getLastMaintUser() {
        return lastMaintUser;
    }
    
    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }
    
    public String getPositionKey() {
        return String.format("%s-%s-%s", portfolioId, date, investmentId);
    }
    
    @Override
    public String toString() {
        return String.format("PositionRecord[key=%s, quantity=%s, status=%s]",
                getPositionKey(), quantity, status);
    }
}
