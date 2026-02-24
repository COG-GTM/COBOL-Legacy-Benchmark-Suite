package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity mapping for the Investment Positions table (Position entity).
 *
 * COBOL Source: POSREC.cpy (POSITION-RECORD)
 *   POS-KEY: POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID
 *   POS-DATA: POS-QUANTITY, POS-COST-BASIS, POS-MARKET-VALUE, POS-CURRENCY, POS-STATUS
 *   POS-AUDIT: POS-LAST-MAINT-DATE, POS-LAST-MAINT-USER
 *
 * DB2 Source: db2-definitions.sql (INVESTMENT_POSITIONS)
 *   Composite PK: (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE)
 *   FK: PORTFOLIO_ID -> PORTFOLIO_MASTER(PORTFOLIO_ID)
 *
 * Uses @EmbeddedId for the composite key matching the VSAM Position Master
 * key structure (Portfolio-ID + Symbol-ID).
 */
@Entity
@Table(name = "investment_positions")
public class InvestmentPosition {

    @EmbeddedId
    private InvestmentPositionId id;

    /**
     * Holding Quantity.
     * COBOL: POS-QUANTITY PIC S9(11)V9(4) COMP-3
     * DB2: QUANTITY DECIMAL(18,4) NOT NULL
     */
    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    @NotNull
    private BigDecimal quantity;

    /**
     * Total Cost Basis.
     * COBOL: POS-COST-BASIS PIC S9(13)V9(2) COMP-3
     * DB2: COST_BASIS DECIMAL(18,2) NOT NULL
     */
    @Column(name = "cost_basis", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal costBasis;

    /**
     * Current Market Value.
     * COBOL: POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3
     * DB2: MARKET_VALUE DECIMAL(18,2) NOT NULL
     */
    @Column(name = "market_value", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal marketValue;

    /**
     * Currency Code (e.g. USD, EUR, GBP).
     * COBOL: POS-CURRENCY PIC X(03)
     * DB2: CURRENCY_CODE CHAR(3) NOT NULL
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    @NotNull
    @Size(max = 3)
    private String currencyCode;

    /**
     * Last Maintenance Timestamp (audit field).
     * COBOL: POS-LAST-MAINT-DATE PIC X(26)
     * DB2: LAST_MAINT_DATE TIMESTAMP NOT NULL
     */
    @Column(name = "last_maint_date", nullable = false)
    @NotNull
    private LocalDateTime lastMaintDate;

    /**
     * Last Maintenance User (audit field).
     * COBOL: POS-LAST-MAINT-USER PIC X(08)
     * DB2: LAST_MAINT_USER VARCHAR(8) NOT NULL
     */
    @Column(name = "last_maint_user", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String lastMaintUser;

    /**
     * Many-to-one relationship to Portfolio Master.
     * Maps the portfolio_id portion of the composite key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", insertable = false, updatable = false)
    private PortfolioMaster portfolio;

    public InvestmentPosition() {
    }

    // --- Getters and Setters ---

    public InvestmentPositionId getId() {
        return id;
    }

    public void setId(InvestmentPositionId id) {
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

    public PortfolioMaster getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(PortfolioMaster portfolio) {
        this.portfolio = portfolio;
    }
}
