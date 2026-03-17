package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Position History entity.
 * Migrated from: VSAM POSHIST file (350-byte records,
 * key = Portfolio ID 8 + Position Date 8 + Investment ID 10).
 * Copybook: HISTREC.cpy
 * Record types: PT=Portfolio, PS=Position, TR=Transaction
 * Action codes: A=Add, C=Change, D=Delete
 */
@Entity
@Table(name = "position_history")
@IdClass(PositionHistoryKey.class)
public class PositionHistory {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @Id
    @Column(name = "history_time", nullable = false)
    private LocalTime historyTime;

    @Id
    @Column(name = "sequence_no", length = 4, nullable = false)
    private String sequenceNo;

    @Column(name = "record_type", length = 2, nullable = false)
    private String recordType;

    @Column(name = "action_code", length = 1, nullable = false)
    private String actionCode;

    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 18, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 18, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "before_image", columnDefinition = "TEXT")
    private String beforeImage;

    @Column(name = "after_image", columnDefinition = "TEXT")
    private String afterImage;

    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date", nullable = false)
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    public PositionHistory() {
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getHistoryDate() {
        return historyDate;
    }

    public void setHistoryDate(LocalDate historyDate) {
        this.historyDate = historyDate;
    }

    public LocalTime getHistoryTime() {
        return historyTime;
    }

    public void setHistoryTime(LocalTime historyTime) {
        this.historyTime = historyTime;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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

    public String getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(String beforeImage) {
        this.beforeImage = beforeImage;
    }

    public String getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(String afterImage) {
        this.afterImage = afterImage;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public LocalDateTime getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDateTime processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
