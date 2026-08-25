package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity for the DB2 INVESTMENT_POSITIONS table
 * ({@code src/database/db2/db2-definitions.sql}); the corresponding VSAM/COBOL
 * record is 01 POSITION-RECORD in {@code src/copybook/common/POSREC.cpy}.
 *
 * <p>Primary key: (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE).
 */
@Entity
@Table(name = "INVESTMENT_POSITIONS")
public class InvestmentPosition {

    @EmbeddedId
    private Key key;

    /** QUANTITY DECIMAL(18,4) / POS-QUANTITY PIC S9(11)V9(4) COMP-3. */
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** COST_BASIS DECIMAL(18,2) / POS-COST-BASIS PIC S9(13)V9(2) COMP-3. */
    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** MARKET_VALUE DECIMAL(18,2) / POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3. */
    @Column(name = "MARKET_VALUE", precision = 18, scale = 2, nullable = false)
    private BigDecimal marketValue;

    /** CURRENCY_CODE CHAR(3) / POS-CURRENCY PIC X(03). */
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** LAST_MAINT_DATE TIMESTAMP / POS-LAST-MAINT-DATE PIC X(26). */
    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    /** LAST_MAINT_USER VARCHAR(8) / POS-LAST-MAINT-USER PIC X(08). */
    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    /** Composite primary key (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE). */
    @Embeddable
    public static class Key implements Serializable {

        /** PORTFOLIO_ID CHAR(8) / POS-PORTFOLIO-ID PIC X(08). */
        @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
        private String portfolioId;

        /** INVESTMENT_ID CHAR(10) / POS-INVESTMENT-ID PIC X(10). */
        @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
        private String investmentId;

        /** POSITION_DATE DATE / POS-DATE PIC X(08). */
        @Column(name = "POSITION_DATE", nullable = false)
        private LocalDate positionDate;

        public Key() {}

        public Key(String portfolioId, String investmentId, LocalDate positionDate) {
            this.portfolioId = portfolioId;
            this.investmentId = investmentId;
            this.positionDate = positionDate;
        }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
        public String getInvestmentId() { return investmentId; }
        public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
        public LocalDate getPositionDate() { return positionDate; }
        public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(investmentId, key.investmentId)
                    && Objects.equals(positionDate, key.positionDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, investmentId, positionDate);
        }
    }

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
}
