package com.portfolio.portmstr.model;

import com.portfolio.portmstr.model.enums.PositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Investment Position entity.
 * Mapped from COBOL copybook POSREC.cpy (POSITION-RECORD) and
 * DB2 table INVESTMENT_POSITIONS (db2-definitions.sql).
 * VSAM KSDS key: POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID.
 */
@Entity
@Table(name = "INVESTMENT_POSITIONS")
@IdClass(InvestmentPositionId.class)
public class InvestmentPosition {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Id
    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "MARKET_VALUE", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "STATUS", length = 1)
    @Enumerated(EnumType.STRING)
    private PositionStatus status;

    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public InvestmentPosition() {
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
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

    public PositionStatus getStatus() {
        return status;
    }

    public void setStatus(PositionStatus status) {
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
