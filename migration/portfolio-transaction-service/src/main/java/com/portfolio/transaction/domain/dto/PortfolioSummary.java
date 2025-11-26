package com.portfolio.transaction.domain.dto;

import com.portfolio.transaction.domain.entity.Portfolio;
import java.math.BigDecimal;

public class PortfolioSummary {

    private String portfolioId;
    private String accountNo;
    private BigDecimal totalUnits;
    private BigDecimal totalCost;

    public PortfolioSummary() {
    }

    public PortfolioSummary(Portfolio portfolio) {
        this.portfolioId = portfolio.getPortfolioId();
        this.accountNo = portfolio.getAccountNo();
        this.totalUnits = portfolio.getTotalUnits();
        this.totalCost = portfolio.getTotalCost();
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
}
