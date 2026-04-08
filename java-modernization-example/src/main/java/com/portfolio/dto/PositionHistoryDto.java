package com.portfolio.dto;

import com.portfolio.model.PositionHistory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for history inquiry.
 * Maps fields from the POSHIST DB2 table that are relevant to the
 * P400-HISTORY-INQUIRY function in INQONLN.cbl.
 */
public class PositionHistoryDto {

    private String accountNo;
    private String portfolioId;
    private LocalDate transDate;
    private LocalTime transTime;
    private String transType;
    private String securityId;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fees;
    private BigDecimal totalAmount;
    private BigDecimal costBasis;
    private BigDecimal gainLoss;
    private Instant auditTimestamp;

    public PositionHistoryDto() {
    }

    /**
     * Factory method to convert a PositionHistory entity to a DTO.
     */
    public static PositionHistoryDto fromEntity(PositionHistory entity) {
        PositionHistoryDto dto = new PositionHistoryDto();
        dto.setAccountNo(entity.getAccountNo());
        dto.setPortfolioId(entity.getPortfolioId());
        dto.setTransDate(entity.getTransDate());
        dto.setTransTime(entity.getTransTime());
        dto.setTransType(entity.getTransType());
        dto.setSecurityId(entity.getSecurityId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setAmount(entity.getAmount());
        dto.setFees(entity.getFees());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCostBasis(entity.getCostBasis());
        dto.setGainLoss(entity.getGainLoss());
        dto.setAuditTimestamp(entity.getAuditTimestamp());
        return dto;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getTransDate() {
        return transDate;
    }

    public void setTransDate(LocalDate transDate) {
        this.transDate = transDate;
    }

    public LocalTime getTransTime() {
        return transTime;
    }

    public void setTransTime(LocalTime transTime) {
        this.transTime = transTime;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public void setFees(BigDecimal fees) {
        this.fees = fees;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getGainLoss() {
        return gainLoss;
    }

    public void setGainLoss(BigDecimal gainLoss) {
        this.gainLoss = gainLoss;
    }

    public Instant getAuditTimestamp() {
        return auditTimestamp;
    }

    public void setAuditTimestamp(Instant auditTimestamp) {
        this.auditTimestamp = auditTimestamp;
    }
}
