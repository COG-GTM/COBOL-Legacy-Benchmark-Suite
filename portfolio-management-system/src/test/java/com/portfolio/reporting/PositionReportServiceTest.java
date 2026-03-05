package com.portfolio.reporting;

import com.portfolio.model.PositionRecord;
import com.portfolio.support.PositionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PositionReportService.
 * Validates report record counts and key field values.
 */
@SpringBootTest
@ActiveProfiles("test")
class PositionReportServiceTest {

    @Autowired
    private PositionReportService reportService;

    @Autowired
    private PositionRecordRepository positionRepository;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
    }

    @Test
    void testGenerateEmptyReport() {
        List<PositionReportService.PositionReportLine> report = reportService.generateDailyReport();
        assertThat(report).isEmpty();
    }

    @Test
    void testGenerateReportWithPositions() {
        positionRepository.save(createPosition("PORT0001", "AAPL      ",
                new BigDecimal("100.0000"), new BigDecimal("5000.00"), new BigDecimal("5500.00")));
        positionRepository.save(createPosition("PORT0001", "GOOGL     ",
                new BigDecimal("50.0000"), new BigDecimal("7500.00"), new BigDecimal("8000.00")));

        List<PositionReportService.PositionReportLine> report = reportService.generateDailyReport();

        // 2 position lines + 1 summary line
        assertThat(report).hasSize(3);

        // Find summary line
        PositionReportService.PositionReportLine summary = report.stream()
                .filter(l -> "**TOTAL**".equals(l.getSymbolId()))
                .findFirst().orElse(null);
        assertThat(summary).isNotNull();
        assertThat(summary.getCostBasis()).isEqualByComparingTo(new BigDecimal("12500.00"));
        assertThat(summary.getMarketValue()).isEqualByComparingTo(new BigDecimal("13500.00"));
    }

    @Test
    void testReportGainLossCalculation() {
        positionRepository.save(createPosition("PORT0001", "AAPL      ",
                new BigDecimal("100.0000"), new BigDecimal("5000.00"), new BigDecimal("5500.00")));

        List<PositionReportService.PositionReportLine> report = reportService.generateDailyReport();

        PositionReportService.PositionReportLine line = report.stream()
                .filter(l -> !"**TOTAL**".equals(l.getSymbolId()))
                .findFirst().orElse(null);
        assertThat(line).isNotNull();
        assertThat(line.getGainLoss()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    private PositionRecord createPosition(String portfolioId, String symbolId,
                                           BigDecimal qty, BigDecimal costBasis, BigDecimal marketValue) {
        PositionRecord pos = new PositionRecord();
        pos.setPortfolioId(portfolioId);
        pos.setSymbolId(symbolId);
        pos.setPositionDate(LocalDate.now());
        pos.setQuantity(qty);
        pos.setCostBasis(costBasis);
        pos.setMarketValue(marketValue);
        pos.setCurrencyCode("USD");
        pos.setStatus("A");
        pos.setLastMaintDate(LocalDateTime.now());
        pos.setLastMaintUser("TEST    ");
        return pos;
    }
}
