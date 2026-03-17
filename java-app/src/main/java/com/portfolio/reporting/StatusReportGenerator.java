package com.portfolio.reporting;

import com.portfolio.model.BatchControlRecord;
import com.portfolio.repository.BatchControlRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import com.portfolio.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Status Report Generator.
 * Replaces: RPTSTA00.cbl - System performance and statistics reports.
 *
 * RPTSTA00 generates a system status report showing:
 * - Batch job status summary
 * - System metrics (portfolio/transaction counts)
 * - Performance statistics
 */
@Component
public class StatusReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(StatusReportGenerator.class);
    private static final String SEPARATOR = "=".repeat(120);
    private static final String LINE_SEP = "-".repeat(120);

    private final BatchControlRepository batchControlRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final MonitoringService monitoringService;

    public StatusReportGenerator(BatchControlRepository batchControlRepository,
                                  PortfolioRepository portfolioRepository,
                                  TransactionHistoryRepository transactionRepository,
                                  MonitoringService monitoringService) {
        this.batchControlRepository = batchControlRepository;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.monitoringService = monitoringService;
    }

    /**
     * Generates the status report.
     * Replaces: RPTSTA00.cbl main processing.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();

        writeHeader(report);
        writeSystemMetrics(report);
        writeBatchJobStatus(report);
        writeHealthStatus(report);
        writeFooter(report);

        log.info("Status report generated");
        return report.toString();
    }

    private void writeHeader(StringBuilder report) {
        report.append(SEPARATOR).append("\n");
        report.append(String.format("%-60s%60s%n",
                "SYSTEM STATUS REPORT",
                "Run Date: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        report.append(SEPARATOR).append("\n");
    }

    private void writeSystemMetrics(StringBuilder report) {
        report.append("\nSYSTEM METRICS\n");
        report.append(LINE_SEP).append("\n");

        Map<String, Object> metrics = monitoringService.getSystemMetrics();
        report.append(String.format("  Total Portfolios:      %s%n",
                metrics.get("totalPortfolios")));
        report.append(String.format("  Active Portfolios:     %s%n",
                metrics.get("activePortfolios")));
        report.append(String.format("  Total Transactions:    %s%n",
                metrics.get("totalTransactions")));
    }

    private void writeBatchJobStatus(StringBuilder report) {
        report.append("\nBATCH JOB STATUS\n");
        report.append(LINE_SEP).append("\n");
        report.append(String.format("%-10s %-10s %-5s %-8s %-8s %-8s %-4s %-50s%n",
                "Job Name", "Date", "Seq", "Status", "Start", "End", "RC", "Error"));
        report.append(LINE_SEP).append("\n");

        List<BatchControlRecord> records = batchControlRepository.findAll();
        for (BatchControlRecord record : records) {
            report.append(String.format("%-10s %-10s %-5d %-8s %-8s %-8s %-4d %-50s%n",
                    record.getKey().getJobName(),
                    record.getKey().getProcessDate(),
                    record.getKey().getSequenceNo(),
                    record.getStatus() != null ? record.getStatus() : "",
                    record.getStartTime() != null ? record.getStartTime() : "",
                    record.getEndTime() != null ? record.getEndTime() : "",
                    record.getReturnCode(),
                    record.getErrorDesc() != null ? record.getErrorDesc() : ""));
        }

        report.append(String.format("%n  Total batch records: %d%n", records.size()));
    }

    private void writeHealthStatus(StringBuilder report) {
        report.append("\nSYSTEM HEALTH\n");
        report.append(LINE_SEP).append("\n");

        MonitoringService.HealthStatus health = monitoringService.checkHealth();
        report.append(String.format("  Status:  %s%n", health.status()));
        report.append(String.format("  Message: %s%n", health.message()));
    }

    private void writeFooter(StringBuilder report) {
        report.append("\n").append(SEPARATOR).append("\n");
        report.append("*** END OF REPORT ***\n");
    }
}
