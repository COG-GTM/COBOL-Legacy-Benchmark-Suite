package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Investment Position entity - migrated from VSAM Position Master (POSMSTRE)
 * and DB2 INVESTMENT_POSITIONS table.
 * Source: POSREC copybook, src/database/db2/db2-definitions.sql
 *
 * Composite key: portfolio_id + investment_id + position_date
 * Status: 'A'=Active, 'C'=Closed, 'P'=Pending
 */
@Entity
@Table(name = "investment_positions")
@IdClass(InvestmentPositionId.class)
public class InvestmentPosition {

    @Id
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Id
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Id
    @Column(name = "position_date")
    private LocalDate positionDate;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    @NotNull
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal costBasis = BigDecimal.ZERO;

    @Column(name = "market_value", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal marketValue = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3, nullable = false)
    @NotBlank
    private String currencyCode = "USD";

    @Column(name = "status", length = 1, nullable = false)
    @NotBlank
    private String status = "A";

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate = LocalDateTime.now();

    @Column(name = "last_maint_user", length = 8, nullable = false)
    @NotBlank
    private String lastMaintUser;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", insertable = false, updatable = false)
    private PortfolioMaster portfolio;

    public InvestmentPosition() {
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastMaintDate() {
        return lastMaintDate;
    }

    public void setLastMaintDate(LocalDateTime lastMaintDate) {
        this.lastMaintDate = lastMaintDate;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }

    public PortfolioMaster getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(PortfolioMaster portfolio) {
        this.portfolio = portfolio;
    }

    public boolean isActive() {
        return "A".equals(this.status);
    }

    /**
     * Compute unrealized gain/loss (POS-MARKET-VALUE - POS-COST-BASIS).
     */
    public BigDecimal getUnrealizedGainLoss() {
        return marketValue.subtract(costBasis);
    }
}
