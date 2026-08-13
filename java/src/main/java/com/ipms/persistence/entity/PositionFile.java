package com.ipms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Relational model of the VSAM KSDS position master file (POSFILE, read by INQPORT via
 * POSREC.cpy). Keyed on the POS-KEY: Portfolio ID (8) + Position Date (8, YYYYMMDD) +
 * Investment ID (10); non-key attributes follow the POSREC.cpy record layout.
 */
@Entity
@Table(name = "POSFILE")
@IdClass(PositionFile.Key.class)
public class PositionFile {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "POSITION_DATE", length = 8, nullable = false)
    private String positionDate;

    @Id
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "QUANTITY", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "COST_BASIS", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "MARKET_VALUE", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "LAST_MAINT_DATE", length = 26)
    private String lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8)
    private String lastMaintUser;

    public static class Key implements Serializable {
        private String portfolioId;
        private String positionDate;
        private String investmentId;

        public Key() {
        }

        public Key(String portfolioId, String positionDate, String investmentId) {
            this.portfolioId = portfolioId;
            this.positionDate = positionDate;
            this.investmentId = investmentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(positionDate, key.positionDate)
                    && Objects.equals(investmentId, key.investmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, positionDate, investmentId);
        }
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(String positionDate) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastMaintDate() {
        return lastMaintDate;
    }

    public void setLastMaintDate(String lastMaintDate) {
        this.lastMaintDate = lastMaintDate;
    }

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }
}
