package com.portfolio.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PositionRecordId implements Serializable {
    private String portfolioId;
    private String investmentId;
    private LocalDate positionDate;

    public PositionRecordId() {}

    public PositionRecordId(String portfolioId, String investmentId, LocalDate positionDate) {
        this.portfolioId = portfolioId;
        this.investmentId = investmentId;
        this.positionDate = positionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionRecordId that = (PositionRecordId) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(investmentId, that.investmentId)
                && Objects.equals(positionDate, that.positionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, investmentId, positionDate);
    }
}
