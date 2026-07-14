package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "investment_positions")
@IdClass(InvestmentPosition.InvestmentPositionId.class)
public class InvestmentPosition {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Id
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    public InvestmentPosition() {
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

    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }

    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }

    public static class InvestmentPositionId implements Serializable {
        private String portfolioId;
        private String investmentId;
        private LocalDate positionDate;

        public InvestmentPositionId() {
        }

        public InvestmentPositionId(String portfolioId, String investmentId, LocalDate positionDate) {
            this.portfolioId = portfolioId;
            this.investmentId = investmentId;
            this.positionDate = positionDate;
        }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

        public String getInvestmentId() { return investmentId; }
        public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

        public LocalDate getPositionDate() { return positionDate; }
        public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InvestmentPositionId)) return false;
            InvestmentPositionId that = (InvestmentPositionId) o;
            return Objects.equals(portfolioId, that.portfolioId)
                    && Objects.equals(investmentId, that.investmentId)
                    && Objects.equals(positionDate, that.positionDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, investmentId, positionDate);
        }
    }
}
