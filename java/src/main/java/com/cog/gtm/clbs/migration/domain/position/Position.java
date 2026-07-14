package com.cog.gtm.clbs.migration.domain.position;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@IdClass(PositionId.class)
@Table(name = "positions")
public class Position {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "position_date", length = 8, nullable = false)
    private String positionDate;

    @Id
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", length = 1)
    private String status;

    @Column(name = "last_maint_date", length = 26)
    private String lastMaintDate;

    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(String positionDate) {
        this.positionDate = positionDate;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
