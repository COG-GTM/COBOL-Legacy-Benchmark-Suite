package com.portfolio.batch.report;

import com.portfolio.domain.InvestmentPosition;
import com.portfolio.repository.InvestmentPositionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PositionReportService {
  private final InvestmentPositionRepository positionRepository;

  public PositionReportService(InvestmentPositionRepository positionRepository) {
    this.positionRepository = positionRepository;
  }

  public ReportResult generate() {
    return generate(LocalDate.now());
  }

  public ReportResult generate(LocalDate reportDate) {
    List<String> lines = new ArrayList<>();
    lines.add("DAILY POSITION REPORT|" + reportDate);
    for (InvestmentPosition position : positionRepository.findAll()) {
      BigDecimal currentValue = value(position);
      BigDecimal previousValue = previousValue(position, reportDate);
      BigDecimal changePercentage = changePercentage(currentValue, previousValue);
      lines.add(
          String.join(
              "|",
              position.getId().getPortfolioId(),
              position.getId().getInvestmentId(),
              decimal(position.getQuantity(), 4),
              decimal(currentValue, 2),
              decimal(changePercentage, 2)));
    }
    return new ReportResult("RPTPOS00", lines, 0);
  }

  public BigDecimal changePercentage(BigDecimal currentValue, BigDecimal previousValue) {
    if (previousValue == null || previousValue.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return currentValue
        .subtract(previousValue)
        .multiply(BigDecimal.valueOf(100))
        .divide(previousValue, 2, RoundingMode.HALF_UP);
  }

  private BigDecimal previousValue(InvestmentPosition position, LocalDate reportDate) {
    return positionRepository
        .findByIdPortfolioIdAndIdPositionDate(
            position.getId().getPortfolioId(), reportDate.minusDays(1))
        .stream()
        .filter(
            previous ->
                previous.getId().getInvestmentId().equals(position.getId().getInvestmentId()))
        .findFirst()
        .map(this::value)
        .orElse(BigDecimal.ZERO);
  }

  private BigDecimal value(InvestmentPosition position) {
    return position.getMarketValue() == null ? BigDecimal.ZERO : position.getMarketValue();
  }

  private String decimal(BigDecimal number, int scale) {
    return number.setScale(scale, RoundingMode.HALF_UP).toPlainString();
  }
}
