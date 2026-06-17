package com.clbs.position.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure, side-effect-free translation of the COBOL position-update business
 * logic. This is the calculation core of the modernized service and is unit
 * tested directly against the copybook field definitions.
 *
 * <p>Sources of the business rules:</p>
 * <ul>
 *   <li>{@code src/programs/portfolio/PORTTRAN.cbl} &mdash; the only concrete
 *       COBOL implementation of BUY/SELL/FEE/TRANSFER position updates,
 *       validation ({@code 2100}) and the insufficient-units guard ({@code 2220}).</li>
 *   <li>{@code documentation/technical/system-architecture.md} &mdash; POSUPDT
 *       spec: "Updates position records, Maintains cost basis, Records
 *       transaction history".</li>
 *   <li>{@code src/database/db2/POSHIST.sql} / {@code DBTBLS.cpy} &mdash; the
 *       {@code COST_BASIS} and {@code GAIN_LOSS} columns that define the P&amp;L
 *       output of each trade.</li>
 *   <li>{@code documentation/technical/data-dictionary.md} (5.1/5.2) &mdash;
 *       validation rules.</li>
 * </ul>
 *
 * <p>All arithmetic uses {@link BigDecimal} at the scales declared by the
 * copybook PIC clauses (see {@link MoneyScale}); floating point is never used.</p>
 */
public final class PositionCalculator {

    private PositionCalculator() {
    }

    /**
     * Field-level transaction validation. Ports {@code PORTTRAN.cbl}
     * {@code 2120-CHECK-TRANSACTION-TYPE} and {@code 2130-CHECK-AMOUNTS}:
     * quantity must be &gt; 0; price and amount must be &gt; 0 except for
     * transfers ({@code TRN-TYPE NOT = 'TR'}).
     */
    public static void validate(TradeInput trade) {
        if (trade.quantity().signum() <= 0) {
            throw new TransactionValidationException("Quantity must be greater than zero");
        }
        if (trade.type() != TransactionType.TRANSFER) {
            if (trade.price().signum() <= 0) {
                throw new TransactionValidationException("Price must be greater than zero");
            }
            if (trade.amount().signum() <= 0) {
                throw new TransactionValidationException("Amount must be greater than zero");
            }
        }
    }

    /**
     * Applies a single trade to a holding, mirroring {@code PORTTRAN.cbl}
     * {@code 2200-UPDATE-POSITIONS} ({@code EVALUATE TRN-TYPE}).
     *
     * @throws TransactionValidationException  invalid field values
     * @throws InsufficientPositionException   SELL exceeding the share balance
     * @throws UnsupportedTransactionException TRANSFER (unimplemented in COBOL)
     */
    public static PositionUpdateResult apply(PositionState current, TradeInput trade) {
        validate(trade);
        return switch (trade.type()) {
            case BUY -> processBuy(current, trade);
            case SELL -> processSell(current, trade);
            case FEE -> processFee(current, trade);
            case TRANSFER -> throw new UnsupportedTransactionException(
                    "Transfer processing not implemented");
        };
    }

    /**
     * BUY: {@code ADD TRN-QUANTITY TO POS-QUANTITY} and
     * {@code ADD TRN-AMOUNT TO POS-COST-BASIS}
     * ({@code PORTTRAN.cbl 2210-PROCESS-BUY}). Market value is re-marked to the
     * trade price. No realized P&amp;L on a buy.
     */
    private static PositionUpdateResult processBuy(PositionState current, TradeInput trade) {
        BigDecimal newQuantity = current.quantity().add(trade.quantity());
        BigDecimal newCostBasis = current.costBasis().add(trade.amount());
        BigDecimal marketValue = markToMarket(newQuantity, trade.price());
        PositionState newState = PositionState.of(newQuantity, newCostBasis, marketValue);
        return buildResult(newState, zeroAmount(), zeroAmount());
    }

