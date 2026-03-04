package com.cobolbenchmark.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Position Record - migrated from POSREC.cpy.
 * Represents a portfolio investment position with composite key
 * (portfolioId, positionDate, investmentId).
 *
 * COMP-3 packed decimal fields are mapped to BigDecimal with explicit scale.
 */
@Entity
@Table(name = "INVESTMENT_POSITIONS")
@IdClass(PositionRecordKey.class)
public class PositionRecord {

    /** POS-PORTFOLIO-ID PIC X(8) */
    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    /** POS-DATE - position date */
    @Id
    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    /** POS-INVESTMENT-ID PIC X(10) */
    @Id
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    /** POS-QUANTITY PIC S9(11)V9(4) COMP-3 → BigDecimal scale 4 */
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3 → BigDecimal scale 2 */
    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3 → BigDecimal scale 2 */
    @Column(name = "MARKET_VALUE", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    /** POS-INVESTMENT-TYPE PIC X(2) */
    @Column(name = "INVESTMENT_TYPE", length = 2)
    private String investmentType;

    /** POS-STATUS PIC X(1) - level-88: A/C/P */
    @Column(name = "STATUS", length = 1)
    private String status;

    /** POS-CURRENCY-CODE PIC X(3) */
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** POS-LAST-MAINT-DATE */
    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private java.sql.Timestamp lastMaintDate;

    /** POS-LAST-MAINT-USER PIC X(8) */
    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public PositionRecord() {
    }

    /**
     * Copy constructor - replaces COBOL group-level MOVE.
     */
    public PositionRecord(PositionRecord other) {
        if (other != null) {
            copyFrom(other);
        }
    }

    /**
     * Replaces COBOL group-level MOVE DFHCOMMAREA TO WS-COMMAREA.
     */
    public void copyFrom(PositionRecord other) {
        this.portfolioId = other.portfolioId;
        this.positionDate = other.positionDate;
        this.investmentId = other.investmentId;
        this.investmentType = other.investmentType;
        this.status = other.status;
        this.quantity = other.quantity;
        this.costBasis = other.costBasis;
        this.marketValue = other.marketValue;
        this.currencyCode = other.currencyCode;
        this.lastMaintDate = other.lastMaintDate;
        this.lastMaintUser = other.lastMaintUser;
    }

    // Getters and Setters

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public java.sql.Timestamp getLastMaintDate() {
        return lastMaintDate;
    }

    public void setLastMaintDate(java.sql.Timestamp lastMaintDate) {
        this.lastMaintDate = lastMaintDate;
    }

    public String getInvestmentType() {
        return investmentType;
    }

    public void setInvestmentType(String investmentType) {
        this.investmentType = investmentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }
}
