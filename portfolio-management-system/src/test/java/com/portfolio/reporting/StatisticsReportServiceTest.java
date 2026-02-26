package com.portfolio.reporting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StatisticsReportService.
 * Validates statistics report generation from TRANSACTION_HISTORY DB2 table.
 */
@SpringBootTest
@ActiveProfiles("test")
class StatisticsReportServiceTest {

    @Autowired
    private StatisticsReportService reportService;

    @Test
    void testGenerateStatisticsReport() {
        Map<String, Object> report = reportService.generateStatisticsReport();

        assertThat(report).containsKey("reportDate");
        assertThat(report).containsKey("reportType");
        assertThat(report).containsKey("totalTransactions");
        assertThat(report).containsKey("totalPositions");
        assertThat(report).containsKey("totalErrors");
        assertThat(report.get("reportType")).isEqualTo("System Performance Statistics");
    }
}
