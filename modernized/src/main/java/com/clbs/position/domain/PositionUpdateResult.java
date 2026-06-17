package com.clbs.position.domain;

import java.math.BigDecimal;

/**
 * Result of applying a single trade to a {@link PositionState}. Carries both the
 * new aggregated holding and the per-trade P&amp;L figures that the COBOL job
 * recorded to the DB2 {@code POSHIST} table
 * ({@code src/database/db2/POSHIST.sql} / copybook {@code DBTBLS.cpy}):
 * {@code PH-COST-BASIS}, {@code PH-GAIN-LOSS}.
 *
 * @param newState           the position holding after the trade is applied
 * @param costOfSharesSold   cost basis removed by a SELL (zero otherwise)
 * @param realizedGainLoss   realized P&amp;L for this trade ({@code PH-GAIN-LOSS}); non-zero only on SELL
 * @param unrealizedGainLoss mark-to-market P&amp;L of the resulting holding (marketValue - costBasis)
 * @param averageCost        derived unit cost of the resulting holding (costBasis / quantity)
 */
public record PositionUpdateResult(
        PositionState newState,
        BigDecimal costOfSharesSold,
        BigDecimal realizedGainLoss,
        BigDecimal unrealizedGainLoss,
        BigDecimal averageCost) {
}
