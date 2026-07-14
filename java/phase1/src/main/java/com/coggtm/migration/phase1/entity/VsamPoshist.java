package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "vsam_poshist")
@IdClass(VsamPoshist.VsamPoshistId.class)
public class VsamPoshist {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @Id
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

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

    public VsamPoshist() {
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public LocalDate getPositionDate() { return positionDate; }
    public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }

    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

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

    public static class VsamPoshistId implements Serializable {
        private String portfolioId;
        private LocalDate positionDate;
        private String investmentId;

        public VsamPoshistId() {
        }

        public VsamPoshistId(String portfolioId, LocalDate positionDate, String investmentId) {
            this.portfolioId = portfolioId;
            this.positionDate = positionDate;
            this.investmentId = investmentId;
        }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

        public LocalDate getPositionDate() { return positionDate; }
        public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }

        public String getInvestmentId() { return investmentId; }
        public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VsamPoshistId)) return false;
            VsamPoshistId that = (VsamPoshistId) o;
            return Objects.equals(portfolioId, that.portfolioId)
                    && Objects.equals(positionDate, that.positionDate)
                    && Objects.equals(investmentId, that.investmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, positionDate, investmentId);
        }
    }
}
