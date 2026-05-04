package com.portfolio.dto;

import java.math.BigDecimal;

public class PortfolioPositionResponse {

    private String accountNo;
    private String fundId;
    private String fundName;
    private BigDecimal units;
    private BigDecimal costBasis;
    private BigDecimal marketValue;

    public PortfolioPositionResponse() {
    }

    public PortfolioPositionResponse(String accountNo, String fundId, String fundName,
                                     BigDecimal units, BigDecimal costBasis, BigDecimal marketValue) {
        this.accountNo = accountNo;
        this.fundId = fundId;
        this.fundName = fundName;
        this.units = units;
        this.costBasis = costBasis;
        this.marketValue = marketValue;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getFundId() {
        return fundId;
    }

    public void setFundId(String fundId) {
        this.fundId = fundId;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
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
}
