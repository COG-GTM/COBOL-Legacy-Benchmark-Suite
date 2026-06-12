package com.clbs.reporting;

import com.clbs.portfolio.domain.PositionRecord;
import java.math.BigDecimal;
import java.util.List;

/**
 * Position valuation report — the migration target for RPTPOS00. Phase 0 provides
 * the core aggregation (total market value, cost basis, unrealized gain/loss);
 * formatted output layouts are added in later phases.
 */
public final class PositionReport {

    public record Totals(int positionCount, BigDecimal marketValue, BigDecimal costBasis, BigDecimal gainLoss) {
    }

    public Totals summarize(List<PositionRecord> positions) {
        BigDecimal marketValue = BigDecimal.ZERO.setScale(2);
        BigDecimal costBasis = BigDecimal.ZERO.setScale(2);
        for (PositionRecord position : positions) {
            marketValue = marketValue.add(position.getMarketValue());
            costBasis = costBasis.add(position.getCostBasis());
        }
        return new Totals(positions.size(), marketValue, costBasis, marketValue.subtract(costBasis));
    }
}
