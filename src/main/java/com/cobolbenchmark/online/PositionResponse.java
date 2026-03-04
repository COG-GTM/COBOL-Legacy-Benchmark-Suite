package com.cobolbenchmark.online;

import java.math.BigDecimal;
import java.util.List;

/**
 * Position Response DTO - replaces POSMAP BMS map output.
 * Maps to EXEC CICS SEND MAP('POSMAP') output fields.
 */
public class PositionResponse {

    private String portfolioId;
    private String portfolioName;
    private String status;
    private List<PositionDetail> positions;
    private BigDecimal totalMarketValue;
    private BigDecimal totalCostBasis;
    private BigDecimal totalGainLoss;
    private String message;

    public PositionResponse() {
    }

    public static class PositionDetail {
        private String investmentId;
        private String investmentType;
        private String positionDate;
        private BigDecimal quantity;
        private BigDecimal costBasis;
        private BigDecimal marketValue;
        private BigDecimal gainLoss;
        private String status;

        public String getInvestmentId() { return investmentId; }
        public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

        public String getInvestmentType() { return investmentType; }
        public void setInvestmentType(String investmentType) { this.investmentType = investmentType; }

        public String getPositionDate() { return positionDate; }
        public void setPositionDate(String positionDate) { this.positionDate = positionDate; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getCostBasis() { return costBasis; }
        public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }

        public BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

        public BigDecimal getGainLoss() { return gainLoss; }
        public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Getters and Setters

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<PositionDetail> getPositions() { return positions; }
    public void setPositions(List<PositionDetail> positions) { this.positions = positions; }

    public BigDecimal getTotalMarketValue() { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }

    public BigDecimal getTotalCostBasis() { return totalCostBasis; }
    public void setTotalCostBasis(BigDecimal totalCostBasis) { this.totalCostBasis = totalCostBasis; }

    public BigDecimal getTotalGainLoss() { return totalGainLoss; }
    public void setTotalGainLoss(BigDecimal totalGainLoss) { this.totalGainLoss = totalGainLoss; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
