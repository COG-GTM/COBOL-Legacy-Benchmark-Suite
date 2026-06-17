package com.clbs.position.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the ported COBOL calculation logic against the copybook field
 * definitions (POSREC / TRNREC) and the documented business rules. This is the
 * primary equivalence test of the migration.
 */
class PositionCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static TradeInput buy(String qty, String price, String amount) {
        return new TradeInput(TransactionType.BUY, bd(qty), bd(price), bd(amount));
    }

    private static TradeInput sell(String qty, String price, String amount) {
        return new TradeInput(TransactionType.SELL, bd(qty), bd(price), bd(amount));
    }

    @Test
    @DisplayName("BUY onto an empty holding adds quantity and cost basis (PORTTRAN 2210)")
    void buyOntoEmpty() {
        PositionUpdateResult r = PositionCalculator.apply(PositionState.empty(), buy("100", "55", "5500.00"));

        assertThat(r.newState().quantity()).isEqualByComparingTo("100.0000");
        assertThat(r.newState().costBasis()).isEqualByComparingTo("5500.00");
        assertThat(r.newState().marketValue()).isEqualByComparingTo("5500.00"); // 100 * 55
        assertThat(r.realizedGainLoss()).isEqualByComparingTo("0.00");
        assertThat(r.averageCost()).isEqualByComparingTo("55.0000");
        assertThat(r.unrealizedGainLoss()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("BUY onto an existing holding aggregates quantity and cost (trade aggregation)")
    void buyAggregates() {
        PositionState start = PositionState.of(bd("1000.0000"), bd("50000.00"), bd("52000.00"));

        PositionUpdateResult r = PositionCalculator.apply(start, buy("100", "55", "5500.00"));

        assertThat(r.newState().quantity()).isEqualByComparingTo("1100.0000");
        assertThat(r.newState().costBasis()).isEqualByComparingTo("55500.00");
        assertThat(r.newState().marketValue()).isEqualByComparingTo("60500.00"); // 1100 * 55
    }

    @Test
    @DisplayName("SELL reduces cost basis at average cost and computes realized P&L")
    void sellComputesRealizedGain() {
        PositionState start = PositionState.of(bd("1000.0000"), bd("50000.00"), bd("52000.00"));

        // avg cost = 50000 / 1000 = 50.00; sell 100 @ proceeds 5500
        PositionUpdateResult r = PositionCalculator.apply(start, sell("100", "55", "5500.00"));

        assertThat(r.costOfSharesSold()).isEqualByComparingTo("5000.00"); // 50 * 100
        assertThat(r.newState().quantity()).isEqualByComparingTo("900.0000");
        assertThat(r.newState().costBasis()).isEqualByComparingTo("45000.00");
        assertThat(r.realizedGainLoss()).isEqualByComparingTo("500.00"); // 5500 - 5000
        assertThat(r.newState().marketValue()).isEqualByComparingTo("49500.00"); // 900 * 55
    }

    @Test
    @DisplayName("SELL at a loss yields a negative realized gain/loss")
    void sellAtLoss() {
        PositionState start = PositionState.of(bd("1000.0000"), bd("50000.00"), bd("52000.00"));

        // avg cost 50; sell 100, proceeds only 4000 -> loss of 1000
        PositionUpdateResult r = PositionCalculator.apply(start, sell("100", "40", "4000.00"));

        assertThat(r.realizedGainLoss()).isEqualByComparingTo("-1000.00");
    }

    @Test
    @DisplayName("SELL exceeding the share balance is rejected (PORTTRAN 2220 / rule E004)")
    void sellInsufficientBalance() {
        PositionState start = PositionState.of(bd("50.0000"), bd("2500.00"), bd("2600.00"));

        assertThatThrownBy(() -> PositionCalculator.apply(start, sell("100", "55", "5500.00")))
                .isInstanceOf(InsufficientPositionException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("FEE reduces cost basis only, leaving quantity and market value (PORTTRAN 2240)")
    void feeReducesCostBasis() {
        PositionState start = PositionState.of(bd("1000.0000"), bd("50000.00"), bd("52000.00"));
        TradeInput fee = new TradeInput(TransactionType.FEE, bd("1"), bd("1"), bd("25.00"));

        PositionUpdateResult r = PositionCalculator.apply(start, fee);

        assertThat(r.newState().quantity()).isEqualByComparingTo("1000.0000");
        assertThat(r.newState().costBasis()).isEqualByComparingTo("49975.00");
        assertThat(r.newState().marketValue()).isEqualByComparingTo("52000.00");
        assertThat(r.realizedGainLoss()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("TRANSFER is rejected as unimplemented (PORTTRAN 2230)")
    void transferUnsupported() {
        TradeInput transfer = new TradeInput(TransactionType.TRANSFER, bd("10"), bd("0"), bd("0"));

        assertThatThrownBy(() -> PositionCalculator.apply(PositionState.empty(), transfer))
                .isInstanceOf(UnsupportedTransactionException.class);
    }

    @Test
    @DisplayName("Validation rejects non-positive quantity, price and amount (PORTTRAN 2130)")
    void validationRules() {
        assertThatThrownBy(() -> PositionCalculator.apply(PositionState.empty(), buy("0", "55", "5500")))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Quantity");

        assertThatThrownBy(() -> PositionCalculator.apply(PositionState.empty(), buy("100", "0", "5500")))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Price");

        assertThatThrownBy(() -> PositionCalculator.apply(PositionState.empty(), buy("100", "55", "0")))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("Amount");
    }

    @Test
    @DisplayName("aggregate() folds a sequence of trades onto a holding (sequential batch loop)")
    void aggregateFoldsTrades() {
        PositionState result = PositionCalculator.aggregate(
                PositionState.empty(),
                List.of(buy("100", "55", "5500.00"), buy("100", "60", "6000.00")));

        assertThat(result.quantity()).isEqualByComparingTo("200.0000");
        assertThat(result.costBasis()).isEqualByComparingTo("11500.00");
        assertThat(result.marketValue()).isEqualByComparingTo("12000.00"); // marked at last price 60
        assertThat(PositionCalculator.averageCost(result)).isEqualByComparingTo("57.50000000");
    }

    @Test
    @DisplayName("Fields preserve copybook decimal scales (quantity=4, amount=2)")
    void preservesCopybookScales() {
        PositionUpdateResult r = PositionCalculator.apply(
                PositionState.empty(), buy("100.1234", "10.0000", "1001.23"));

        assertThat(r.newState().quantity().scale()).isEqualTo(MoneyScale.QUANTITY_SCALE);
        assertThat(r.newState().costBasis().scale()).isEqualTo(MoneyScale.AMOUNT_SCALE);
        assertThat(r.newState().marketValue().scale()).isEqualTo(MoneyScale.AMOUNT_SCALE);
    }

    @Test
    @DisplayName("averageCost of an empty holding is zero (no divide-by-zero / S0C7)")
    void averageCostOfEmpty() {
        assertThat(PositionCalculator.averageCost(PositionState.empty())).isEqualByComparingTo("0");
    }
}
