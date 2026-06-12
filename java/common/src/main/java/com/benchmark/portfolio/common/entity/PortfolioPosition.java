package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portfolio position record, migrated from POSREC.cpy (POSITION-RECORD).
 * VSAM KSDS with RECORD KEY POS-KEY = POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID,
 * mapped to table PORTFOLIO_POSITION with composite PK
 * (PORTFOLIO_ID, POSITION_DATE, INVESTMENT_ID).
 * POS-FILLER PIC X(50) is reserved space and is not migrated.
 */
@Entity
@Table(name = "PORTFOLIO_POSITION")
public class PortfolioPosition {

    /** POS-KEY = POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID. */
    @EmbeddedId
    private PortfolioPositionId id;

    /** POS-QUANTITY PIC S9(11)V9(4) COMP-3. */
    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** POS-COST-BASIS PIC S9(13)V9(2) COMP-3. */
    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3. */
    @Column(name = "MARKET_VALUE", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    /** POS-CURRENCY PIC X(03) (ISO 4217 code). */
    @Column(name = "CURRENCY_CODE", columnDefinition = "CHAR(3)", length = 3, nullable = false)
    private String currencyCode;

    /** POS-STATUS PIC X(01); 88-levels: 'A' Active, 'C' Closed, 'P' Pending. */
    @Column(name = "STATUS", columnDefinition = "CHAR(1)", length = 1, nullable = false)
    private String status;

    /** POS-LAST-MAINT-DATE PIC X(26) (DB2 timestamp format). */
    @Column(name = "LAST_MAINT_DATE")
    private LocalDateTime lastMaintDate;

    /** POS-LAST-MAINT-USER PIC X(08). */
    @Column(name = "LAST_MAINT_USER", length = 8)
    private String lastMaintUser;

    public PortfolioPositionId getId() {
        return id;
    }

    public void setId(PortfolioPositionId id) {
        this.id = id;
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
}
