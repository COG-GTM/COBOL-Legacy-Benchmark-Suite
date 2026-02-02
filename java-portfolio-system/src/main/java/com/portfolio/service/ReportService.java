package com.portfolio.service;

import com.portfolio.domain.*;
import com.portfolio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report Service - migrated from COBOL RPTPOS00, RPTAUD00, RPTSTA00
 * Generates position, audit, and statistics reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BatchControlRepository batchControlRepository;

    /**
     * Generate Position Report - migrated from RPTPOS00
     */
    public PositionReport generatePositionReport(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        if (portfolio == null) {
            return null;
        }

        List<Position> positions = positionRepository.findActivePositionsByPortfolio(portfolioId);
        
        PositionReport report = new PositionReport();
        report.setReportDate(LocalDate.now());
        report.setPortfolioId(portfolioId);
        report.setAccountNo(portfolio.getAccountNo());
        report.setClientName(portfolio.getClientName());
        report.setStatus(portfolio.getStatus().name());
        
        List<PositionReport.PositionLine> lines = new ArrayList<>();
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        
        for (Position position : positions) {
            PositionReport.PositionLine line = new PositionReport.PositionLine();
            line.setInvestmentId(position.getInvestmentId());
            line.setQuantity(position.getQuantity());
            line.setCostBasis(position.getCostBasis());
            line.setMarketValue(position.getMarketValue());
            line.setCurrency(position.getCurrency());
            line.setGainLoss(position.getMarketValue().subtract(position.getCostBasis()));
            lines.add(line);
            
            totalMarketValue = totalMarketValue.add(position.getMarketValue());
            totalCostBasis = totalCostBasis.add(position.getCostBasis());
        }
        
        report.setPositions(lines);
        report.setTotalMarketValue(totalMarketValue);
        report.setTotalCostBasis(totalCostBasis);
        report.setTotalGainLoss(totalMarketValue.subtract(totalCostBasis));
        report.setCashBalance(portfolio.getCashBalance());
        report.setTotalPortfolioValue(totalMarketValue.add(portfolio.getCashBalance()));
        
        log.info("Position report generated for portfolio: {}", portfolioId);
        return report;
    }

    /**
     * Generate Audit Report - migrated from RPTAUD00
     */
    public AuditReport generateAuditReport(LocalDateTime startTime, LocalDateTime endTime) {
        List<AuditLog> audits = auditLogRepository.findByTimestampBetween(startTime, endTime);
        
        AuditReport report = new AuditReport();
        report.setReportDate(LocalDate.now());
        report.setStartTime(startTime);
        report.setEndTime(endTime);
        
        List<AuditReport.AuditLine> lines = audits.stream()
                .map(audit -> {
                    AuditReport.AuditLine line = new AuditReport.AuditLine();
                    line.setTimestamp(audit.getTimestamp());
                    line.setUserId(audit.getUserId());
                    line.setProgram(audit.getProgram());
                    line.setAction(audit.getAction().name());
                    line.setStatus(audit.getStatus().name());
                    line.setPortfolioId(audit.getPortfolioId());
                    line.setMessage(audit.getMessage());
                    return line;
                })
                .collect(Collectors.toList());
        
        report.setAuditEntries(lines);
        report.setTotalEntries(lines.size());
        
        Map<String, Long> actionCounts = audits.stream()
                .collect(Collectors.groupingBy(a -> a.getAction().name(), Collectors.counting()));
        report.setActionCounts(actionCounts);
        
        long successCount = audits.stream()
                .filter(a -> a.getStatus() == AuditLog.AuditStatus.SUCC)
                .count();
        long failCount = audits.stream()
                .filter(a -> a.getStatus() == AuditLog.AuditStatus.FAIL)
                .count();
        
        report.setSuccessCount((int) successCount);
        report.setFailureCount((int) failCount);
        
        log.info("Audit report generated: {} entries", lines.size());
        return report;
    }

    /**
     * Generate Statistics Report - migrated from RPTSTA00
     */
    public StatisticsReport generateStatisticsReport(LocalDate reportDate) {
        StatisticsReport report = new StatisticsReport();
        report.setReportDate(reportDate);
        
        List<Portfolio> allPortfolios = portfolioRepository.findAll();
        List<Portfolio> activePortfolios = portfolioRepository.findByStatus(Portfolio.PortfolioStatus.A);
        
        report.setTotalPortfolios(allPortfolios.size());
        report.setActivePortfolios(activePortfolios.size());
        
        BigDecimal totalAUM = activePortfolios.stream()
                .map(Portfolio::getTotalValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalAssetsUnderManagement(totalAUM);
        
        List<Transaction> todayTransactions = transactionRepository
                .findByTransactionDateBetween(reportDate, reportDate);
        report.setTransactionsToday(todayTransactions.size());
        
        long buyCount = todayTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.BU)
                .count();
        long sellCount = todayTransactions.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.SL)
                .count();
        
        report.setBuyTransactions((int) buyCount);
        report.setSellTransactions((int) sellCount);
        
        BigDecimal totalVolume = todayTransactions.stream()
                .map(Transaction::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalTransactionVolume(totalVolume);
        
        List<BatchControl> completedJobs = batchControlRepository.findCompletedJobsForDate(reportDate);
        report.setBatchJobsCompleted(completedJobs.size());
        
        List<BatchControl> failedJobs = batchControlRepository.findFailedJobs();
        report.setBatchJobsFailed(failedJobs.size());
        
        Map<String, Integer> portfoliosByType = new HashMap<>();
        portfoliosByType.put("Individual", (int) activePortfolios.stream()
                .filter(p -> p.getClientType() == Portfolio.ClientType.I).count());
        portfoliosByType.put("Corporate", (int) activePortfolios.stream()
                .filter(p -> p.getClientType() == Portfolio.ClientType.C).count());
        portfoliosByType.put("Trust", (int) activePortfolios.stream()
                .filter(p -> p.getClientType() == Portfolio.ClientType.T).count());
        report.setPortfoliosByType(portfoliosByType);
        
        log.info("Statistics report generated for date: {}", reportDate);
        return report;
    }

    public static class PositionReport {
        private LocalDate reportDate;
        private String portfolioId;
        private String accountNo;
        private String clientName;
        private String status;
        private List<PositionLine> positions;
        private BigDecimal totalMarketValue;
        private BigDecimal totalCostBasis;
        private BigDecimal totalGainLoss;
        private BigDecimal cashBalance;
        private BigDecimal totalPortfolioValue;

        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<PositionLine> getPositions() { return positions; }
        public void setPositions(List<PositionLine> positions) { this.positions = positions; }
        public BigDecimal getTotalMarketValue() { return totalMarketValue; }
        public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }
        public BigDecimal getTotalCostBasis() { return totalCostBasis; }
        public void setTotalCostBasis(BigDecimal totalCostBasis) { this.totalCostBasis = totalCostBasis; }
        public BigDecimal getTotalGainLoss() { return totalGainLoss; }
        public void setTotalGainLoss(BigDecimal totalGainLoss) { this.totalGainLoss = totalGainLoss; }
        public BigDecimal getCashBalance() { return cashBalance; }
        public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
        public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
        public void setTotalPortfolioValue(BigDecimal totalPortfolioValue) { this.totalPortfolioValue = totalPortfolioValue; }

        public static class PositionLine {
            private String investmentId;
            private BigDecimal quantity;
            private BigDecimal costBasis;
            private BigDecimal marketValue;
            private String currency;
            private BigDecimal gainLoss;

            public String getInvestmentId() { return investmentId; }
            public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
            public BigDecimal getQuantity() { return quantity; }
            public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
            public BigDecimal getCostBasis() { return costBasis; }
            public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
            public BigDecimal getMarketValue() { return marketValue; }
            public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
            public BigDecimal getGainLoss() { return gainLoss; }
            public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
        }
    }

    public static class AuditReport {
        private LocalDate reportDate;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<AuditLine> auditEntries;
        private int totalEntries;
        private int successCount;
        private int failureCount;
        private Map<String, Long> actionCounts;

        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public List<AuditLine> getAuditEntries() { return auditEntries; }
        public void setAuditEntries(List<AuditLine> auditEntries) { this.auditEntries = auditEntries; }
        public int getTotalEntries() { return totalEntries; }
        public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public Map<String, Long> getActionCounts() { return actionCounts; }
        public void setActionCounts(Map<String, Long> actionCounts) { this.actionCounts = actionCounts; }

        public static class AuditLine {
            private LocalDateTime timestamp;
            private String userId;
            private String program;
            private String action;
            private String status;
            private String portfolioId;
            private String message;

            public LocalDateTime getTimestamp() { return timestamp; }
            public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
            public String getUserId() { return userId; }
            public void setUserId(String userId) { this.userId = userId; }
            public String getProgram() { return program; }
            public void setProgram(String program) { this.program = program; }
            public String getAction() { return action; }
            public void setAction(String action) { this.action = action; }
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
            public String getPortfolioId() { return portfolioId; }
            public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
        }
    }

    public static class StatisticsReport {
        private LocalDate reportDate;
        private int totalPortfolios;
        private int activePortfolios;
        private BigDecimal totalAssetsUnderManagement;
        private int transactionsToday;
        private int buyTransactions;
        private int sellTransactions;
        private BigDecimal totalTransactionVolume;
        private int batchJobsCompleted;
        private int batchJobsFailed;
        private Map<String, Integer> portfoliosByType;

        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public int getTotalPortfolios() { return totalPortfolios; }
        public void setTotalPortfolios(int totalPortfolios) { this.totalPortfolios = totalPortfolios; }
        public int getActivePortfolios() { return activePortfolios; }
        public void setActivePortfolios(int activePortfolios) { this.activePortfolios = activePortfolios; }
        public BigDecimal getTotalAssetsUnderManagement() { return totalAssetsUnderManagement; }
        public void setTotalAssetsUnderManagement(BigDecimal totalAssetsUnderManagement) { this.totalAssetsUnderManagement = totalAssetsUnderManagement; }
        public int getTransactionsToday() { return transactionsToday; }
        public void setTransactionsToday(int transactionsToday) { this.transactionsToday = transactionsToday; }
        public int getBuyTransactions() { return buyTransactions; }
        public void setBuyTransactions(int buyTransactions) { this.buyTransactions = buyTransactions; }
        public int getSellTransactions() { return sellTransactions; }
        public void setSellTransactions(int sellTransactions) { this.sellTransactions = sellTransactions; }
        public BigDecimal getTotalTransactionVolume() { return totalTransactionVolume; }
        public void setTotalTransactionVolume(BigDecimal totalTransactionVolume) { this.totalTransactionVolume = totalTransactionVolume; }
        public int getBatchJobsCompleted() { return batchJobsCompleted; }
        public void setBatchJobsCompleted(int batchJobsCompleted) { this.batchJobsCompleted = batchJobsCompleted; }
        public int getBatchJobsFailed() { return batchJobsFailed; }
        public void setBatchJobsFailed(int batchJobsFailed) { this.batchJobsFailed = batchJobsFailed; }
        public Map<String, Integer> getPortfoliosByType() { return portfoliosByType; }
        public void setPortfoliosByType(Map<String, Integer> portfoliosByType) { this.portfoliosByType = portfoliosByType; }
    }
}
