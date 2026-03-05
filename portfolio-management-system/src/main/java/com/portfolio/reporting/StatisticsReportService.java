package com.portfolio.reporting;

import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.TransactionRecordRepository;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.ErrorRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Statistics Report Service.
 * Migrated from COBOL RPTSTA00.
 * System performance metrics from TRANSACTION_HISTORY DB2 table.
 * Read-only: does not modify any data.
 */
@Service
public class StatisticsReportService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsReportService.class);

    private final TransactionRecordRepository transactionRepository;
    private final PositionRecordRepository positionRepository;
    private final ErrorRecordRepository errorRecordRepository;
    private final Db2StatisticsService statisticsService;

    public StatisticsReportService(
            TransactionRecordRepository transactionRepository,
            PositionRecordRepository positionRepository,
            ErrorRecordRepository errorRecordRepository,
            Db2StatisticsService statisticsService) {
        this.transactionRepository = transactionRepository;
        this.positionRepository = positionRepository;
        this.errorRecordRepository = errorRecordRepository;
        this.statisticsService = statisticsService;
    }

    /**
     * Generate system performance statistics report.
     * Replaces COBOL RPTSTA00 statistics generation.
     */
    public Map<String, Object> generateStatisticsReport() {
        log.info("Generating statistics report (RPTSTA00)");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportDate", LocalDate.now().toString());
        report.put("reportType", "System Performance Statistics");

        // Transaction statistics
        long totalTransactions = transactionRepository.count();
        long pendingTransactions = transactionRepository.countByStatus("P");
        long completedTransactions = transactionRepository.countByStatus("D");
        long failedTransactions = transactionRepository.countByStatus("F");

        report.put("totalTransactions", totalTransactions);
        report.put("pendingTransactions", pendingTransactions);
        report.put("completedTransactions", completedTransactions);
        report.put("failedTransactions", failedTransactions);

        // Position statistics
        long totalPositions = positionRepository.count();
        report.put("totalPositions", totalPositions);

        // Error statistics
        long totalErrors = errorRecordRepository.count();
        long todayErrors = errorRecordRepository.countByProcessDate(LocalDate.now());
        report.put("totalErrors", totalErrors);
        report.put("todayErrors", todayErrors);

        // DB2 operation metrics
        report.put("db2QueryCount", statisticsService.getQueryCount());
        report.put("db2InsertCount", statisticsService.getInsertCount());
        report.put("db2UpdateCount", statisticsService.getUpdateCount());
        report.put("db2ErrorCount", statisticsService.getErrorCount());

        statisticsService.recordQuery();

        log.info("Statistics report generated: {} transactions, {} positions, {} errors",
                totalTransactions, totalPositions, totalErrors);
        return report;
    }
}
