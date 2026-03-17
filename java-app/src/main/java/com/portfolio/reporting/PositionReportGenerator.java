package com.portfolio.reporting;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.Portfolio;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Position Report Generator.
 * Replaces: RPTPOS00.cbl - Portfolio valuation and summary reports.
 *
 * RPTPOS00 generates a fixed-width report with:
 * - Report header with run date/time
 * - Portfolio detail lines: Fund ID, Fund Name, Units, Cost Basis, Market Value
 * - Portfolio subtotals
 * - Grand totals
 */
@Component
public class PositionReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(PositionReportGenerator.class);
    private static final String SEPARATOR = "=".repeat(120);
    private static final String LINE_SEP = "-".repeat(120);

    private final PortfolioRepository portfolioRepository;
    private final InvestmentPositionRepository positionRepository;

    public PositionReportGenerator(PortfolioRepository portfolioRepository,
                                    InvestmentPositionRepository positionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Generates the position report.
     * Replaces: RPTPOS00.cbl 1000-INITIALIZE through 9000-FINALIZE.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();

        writeHeader(report);

        List<Portfolio> portfolios = portfolioRepository.findByStatus("A");
        BigDecimal grandTotalCost = BigDecimal.ZERO;
        BigDecimal grandTotalMarket = BigDecimal.ZERO;
        int portfolioCount = 0;
        int positionCount = 0;

        for (Portfolio portfolio : portfolios) {
            List<InvestmentPosition> positions =
                    positionRepository.findByKeyPortfolioId(portfolio.getPortfolioId());

            if (positions.isEmpty()) {
                continue;
            }

            writePortfolioHeader(report, portfolio);
            BigDecimal portfolioCost = BigDecimal.ZERO;
            BigDecimal portfolioMarket = BigDecimal.ZERO;

            for (InvestmentPosition pos : positions) {
                writePositionLine(report, pos);
                if (pos.getCostBasis() != null) {
                    portfolioCost = portfolioCost.add(pos.getCostBasis());
                }
                if (pos.getMarketValue() != null) {
                    portfolioMarket = portfolioMarket.add(pos.getMarketValue());
                }
                positionCount++;
            }

            writePortfolioSubtotal(report, portfolioCost, portfolioMarket);
            grandTotalCost = grandTotalCost.add(portfolioCost);
            grandTotalMarket = grandTotalMarket.add(portfolioMarket);
            portfolioCount++;
        }

        writeGrandTotal(report, grandTotalCost, grandTotalMarket,
                portfolioCount, positionCount);

        log.info("Position report generated: {} portfolios, {} positions",
                portfolioCount, positionCount);
        return report.toString();
    }

    private void writeHeader(StringBuilder report) {
        report.append(SEPARATOR).append("\n");
        report.append(String.format("%-60s%60s%n",
                "INVESTMENT PORTFOLIO POSITION REPORT",
                "Run Date: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        report.append(SEPARATOR).append("\n");
        report.append(String.format("%-10s %-30s %15s %18s %18s%n",
                "Fund ID", "Fund Name", "Units", "Cost Basis", "Market Value"));
        report.append(LINE_SEP).append("\n");
    }

    private void writePortfolioHeader(StringBuilder report, Portfolio portfolio) {
        report.append(String.format("%nPortfolio: %-8s  %-50s  Status: %s%n",
                portfolio.getPortfolioId(),
                portfolio.getPortfolioName() != null ? portfolio.getPortfolioName() : "",
                portfolio.getStatus()));
        report.append(LINE_SEP).append("\n");
    }

    private void writePositionLine(StringBuilder report, InvestmentPosition pos) {
        report.append(String.format("  %-10s %-30s %15s %18s %18s%n",
                pos.getKey().getInvestmentId(),
                pos.getInvestmentName() != null ? pos.getInvestmentName() : "",
                pos.getQuantity() != null ? pos.getQuantity().toPlainString() : "0",
                pos.getCostBasis() != null ? pos.getCostBasis().toPlainString() : "0.00",
                pos.getMarketValue() != null ? pos.getMarketValue().toPlainString() : "0.00"));
    }

    private void writePortfolioSubtotal(StringBuilder report,
                                         BigDecimal totalCost, BigDecimal totalMarket) {
        report.append(String.format("  %42s %18s %18s%n",
                "Portfolio Subtotal:", totalCost.toPlainString(), totalMarket.toPlainString()));
    }

    private void writeGrandTotal(StringBuilder report,
                                  BigDecimal totalCost, BigDecimal totalMarket,
                                  int portfolioCount, int positionCount) {
        report.append("\n").append(SEPARATOR).append("\n");
        report.append(String.format("  GRAND TOTAL: %28s %18s %18s%n",
                "", totalCost.toPlainString(), totalMarket.toPlainString()));
        report.append(String.format("  Portfolios: %d   Positions: %d%n",
                portfolioCount, positionCount));
        report.append(SEPARATOR).append("\n");
        report.append("*** END OF REPORT ***\n");
    }
}
