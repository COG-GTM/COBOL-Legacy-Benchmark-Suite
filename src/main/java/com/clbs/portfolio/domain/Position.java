package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Investment position (copybook POSREC, DB2 INVESTMENT_POSITIONS). */
@Entity
@Table(name = "POSITION",
        uniqueConstraints = @UniqueConstraint(columnNames = {"PORTFOLIO_ID", "INVESTMENT_ID", "POSITION_DATE"}))
public class Position {

    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO.setScale(4);

    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis = BigDecimal.ZERO.setScale(2);

    @Column(name = "MARKET_VALUE", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue = BigDecimal.ZERO.setScale(2);

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    /** A=Active, C=Closed, P=Pending. */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status = STATUS_ACTIVE;

    @Column(name = "LAST_MAINT_TS")
    private LocalDateTime lastMaintTimestamp;

    @Column(name = "LAST_MAINT_USER", length = 8)
    private String lastMaintUser;

    protected Position() {
    }

    public Position(String portfolioId, LocalDate positionDate, String investmentId, String currency) {
        this.portfolioId = portfolioId;
        this.positionDate = positionDate;
        this.investmentId = investmentId;
        this.currency = currency;
    }

    public Long getId() { return id; }
    public String getPortfolioId() { return portfolioId; }
    public LocalDate getPositionDate() { return positionDate; }
    public String getInvestmentId() { return investmentId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastMaintTimestamp() { return lastMaintTimestamp; }
    public void setLastMaintTimestamp(LocalDateTime lastMaintTimestamp) { this.lastMaintTimestamp = lastMaintTimestamp; }
    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
}
