package com.ipms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** INVESTMENT_POSITIONS table from {@code src/database/db2/db2-definitions.sql}. */
@Entity
@Table(name = "INVESTMENT_POSITIONS")
@IdClass(InvestmentPosition.Key.class)
public class InvestmentPosition {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Id
    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "MARKET_VALUE", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public static class Key implements Serializable {
        private String portfolioId;
        private String investmentId;
        private LocalDate positionDate;

        public Key() {
        }

        public Key(String portfolioId, String investmentId, LocalDate positionDate) {
            this.portfolioId = portfolioId;
            this.investmentId = investmentId;
            this.positionDate = positionDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(investmentId, key.investmentId)
                    && Objects.equals(positionDate, key.positionDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, investmentId, positionDate);
        }
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
}
