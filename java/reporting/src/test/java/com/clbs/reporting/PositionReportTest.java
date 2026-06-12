package com.clbs.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.portfolio.domain.PositionKey;
import com.clbs.portfolio.domain.PositionRecord;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionReportTest {

    @Test
    void summarizesMarketValueCostBasisAndGainLoss() {
        PositionReport report = new PositionReport();
        PositionReport.Totals totals = report.summarize(List.of(
                position("AAPL000001", "15000.00", "17500.00"),
                position("MSFT000001", "20000.00", "19000.00")));

        assertThat(totals.positionCount()).isEqualTo(2);
        assertThat(totals.marketValue()).isEqualByComparingTo("36500.00");
        assertThat(totals.costBasis()).isEqualByComparingTo("35000.00");
        assertThat(totals.gainLoss()).isEqualByComparingTo("1500.00");
    }

    private static PositionRecord position(String investmentId, String costBasis, String marketValue) {
        PositionRecord p = new PositionRecord();
        p.setKey(new PositionKey("PORT0001", "20240320", investmentId));
        p.setQuantity(new BigDecimal("100.0000"));
        p.setCostBasis(new BigDecimal(costBasis));
        p.setMarketValue(new BigDecimal(marketValue));
        p.setCurrency("USD");
        p.setStatus("A");
        p.setLastMaintDate("2024-03-20-15.30.45.123456");
        p.setLastMaintUser("TSTGEN00");
        return p;
    }
}
