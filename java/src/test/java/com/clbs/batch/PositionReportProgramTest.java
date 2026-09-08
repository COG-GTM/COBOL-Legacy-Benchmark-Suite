package com.clbs.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.common.ProgramResult;
import com.clbs.domain.PositionRecord;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositionReportProgramTest {

    private static PositionRecord position(String market, String previous) {
        return position("IBM0000001", market, previous);
    }

    private static PositionRecord position(String investmentId, String market, String previous) {
        PositionRecord record = new PositionRecord();
        record.setPortfolioId("PORT0001");
        record.setInvestmentId(investmentId);
        record.setDescription("IBM COMMON");
        record.setQuantity(new BigDecimal("100.0000"));
        record.setMarketValue(new BigDecimal(market));
        record.setPreviousValue(new BigDecimal(previous));
        return record;
    }

    @Test
    void changePercentIsRoundedToTwoDecimals() {
        assertThat(PositionReportProgram.changePercent(position("11000.00", "10000.00")))
                .isEqualByComparingTo("10.00");
        assertThat(PositionReportProgram.changePercent(position("9500.00", "10000.00")))
                .isEqualByComparingTo("-5.00");
    }

    @Test
    void changePercentIsZeroWhenPreviousValueIsZero() {
        BigDecimal percent = PositionReportProgram.changePercent(position("11000.00", "0.00"));
        assertThat(percent).isEqualByComparingTo("0.00");
        assertThat(percent.scale()).isEqualTo(2);
    }

    @Test
    void reportWritesHeadersAndOneLinePerPosition() {
        DatasetCatalog datasets = new DatasetCatalog();
        datasets.positionFile().open();
        datasets.positionFile().write(position("IBM0000001", "11000.00", "10000.00"));
        datasets.positionFile().write(position("MSFT000001", "9000.00", "10000.00"));

        ProgramResult result = new PositionReportProgram(datasets).run();

        assertThat(result.counter("positions")).isEqualTo(2);
        assertThat(result.getReturnCode()).isEqualTo(BatchConstants.RC_SUCCESS);
        assertThat(result.getDisplay()).anyMatch(line -> line.contains("DAILY POSITION REPORT"));
        assertThat(result.getDisplay())
                .allMatch(line -> line.length() == PositionReportProgram.WIDTH);
    }
}
