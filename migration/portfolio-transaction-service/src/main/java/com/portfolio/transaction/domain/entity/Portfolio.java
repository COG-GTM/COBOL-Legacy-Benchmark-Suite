package com.portfolio.transaction.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 10)
    private String portfolioId;

    @Column(name = "account_no", length = 15)
    private String accountNo;

    @Column(name = "total_units", precision = 15, scale = 4)
    private BigDecimal totalUnits;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Version
    private Long version;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public Portfolio() {
        this.totalUnits = BigDecimal.ZERO;
        this.totalCost = BigDecimal.ZERO;
    }

    public Portfolio(String portfolioId, String accountNo) {
        this();
        this.portfolioId = portfolioId;
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public BigDecimal getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(BigDecimal totalUnits) {
        this.totalUnits = totalUnits;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Portfolio clone() {
        Portfolio copy = new Portfolio();
        copy.portfolioId = this.portfolioId;
        copy.accountNo = this.accountNo;
        copy.totalUnits = this.totalUnits;
        copy.totalCost = this.totalCost;
        copy.version = this.version;
        copy.lastUpdated = this.lastUpdated;
        return copy;
    }
}
