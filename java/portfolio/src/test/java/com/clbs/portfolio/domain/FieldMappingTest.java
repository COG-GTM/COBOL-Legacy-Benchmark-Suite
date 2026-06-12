package com.clbs.portfolio.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.common.cobol.Comp3;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Validates that entity field types/scales match the copybook PIC clauses
 * (Phase 0, task 0.4 AC) without needing a database.
 */
class FieldMappingTest {

    @Test
    void portfolioMoneyFieldsKeepComp3Scale() {
        PortfolioMaster pm = new PortfolioMaster();
        pm.setKey(new PortfolioKey("PORT0001", "ACCT000001"));
        // S9(13)V99 -> scale 2
        pm.setTotalValue(Comp3.money(new BigDecimal("12345678.999")));
        pm.setCashBalance(Comp3.money(new BigDecimal("1000000")));

        assertThat(pm.getTotalValue()).isEqualByComparingTo("12345679.00");
        assertThat(pm.getTotalValue().scale()).isEqualTo(2);
        assertThat(pm.getCashBalance().scale()).isEqualTo(2);
        assertThat(pm.getKey().getPortId()).isEqualTo("PORT0001");
    }

    @Test
    void transactionQuantityAndPriceKeepFourDecimals() {
        TransactionRecord t = new TransactionRecord();
        t.setKey(new TransactionKey("20240320", "153045", "PORT0001", "000001"));
        // S9(11)V9(4) -> scale 4
        t.setQuantity(Comp3.quantity(new BigDecimal("100")));
        t.setPrice(Comp3.quantity(new BigDecimal("10.50005")));
        t.setAmount(Comp3.money(new BigDecimal("1050.005")));

        assertThat(t.getQuantity().scale()).isEqualTo(4);
        assertThat(t.getPrice()).isEqualByComparingTo("10.5001");
        assertThat(t.getAmount().scale()).isEqualTo(2);
    }

    @Test
    void embeddedKeysEqualByValue() {
        assertThat(new PositionKey("PORT0001", "20240320", "AAPL000001"))
                .isEqualTo(new PositionKey("PORT0001", "20240320", "AAPL000001"));
        assertThat(new HistoryKey("PORT0001", "20240320", "153045", "0001"))
                .isEqualTo(new HistoryKey("PORT0001", "20240320", "153045", "0001"));
    }
}
