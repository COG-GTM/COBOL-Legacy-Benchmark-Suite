package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.entity.HistoryRecord;
import com.clbs.portfolio.entity.PositionHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryLoadProcessorTest {

    private HistoryLoadProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new HistoryLoadProcessor();
    }

    @Test
    void shouldMapAllFieldsFromHistoryRecord() throws Exception {
        HistoryRecord input = HistoryRecord.builder()
                .accountNo("ACCT001234")
                .portfolioId("PORT001234")
                .transDate("2024-03-15")
                .transTime("14:30:25")
                .transType("BU")
                .securityId("AAPL00000001")
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("150.0000"))
                .amount(new BigDecimal("15000.00"))
                .fees(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("15015.00"))
                .costBasis(new BigDecimal("15000.00"))
                .gainLoss(new BigDecimal("0.00"))
                .build();

        PositionHistory result = processor.process(input);

        assertThat(result.getAccountNo()).isEqualTo("ACCT001234");
        assertThat(result.getPortfolioId()).isEqualTo("PORT001234");
        assertThat(result.getTransDate()).isEqualTo("2024-03-15");
        assertThat(result.getTransTime()).isEqualTo("14:30:25");
        assertThat(result.getTransType()).isEqualTo("BU");
        assertThat(result.getSecurityId()).isEqualTo("AAPL00000001");
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("100.000"));
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("150.000"));
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(result.getFees()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("15015.00"));
        assertThat(result.getCostBasis()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(result.getGainLoss()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(result.getProgramId()).isEqualTo("HISTLD00");
        assertThat(result.getUserId()).isEqualTo("BATCH");
        assertThat(result.getAuditTimestamp()).isNotNull();
        assertThat(result.getProcessDate()).isNotNull();
        assertThat(result.getProcessTime()).isNotNull();
    }

    @Test
    void shouldHandleNullFees() throws Exception {
        HistoryRecord input = HistoryRecord.builder()
                .accountNo("ACCT001234")
                .portfolioId("PORT001234")
                .transDate("2024-03-15")
                .transTime("14:30:25")
                .transType("SL")
                .securityId("AAPL00000001")
                .quantity(new BigDecimal("50.0000"))
                .price(new BigDecimal("200.0000"))
                .amount(new BigDecimal("10000.00"))
                .fees(null)
                .totalAmount(new BigDecimal("10000.00"))
                .costBasis(new BigDecimal("7500.00"))
                .gainLoss(new BigDecimal("2500.00"))
                .build();

        PositionHistory result = processor.process(input);

        assertThat(result.getFees()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getGainLoss()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    void shouldSetQuantityScaleTo3Decimals() throws Exception {
        HistoryRecord input = createMinimalHistoryRecord();
        input.setQuantity(new BigDecimal("100.12345"));

        PositionHistory result = processor.process(input);
        assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("100.123"));
    }

    @Test
    void shouldSetPriceScaleTo3Decimals() throws Exception {
        HistoryRecord input = createMinimalHistoryRecord();
        input.setPrice(new BigDecimal("99.99999"));

        PositionHistory result = processor.process(input);
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("100.000"));
    }

    @Test
    void shouldHandleNullQuantityAsZero() throws Exception {
        HistoryRecord input = createMinimalHistoryRecord();
        input.setQuantity(null);

        PositionHistory result = processor.process(input);
        assertThat(result.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private HistoryRecord createMinimalHistoryRecord() {
        return HistoryRecord.builder()
                .accountNo("ACCT000001")
                .portfolioId("PORT000001")
                .transDate("2024-01-01")
                .transTime("00:00:00")
                .transType("BU")
                .securityId("SEC000000001")
                .quantity(new BigDecimal("10.0000"))
                .price(new BigDecimal("100.0000"))
                .amount(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("1000.00"))
                .costBasis(new BigDecimal("1000.00"))
                .gainLoss(BigDecimal.ZERO)
                .build();
    }
}
