package com.portfolio.dto;

import com.portfolio.entity.PortfolioMaster;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Portfolio response DTO - replaces BMS POSMAP screen output.
 * Source: src/maps/INQSET.bms POSMAP definition
 */
public class PortfolioResponse {

    private String portfolioId;
    private String portfolioName;
    private String accountType;
    private String branchId;
    private String clientId;
    private String currencyCode;
    private String riskLevel;
    private String status;
    private LocalDate openDate;
    private LocalDate closeDate;
    private LocalDateTime lastMaintDate;
    private BigDecimal totalMarketValue;
    private BigDecimal totalCostBasis;
    private BigDecimal totalGainLoss;
    private List<PositionResponse> positions;

    public PortfolioResponse() {
    }

    public static PortfolioResponse fromEntity(PortfolioMaster entity) {
        PortfolioResponse response = new PortfolioResponse();
        response.setPortfolioId(entity.getPortfolioId());
        response.setPortfolioName(entity.getPortfolioName());
        response.setAccountType(entity.getAccountType());
        response.setBranchId(entity.getBranchId());
        response.setClientId(entity.getClientId());
        response.setCurrencyCode(entity.getCurrencyCode());
        response.setRiskLevel(entity.getRiskLevel());
        response.setStatus(entity.getStatus());
        response.setOpenDate(entity.getOpenDate());
        response.setCloseDate(entity.getCloseDate());
        response.setLastMaintDate(entity.getLastMaintDate());
        return response;
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public BigDecimal getTotalMarketValue() { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }
    public BigDecimal getTotalCostBasis() { return totalCostBasis; }
    public void setTotalCostBasis(BigDecimal totalCostBasis) { this.totalCostBasis = totalCostBasis; }
    public BigDecimal getTotalGainLoss() { return totalGainLoss; }
    public void setTotalGainLoss(BigDecimal totalGainLoss) { this.totalGainLoss = totalGainLoss; }
    public List<PositionResponse> getPositions() { return positions; }
    public void setPositions(List<PositionResponse> positions) { this.positions = positions; }
}
