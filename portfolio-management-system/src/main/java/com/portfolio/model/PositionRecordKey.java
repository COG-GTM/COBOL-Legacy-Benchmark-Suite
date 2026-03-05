package com.portfolio.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for PositionRecord (POSITION_MASTER table).
 * Key: (PORTFOLIO_ID, SYMBOL_ID) - replaces VSAM KSDS key structure.
 */
public class PositionRecordKey implements Serializable {

    private String portfolioId;
    private String symbolId;

    public PositionRecordKey() {}

    public PositionRecordKey(String portfolioId, String symbolId) {
        this.portfolioId = portfolioId;
        this.symbolId = symbolId;
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public String getSymbolId() { return symbolId; }
    public void setSymbolId(String symbolId) { this.symbolId = symbolId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionRecordKey that = (PositionRecordKey) o;
        return Objects.equals(portfolioId, that.portfolioId) &&
               Objects.equals(symbolId, that.symbolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, symbolId);
    }
}