    /**
     * SELL: validates the share balance ({@code IF POS-QUANTITY < TRN-QUANTITY})
     * then reduces quantity and cost basis. Cost basis is reduced by the
     * average-cost value of the shares sold (weighted-average cost method), and
     * the realized gain/loss recorded to {@code POSHIST.GAIN_LOSS} is
     * {@code proceeds - costOfSharesSold}.
     */
    private static PositionUpdateResult processSell(PositionState current, TradeInput trade) {
        if (current.quantity().compareTo(trade.quantity()) < 0) {
            throw new InsufficientPositionException("Insufficient units for sale");
        }
        BigDecimal avgCost = averageCost(current);
        BigDecimal costOfSharesSold = avgCost.multiply(trade.quantity())
                .setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING);
        BigDecimal newQuantity = current.quantity().subtract(trade.quantity());
        BigDecimal newCostBasis = current.costBasis().subtract(costOfSharesSold);
        BigDecimal proceeds = trade.amount();
        BigDecimal realized = proceeds.subtract(costOfSharesSold)
                .setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING);
        BigDecimal marketValue = markToMarket(newQuantity, trade.price());
        PositionState newState = PositionState.of(newQuantity, newCostBasis, marketValue);
        return buildResult(newState, costOfSharesSold, realized);
    }

    /**
     * FEE: {@code SUBTRACT TRN-AMOUNT FROM POS-COST-BASIS}
     * ({@code PORTTRAN.cbl 2240-PROCESS-FEE}). Quantity and market value are
     * unchanged; no realized P&amp;L.
     */
    private static PositionUpdateResult processFee(PositionState current, TradeInput trade) {
        BigDecimal newCostBasis = current.costBasis().subtract(trade.amount());
        PositionState newState = PositionState.of(
                current.quantity(), newCostBasis, current.marketValue());
        return buildResult(newState, zeroAmount(), zeroAmount());
    }

    /**
     * Folds a sequence of trades onto a starting holding &mdash; the
     * "trade aggregation" performed by the batch loop
     * ({@code PORTTRAN.cbl 2000-PROCESS-TRANSACTIONS} reading the transaction
     * file sequentially and updating the position master).
     */
    public static PositionState aggregate(PositionState start, List<TradeInput> trades) {
        PositionState state = start;
        for (TradeInput trade : trades) {
            state = apply(state, trade).newState();
        }
        return state;
    }

    /**
     * Weighted-average unit cost of a holding: {@code costBasis / quantity},
     * carried at extra precision ({@link MoneyScale#UNIT_COST_SCALE}). Returns
     * zero for an empty holding (avoids COBOL S0C7 / divide-by-zero).
     */
    public static BigDecimal averageCost(PositionState state) {
        if (state.quantity().signum() == 0) {
            return BigDecimal.ZERO.setScale(MoneyScale.UNIT_COST_SCALE);
        }
        return state.costBasis().divide(
                state.quantity(), MoneyScale.UNIT_COST_SCALE, MoneyScale.ROUNDING);
    }

    /**
     * Unrealized (mark-to-market) gain/loss of a holding:
     * {@code marketValue - costBasis}.
     */
    public static BigDecimal unrealizedGainLoss(PositionState state) {
        return state.marketValue().subtract(state.costBasis())
                .setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING);
    }

    private static BigDecimal markToMarket(BigDecimal quantity, BigDecimal price) {
        return quantity.multiply(price).setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING);
    }

    private static PositionUpdateResult buildResult(
            PositionState newState, BigDecimal costOfSharesSold, BigDecimal realized) {
        return new PositionUpdateResult(
                newState,
                costOfSharesSold,
                realized,
                unrealizedGainLoss(newState),
                newState.quantity().signum() == 0
                        ? BigDecimal.ZERO.setScale(MoneyScale.QUANTITY_SCALE)
                        : newState.costBasis().divide(
                                newState.quantity(), MoneyScale.QUANTITY_SCALE, MoneyScale.ROUNDING));
    }

    private static BigDecimal zeroAmount() {
        return BigDecimal.ZERO.setScale(MoneyScale.AMOUNT_SCALE);
    }
}
