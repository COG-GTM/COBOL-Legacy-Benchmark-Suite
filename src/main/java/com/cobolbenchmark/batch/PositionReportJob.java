package com.cobolbenchmark.batch;

import com.cobolbenchmark.db.PositionRepository;
import com.cobolbenchmark.db.PortfolioMasterRepository;
import com.cobolbenchmark.model.PortfolioMaster;
import com.cobolbenchmark.model.PositionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Position Report Job - migrated from RPTPOS00.cbl.
 * Converts report generation with PIC edit masks to String.format().
 */
@Service
public class PositionReportJob {

    private static final Logger logger = LoggerFactory.getLogger(PositionReportJob.class);

    private final PositionRepository positionRepository;
    private final PortfolioMasterRepository portfolioMasterRepository;

    public PositionReportJob(PositionRepository positionRepository,
                             PortfolioMasterRepository portfolioMasterRepository) {
        this.positionRepository = positionRepository;
        this.portfolioMasterRepository = portfolioMasterRepository;
    }

    /**
     * Generate position report for a portfolio.
     * Converts PIC edit masks to String.format().
     */
    public String generateReport(String portfolioId) {
        logger.info("Generating position report for portfolio: {}", portfolioId);

        PortfolioMaster portfolio = portfolioMasterRepository.findById(portfolioId).orElse(null);
        List<PositionRecord> positions = positionRepository.findByPortfolioId(portfolioId);

        StringBuilder report = new StringBuilder();

        // Report header - replaces PIC edit mask header lines
        report.append(String.format("%-60s%n", "PORTFOLIO POSITION REPORT"));
        report.append(String.format("%-60s%n", "========================"));
        if (portfolio != null) {
            report.append(String.format("Portfolio: %-8s  Name: %-50s%n",
                    portfolio.getPortfolioId(), portfolio.getPortfolioName()));
            report.append(String.format("Status: %-1s  Currency: %-3s%n",
                    portfolio.getStatus(), portfolio.getCurrencyCode()));
        }
        report.append(String.format("%n"));

        // Column headers
        report.append(String.format("%-12s %-10s %-8s %15s %15s %15s %15s%n",
                "Investment", "Type", "Date", "Quantity", "Cost Basis", "Market Value", "Gain/Loss"));
        report.append(String.format("%-12s %-10s %-8s %15s %15s %15s %15s%n",
                "----------", "--------", "--------", "----------", "----------", "------------", "---------"));

        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalGainLoss = BigDecimal.ZERO;

        for (PositionRecord pos : positions) {
            BigDecimal gainLoss = pos.getMarketValue().subtract(pos.getCostBasis());

            // Convert PIC edit masks to String.format()
            report.append(String.format("%-12s %-10s %-8s %,15.4f %,15.2f %,15.2f %,15.2f%n",
                    pos.getInvestmentId(),
                    pos.getInvestmentType(),
                    pos.getPositionDate() != null ? pos.getPositionDate().toString() : "",
                    pos.getQuantity(),
                    pos.getCostBasis(),
                    pos.getMarketValue(),
                    gainLoss));

            totalCostBasis = totalCostBasis.add(pos.getCostBasis());
            totalMarketValue = totalMarketValue.add(pos.getMarketValue());
            totalGainLoss = totalGainLoss.add(gainLoss);
        }

        // Report totals
        report.append(String.format("%n"));
        report.append(String.format("%-33s %,15.2f %,15.2f %,15.2f%n",
                "TOTALS:", totalCostBasis, totalMarketValue, totalGainLoss));
        report.append(String.format("Total positions: %d%n", positions.size()));

        logger.info("Position report generated: {} positions", positions.size());
        return report.toString();
    }
}
