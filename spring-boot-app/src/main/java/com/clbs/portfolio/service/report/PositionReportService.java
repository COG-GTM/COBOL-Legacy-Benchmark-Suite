package com.clbs.portfolio.service.report;

import com.clbs.portfolio.config.ReportConfig;
import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.EntityStatus;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PositionReportService {

    private static final Logger log = LoggerFactory.getLogger(PositionReportService.class);
    private static final int LINE_WIDTH = 132;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PositionRepository positionRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ReportConfig reportConfig;

    public PositionReportService(PositionRepository positionRepository,
                                  TransactionRecordRepository transactionRecordRepository,
                                  ReportConfig reportConfig) {
        this.positionRepository = positionRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.reportConfig = reportConfig;
    }

    public String generateReport(LocalDate reportDate, String format) {
        List<Position> activePositions = positionRepository.findByStatus(EntityStatus.ACTIVE);
        LocalDate periodStart = reportDate.minusDays(1);
        List<TransactionRecord> transactions = transactionRecordRepository
                .findByTransactionDateBetween(periodStart, reportDate);

        if ("csv".equalsIgnoreCase(format)) {
            return generateCsvReport(reportDate, activePositions, transactions);
        }
        return generateTextReport(reportDate, activePositions, transactions);
    }

    public String generateCsvReport(LocalDate reportDate, List<Position> positions,
                                     List<TransactionRecord> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("DAILY POSITION REPORT - ").append(reportDate.format(DATE_FMT)).append("\n\n");

        // Position summary
        sb.append("Portfolio ID,Investment ID,Quantity,Cost Basis,Market Value,Gain/Loss,Change %\n");
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;

        for (Position pos : positions) {
            BigDecimal gainLoss = pos.getMarketValue().subtract(pos.getCostBasis());
            BigDecimal changePct = BigDecimal.ZERO;
            if (pos.getCostBasis().compareTo(BigDecimal.ZERO) != 0) {
                changePct = gainLoss.multiply(BigDecimal.valueOf(100))
                        .divide(pos.getCostBasis(), 2, RoundingMode.HALF_UP);
            }
            sb.append(pos.getPortfolioId()).append(",")
              .append(pos.getInvestmentId()).append(",")
              .append(pos.getQuantity()).append(",")
              .append(pos.getCostBasis()).append(",")
              .append(pos.getMarketValue()).append(",")
              .append(gainLoss).append(",")
              .append(changePct).append("\n");

            totalMarketValue = totalMarketValue.add(pos.getMarketValue());
            totalCostBasis = totalCostBasis.add(pos.getCostBasis());
        }

        // Transaction activity
        sb.append("\nTRANSACTION ACTIVITY\n");
        sb.append("Date,Portfolio ID,Type,Investment ID,Quantity,Price,Amount\n");
        for (TransactionRecord trn : transactions) {
            sb.append(trn.getTransactionDate()).append(",")
              .append(trn.getPortfolioId()).append(",")
              .append(trn.getTransactionType()).append(",")
              .append(trn.getInvestmentId()).append(",")
              .append(trn.getQuantity()).append(",")
              .append(trn.getPrice()).append(",")
              .append(trn.getAmount()).append("\n");
        }

        // Summary
        BigDecimal totalGainLoss = totalMarketValue.subtract(totalCostBasis);
        sb.append("\nSUMMARY\n");
        sb.append("Total Positions,").append(positions.size()).append("\n");
        sb.append("Total Market Value,").append(totalMarketValue).append("\n");
        sb.append("Total Cost Basis,").append(totalCostBasis).append("\n");
        sb.append("Total Gain/Loss,").append(totalGainLoss).append("\n");
        sb.append("Total Transactions,").append(transactions.size()).append("\n");

        writeToFile(reportDate, "csv", sb.toString());
        return sb.toString();
    }

    public String generateTextReport(LocalDate reportDate, List<Position> positions,
                                      List<TransactionRecord> transactions) {
        StringBuilder sb = new StringBuilder();

        // Header (matching RPTPOS00.cbl format)
        sb.append(repeat('*', LINE_WIDTH)).append("\n");
        sb.append(center("DAILY POSITION REPORT", LINE_WIDTH)).append("\n");
        sb.append(String.format("%-15s%-117s", "REPORT DATE: " + reportDate.format(DATE_FMT), "")).append("\n");
        sb.append(repeat('*', LINE_WIDTH)).append("\n\n");

        // Position detail section
        sb.append(center("PORTFOLIO POSITION SUMMARY", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        sb.append(String.format("%-10s  %-10s  %16s  %18s  %18s  %9s",
                "PORT ID", "INVEST ID", "QUANTITY", "COST BASIS", "MARKET VALUE", "CHG %")).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;

        Map<String, List<Position>> byPortfolio = positions.stream()
                .collect(Collectors.groupingBy(Position::getPortfolioId, LinkedHashMap::new, Collectors.toList()));

        Set<String> portfoliosWithTransactions = transactions.stream()
                .map(TransactionRecord::getPortfolioId)
                .collect(Collectors.toSet());

        List<Position> exceptions = new ArrayList<>();

        for (Map.Entry<String, List<Position>> entry : byPortfolio.entrySet()) {
            for (Position pos : entry.getValue()) {
                BigDecimal gainLoss = pos.getMarketValue().subtract(pos.getCostBasis());
                BigDecimal changePct = BigDecimal.ZERO;
                if (pos.getCostBasis().compareTo(BigDecimal.ZERO) != 0) {
                    changePct = gainLoss.multiply(BigDecimal.valueOf(100))
                            .divide(pos.getCostBasis(), 2, RoundingMode.HALF_UP);
                }

                sb.append(String.format("%-10s  %-10s  %,16.2f  %,18.2f  %,18.2f  %+8.2f%%",
                        pos.getPortfolioId(), pos.getInvestmentId(),
                        pos.getQuantity(), pos.getCostBasis(),
                        pos.getMarketValue(), changePct)).append("\n");

                totalMarketValue = totalMarketValue.add(pos.getMarketValue());
                totalCostBasis = totalCostBasis.add(pos.getCostBasis());

                if (!portfoliosWithTransactions.contains(pos.getPortfolioId())) {
                    exceptions.add(pos);
                }
                if (changePct.abs().compareTo(BigDecimal.TEN) > 0) {
                    exceptions.add(pos);
                }
            }
        }

        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        // Transaction activity section
        sb.append("\n");
        sb.append(center("TRANSACTION ACTIVITY", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        Map<String, List<TransactionRecord>> trnByPortfolio = transactions.stream()
                .collect(Collectors.groupingBy(TransactionRecord::getPortfolioId,
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<TransactionRecord>> entry : trnByPortfolio.entrySet()) {
            sb.append("  Portfolio: ").append(entry.getKey()).append("\n");
            for (TransactionRecord trn : entry.getValue()) {
                sb.append(String.format("    %-10s  %-8s  %-10s  %,16.4f  %,16.4f  %,18.2f",
                        trn.getTransactionDate(), trn.getTransactionType(),
                        trn.getInvestmentId(),
                        trn.getQuantity(), trn.getPrice(), trn.getAmount())).append("\n");
            }
        }

        // Exception report
        sb.append("\n");
        sb.append(center("EXCEPTION REPORT", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        if (exceptions.isEmpty()) {
            sb.append("  No exceptions found.\n");
        } else {
            Set<String> reported = new HashSet<>();
            for (Position pos : exceptions) {
                String key = pos.getPortfolioId() + "-" + pos.getInvestmentId();
                if (reported.add(key)) {
                    if (!portfoliosWithTransactions.contains(pos.getPortfolioId())) {
                        sb.append(String.format("  %-10s  %-10s  NO ACTIVITY IN PERIOD%n",
                                pos.getPortfolioId(), pos.getInvestmentId()));
                    }
                    BigDecimal changePct = BigDecimal.ZERO;
                    if (pos.getCostBasis().compareTo(BigDecimal.ZERO) != 0) {
                        changePct = pos.getMarketValue().subtract(pos.getCostBasis())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(pos.getCostBasis(), 2, RoundingMode.HALF_UP);
                    }
                    if (changePct.abs().compareTo(BigDecimal.TEN) > 0) {
                        sb.append(String.format("  %-10s  %-10s  LARGE VALUE CHANGE: %+.2f%%%n",
                                pos.getPortfolioId(), pos.getInvestmentId(), changePct));
                    }
                }
            }
        }

        // Performance metrics
        sb.append("\n");
        sb.append(center("PERFORMANCE METRICS", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        BigDecimal totalGainLoss = totalMarketValue.subtract(totalCostBasis);
        BigDecimal overallChangePct = BigDecimal.ZERO;
        if (totalCostBasis.compareTo(BigDecimal.ZERO) != 0) {
            overallChangePct = totalGainLoss.multiply(BigDecimal.valueOf(100))
                    .divide(totalCostBasis, 2, RoundingMode.HALF_UP);
        }
        sb.append(String.format("  Total Gain/Loss:          %,18.2f%n", totalGainLoss));
        sb.append(String.format("  Overall Change:           %+.2f%%%n", overallChangePct));

        // Summary totals
        sb.append("\n");
        sb.append(center("SUMMARY TOTALS", LINE_WIDTH)).append("\n");
        sb.append(repeat('=', LINE_WIDTH)).append("\n");
        sb.append(String.format("  Total Positions:          %,d%n", positions.size()));
        sb.append(String.format("  Total Portfolios:         %,d%n", byPortfolio.size()));
        sb.append(String.format("  Total Market Value:       %,18.2f%n", totalMarketValue));
        sb.append(String.format("  Total Cost Basis:         %,18.2f%n", totalCostBasis));
        sb.append(String.format("  Total Transactions:       %,d%n", transactions.size()));
        sb.append(repeat('*', LINE_WIDTH)).append("\n");

        writeToFile(reportDate, "txt", sb.toString());
        return sb.toString();
    }

    private void writeToFile(LocalDate reportDate, String extension, String content) {
        try {
            Path outputPath = Paths.get(reportConfig.getOutputDirectory(),
                    "position_report_" + reportDate.format(DATE_FMT) + "." + extension);
            Files.writeString(outputPath, content);
            log.info("Position report written to {}", outputPath);
        } catch (IOException e) {
            log.error("Failed to write position report file", e);
        }
    }

    private String repeat(char c, int count) {
        return String.valueOf(c).repeat(count);
    }

    private String center(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text +
               " ".repeat(Math.max(0, width - padding - text.length()));
    }
}
