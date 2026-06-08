package com.portfolio.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link Position}.
 */
public class PositionId implements Serializable {

    private String portfolioId;
    private String positionDate;
    private String investmentId;

    public PositionId() {}

    public PositionId(String portfolioId, String positionDate, String investmentId) {
        this.portfolioId = portfolioId;
        this.positionDate = positionDate;
        this.investmentId = investmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionId that)) return false;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(positionDate, that.positionDate)
                && Objects.equals(investmentId, that.investmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, positionDate, investmentId);
    }
}
