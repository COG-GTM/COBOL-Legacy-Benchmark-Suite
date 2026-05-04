package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Investment Position entity - migrated from COBOL POSREC.cpy.
 *
 * COBOL field mappings:
 * - POS-QUANTITY (PIC S9(11)V9(4) COMP-3) -> quantity (BigDecimal, precision=15, scale=4)
 * - POS-COST-BASIS (PIC S9(13)V9(2) COMP-3) -> costBasis (BigDecimal, precision=15, scale=2)
 * - POS-MARKET-VALUE (PIC S9(13)V9(2) COMP-3) -> marketValue (BigDecimal, precision=15, scale=2)
 */
@Entity
@Table(name = "investment_positions")
@IdClass(PositionId.class)
public class Position {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Id
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    @Column(name = "filler", length = 50)
    private String filler;

    public Position() {
        this.quantity = BigDecimal.ZERO;
        this.costBasis = BigDecimal.ZERO;
        this.marketValue = BigDecimal.ZERO;
        this.currencyCode = "USD";
        this.status = "A";
        this.lastMaintDate = LocalDateTime.now();
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public LocalDate getPositionDate() { return positionDate; }
    public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
    public String getFiller() { return filler; }
    public void setFiller(String filler) { this.filler = filler; }

    public boolean isActive() { return "A".equals(status); }
    public boolean isClosed() { return "C".equals(status); }
    public boolean isPending() { return "P".equals(status); }
}
