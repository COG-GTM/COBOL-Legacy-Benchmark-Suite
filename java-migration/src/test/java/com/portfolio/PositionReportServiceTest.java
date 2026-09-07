package com.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.portfolio.batch.report.PositionReportService;
import com.portfolio.repository.InvestmentPositionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositionReportServiceTest {
  private final PositionReportService reportService =
      new PositionReportService(org.mockito.Mockito.mock(InvestmentPositionRepository.class));

  @Test
  void previousValueZeroProducesZeroPercentage() {
    assertEquals(
        new BigDecimal("0.00"),
        reportService.changePercentage(new BigDecimal("110"), BigDecimal.ZERO));
  }

  @Test
  void previousValueOneHundredAndCurrentOneHundredTenProducesTenPercent() {
    assertEquals(
        new BigDecimal("10.00"),
        reportService.changePercentage(new BigDecimal("110"), new BigDecimal("100")));
  }
}
