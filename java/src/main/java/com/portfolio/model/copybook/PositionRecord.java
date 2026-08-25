package com.portfolio.model.copybook;

import java.math.BigDecimal;

/**
 * Migrated from copybook {@code src/copybook/common/POSREC.cpy} (01 POSITION-RECORD).
 *
 * <p>Key = POS-KEY (portfolio id + date + investment id).
 */
public class PositionRecord {

    /** POS-PORTFOLIO-ID PIC X(08). */
    private String portfolioId;

    /** POS-DATE PIC X(08) — YYYYMMDD. */
    private String date;

    /** POS-INVESTMENT-ID PIC X(10). */
    private String investmentId;

    /** POS-QUANTITY PIC S9(11)V9(4) COMP-3. */
    private BigDecimal quantity;

    /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3. */
    private BigDecimal costBasis;

    /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3. */
    private BigDecimal marketValue;

    /** POS-CURRENCY PIC X(03). */
    private String currency;

    /** POS-STATUS PIC X(01) — A=Active, C=Closed, P=Pending (level-88s). */
    private String status;

    /** POS-LAST-MAINT-DATE PIC X(26). */
    private String lastMaintDate;

    /** POS-LAST-MAINT-USER PIC X(08). */
    private String lastMaintUser;

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(String lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
}
