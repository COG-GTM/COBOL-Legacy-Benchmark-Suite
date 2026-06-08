package com.portfolio.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps COBOL POSREC.cpy POSITION-RECORD.
 */
@Entity
@Table(name = "position")
@IdClass(PositionId.class)
public class Position {

    @Id
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Id
    @Column(name = "position_date", length = 8)
    private String positionDate;

    @Id
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private PositionStatus status;

    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    protected Position() { /* JPA */ }

    public Position(String portfolioId, String positionDate, String investmentId) {
        this.portfolioId = portfolioId;
        this.positionDate = positionDate;
        this.investmentId = investmentId;
        this.quantity = BigDecimal.ZERO;
        this.costBasis = BigDecimal.ZERO;
        this.marketValue = BigDecimal.ZERO;
        this.status = PositionStatus.ACTIVE;
    }

    public String getPortfolioId() { return portfolioId; }
    public String getPositionDate() { return positionDate; }
    public String getInvestmentId() { return investmentId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getCostBasis() { return costBasis; }
    public BigDecimal getMarketValue() { return marketValue; }
    public String getCurrency() { return currency; }
    public PositionStatus getStatus() { return status; }
    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public String getLastMaintUser() { return lastMaintUser; }

    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(PositionStatus status) { this.status = status; }

    public void markMaintenance(String userId) {
        this.lastMaintDate = LocalDateTime.now();
        this.lastMaintUser = userId;
    }
}
