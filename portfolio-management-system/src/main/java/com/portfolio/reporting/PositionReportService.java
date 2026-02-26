package com.portfolio.reporting;

import com.portfolio.model.PositionRecord;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.PositionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Position Report Service.
 * Migrated from COBOL RPTPOS00.
 * Generates daily position reports and portfolio valuations.
 * Read-only: does not modify any data.
 * Output: 132-character equivalent line width CSV/report format.
 */
@Service
public class PositionReportService {

    private static final Logger log = LoggerFactory.getLogger(PositionReportService.class);

    private final PositionRecordRepository positionRepository;
    private final Db2StatisticsService statisticsService;

    public PositionReportService(PositionRecordRepository positionRepository,
                                  Db2StatisticsService statisticsService) {
        this.positionRepository = positionRepository;
        this.statisticsService = statisticsService;
    }

    /**
     * Generate daily position report.
     * Replaces COBOL RPTPOS00 report generation.
     */
    public List<PositionReportLine> generateDailyReport() {
        log.info("Generating daily position report (RPTPOS00)");

        List<PositionRecord> positions = positionRepository.findAll();
        statisticsService.recordQuery();

        List<PositionReportLine> report = new ArrayList<>();

        // Group by portfolio
        Map<String, List<PositionRecord>> byPortfolio = positions.stream()
                .collect(Collectors.groupingBy(PositionRecord::getPortfolioId));

        for (Map.Entry<String, List<PositionRecord>> entry : byPortfolio.entrySet()) {
            String portfolioId = entry.getKey();
            List<PositionRecord> portfolioPositions = entry.getValue();

            BigDecimal totalCostBasis = BigDecimal.ZERO;
            BigDecimal totalMarketValue = BigDecimal.ZERO;

            for (PositionRecord pos : portfolioPositions) {
                PositionReportLine line = new PositionReportLine();
                line.setReportDate(LocalDate.now());
                line.setPortfolioId(portfolioId);
                line.setSymbolId(pos.getSymbolId());
                line.setQuantity(pos.getQuantity());
                line.setCostBasis(pos.getCostBasis());
                line.setMarketValue(pos.getMarketValue());
                line.setGainLoss(pos.getMarketValue().subtract(pos.getCostBasis()));
                line.setStatus(pos.getStatus());
                report.add(line);

                totalCostBasis = totalCostBasis.add(pos.getCostBasis());
                totalMarketValue = totalMarketValue.add(pos.getMarketValue());
            }

            // Summary line
            PositionReportLine summary = new PositionReportLine();
            summary.setReportDate(LocalDate.now());
            summary.setPortfolioId(portfolioId);
            summary.setSymbolId("**TOTAL**");
            summary.setCostBasis(totalCostBasis);
            summary.setMarketValue(totalMarketValue);
            summary.setGainLoss(totalMarketValue.subtract(totalCostBasis));
            summary.setQuantity(BigDecimal.ZERO);
            summary.setStatus("S");
            report.add(summary);
        }

        log.info("Position report generated: {} lines for {} portfolios",
                report.size(), byPortfolio.size());
        return report;
    }

    /**
     * Position report line item.
     */
    public static class PositionReportLine {
        private LocalDate reportDate;
        private String portfolioId;
        private String symbolId;
        private BigDecimal quantity;
        private BigDecimal costBasis;
        private BigDecimal marketValue;
        private BigDecimal gainLoss;
        private String status;

        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
        public String getSymbolId() { return symbolId; }
        public void setSymbolId(String symbolId) { this.symbolId = symbolId; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getCostBasis() { return costBasis; }
        public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
        public BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
        public BigDecimal getGainLoss() { return gainLoss; }
        public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
