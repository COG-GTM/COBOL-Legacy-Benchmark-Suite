package com.portfolio.dto;

import com.portfolio.entity.InvestmentPosition;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Position response DTO - maps to BMS POSMAP position detail fields.
 */
public class PositionResponse {

    private String portfolioId;
    private String investmentId;
    private LocalDate positionDate;
    private BigDecimal quantity;
    private BigDecimal costBasis;
    private BigDecimal marketValue;
    private BigDecimal unrealizedGainLoss;
    private String currencyCode;
    private String status;

    public PositionResponse() {
    }

    public static PositionResponse fromEntity(InvestmentPosition entity) {
        PositionResponse response = new PositionResponse();
        response.setPortfolioId(entity.getPortfolioId());
        response.setInvestmentId(entity.getInvestmentId());
        response.setPositionDate(entity.getPositionDate());
        response.setQuantity(entity.getQuantity());
        response.setCostBasis(entity.getCostBasis());
        response.setMarketValue(entity.getMarketValue());
        response.setUnrealizedGainLoss(entity.getUnrealizedGainLoss());
        response.setCurrencyCode(entity.getCurrencyCode());
        response.setStatus(entity.getStatus());
        return response;
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
    public BigDecimal getUnrealizedGainLoss() { return unrealizedGainLoss; }
    public void setUnrealizedGainLoss(BigDecimal unrealizedGainLoss) { this.unrealizedGainLoss = unrealizedGainLoss; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
