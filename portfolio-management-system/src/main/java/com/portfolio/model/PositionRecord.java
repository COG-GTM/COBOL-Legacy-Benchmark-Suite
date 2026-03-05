package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Position Record entity.
 * Migrated from COBOL POSREC copybook.
 * Replaces VSAM KSDS PORTFOLIO.POSITION.VSAM (PORTDFN.csd lines 69-79)
 * RECORDSIZE(200), STRINGS(10)
 */
@Entity
@Table(name = "POSITION_MASTER")
@IdClass(PositionRecordKey.class)
public class PositionRecord {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "SYMBOL_ID", length = 10, nullable = false)
    private String symbolId;

    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    /** PIC S9(11)V9(4) COMP-3 -> BigDecimal */
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** PIC S9(13)V9(2) COMP-3 -> BigDecimal */
    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** PIC S9(13)V9(2) COMP-3 -> BigDecimal */
    @Column(name = "MARKET_VALUE", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** Status: A=Active, C=Closed, P=Pending */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public PositionRecord() {}

    // Status constants from POSREC copybook
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";
    public static final String STATUS_PENDING = "P";

    // Getters and setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getSymbolId() { return symbolId; }
    public void setSymbolId(String symbolId) { this.symbolId = symbolId; }

    public LocalDate getPositionDate() { return positionDate; }
    public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }

    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }

    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
}
