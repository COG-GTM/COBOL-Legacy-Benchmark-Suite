package com.clbs.position.domain;

import java.math.BigDecimal;

/**
 * Immutable snapshot of the calculable fields of a position holding, mirroring
 * the {@code POS-DATA} group of {@code POSITION-RECORD}
 * (copybook {@code src/copybook/common/POSREC.cpy}):
 *
 * <pre>
 *   05  POS-DATA.
 *       10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *       10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
 *       10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
 * </pre>
 *
 * @param quantity    holding quantity ({@code POS-QUANTITY}), scale 4
 * @param costBasis   total cost basis ({@code POS-COST-BASIS}), scale 2
 * @param marketValue current market value ({@code POS-MARKET-VALUE}), scale 2
 */
public record PositionState(BigDecimal quantity, BigDecimal costBasis, BigDecimal marketValue) {

    /** An empty (zeroed) holding, the starting point for trade aggregation. */
    public static PositionState empty() {
        return new PositionState(
                BigDecimal.ZERO.setScale(MoneyScale.QUANTITY_SCALE),
                BigDecimal.ZERO.setScale(MoneyScale.AMOUNT_SCALE),
                BigDecimal.ZERO.setScale(MoneyScale.AMOUNT_SCALE));
    }

    public static PositionState of(BigDecimal quantity, BigDecimal costBasis, BigDecimal marketValue) {
        return new PositionState(
                quantity.setScale(MoneyScale.QUANTITY_SCALE, MoneyScale.ROUNDING),
                costBasis.setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING),
                marketValue.setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING));
    }
}
