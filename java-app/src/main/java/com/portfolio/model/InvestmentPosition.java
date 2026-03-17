package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Investment Position entity.
 * Migrated from: DB2 INVESTMENT_POSITIONS table (db2-definitions.sql lines 29-41).
 * Copybook: POSREC.cpy
 *
 * All financial fields use BigDecimal to match COBOL fixed-point arithmetic:
 *   PIC S9(11)V9(4) COMP-3 -> BigDecimal (quantity)
 *   PIC S9(13)V9(2) COMP-3 -> BigDecimal (cost_basis, market_value)
 */
@Entity
@Table(name = "investment_positions")
public class InvestmentPosition {

    @EmbeddedId
    private InvestmentPositionKey key;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "market_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "investment_name", length = 50)
    private String investmentName;

    @Column(name = "last_activity_date", length = 8)
    private String lastActivityDate;

    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    public InvestmentPosition() {
    }

    public InvestmentPositionKey getKey() {
        return key;
    }

    public void setKey(InvestmentPositionKey key) {
        this.key = key;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastMaintDate() {
        return lastMaintDate;
    }

    public void setLastMaintDate(LocalDateTime lastMaintDate) {
        this.lastMaintDate = lastMaintDate;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }

    public String getInvestmentName() {
        return investmentName;
    }

    public void setInvestmentName(String investmentName) {
        this.investmentName = investmentName;
    }

    public String getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(String lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }
}
