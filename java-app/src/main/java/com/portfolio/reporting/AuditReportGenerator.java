package com.portfolio.reporting;

import com.portfolio.model.AuditRecord;
import com.portfolio.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Audit Report Generator.
 * Replaces: RPTAUD00.cbl - Security and process audit trail reports.
 *
 * RPTAUD00 generates fixed-width audit reports showing:
 * - User access events
 * - Transaction events
 * - System events
 * - Summary counts by event type
 */
@Component
public class AuditReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(AuditReportGenerator.class);
    private static final String SEPARATOR = "=".repeat(120);
    private static final String LINE_SEP = "-".repeat(120);

    private final AuditRepository auditRepository;

    public AuditReportGenerator(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Generates the audit report for the current date.
     * Replaces: RPTAUD00.cbl main processing loop.
     */
    public String generateReport() {
        return generateReport(LocalDate.now());
    }

    /**
     * Generates the audit report for a specific date range.
     */
    public String generateReport(LocalDate date) {
        StringBuilder report = new StringBuilder();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<AuditRecord> records = auditRepository
                .findByAuditTimestampBetween(startOfDay, endOfDay);

        writeHeader(report, date);
        writeDetailSection(report, records);
        writeSummary(report, records);

        log.info("Audit report generated for {}: {} records", date, records.size());
        return report.toString();
    }

    private void writeHeader(StringBuilder report, LocalDate date) {
        report.append(SEPARATOR).append("\n");
        report.append(String.format("%-60s%60s%n",
                "AUDIT TRAIL REPORT",
                "Report Date: " + date.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        report.append(String.format("%-60s%60s%n",
                "",
                "Run Time: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        report.append(SEPARATOR).append("\n");
        report.append(String.format("%-20s %-10s %-15s %-10s %-10s %-50s%n",
                "Timestamp", "User", "Audit Type", "Action", "Status", "Message"));
        report.append(LINE_SEP).append("\n");
    }

    private void writeDetailSection(StringBuilder report, List<AuditRecord> records) {
        for (AuditRecord record : records) {
            report.append(String.format("%-20s %-10s %-15s %-10s %-10s %-50s%n",
                    record.getAuditTimestamp() != null
                            ? record.getAuditTimestamp()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            : "",
                    truncate(record.getUserId(), 10),
                    truncate(record.getAuditType(), 15),
                    truncate(record.getAction(), 10),
                    truncate(record.getStatus(), 10),
                    truncate(record.getMessage(), 50)));
        }
    }

    private void writeSummary(StringBuilder report, List<AuditRecord> records) {
        report.append("\n").append(LINE_SEP).append("\n");
        report.append("SUMMARY\n");

        long accessCount = records.stream()
                .filter(r -> "USER".equals(r.getAuditType()))
                .count();
        long transactionCount = records.stream()
                .filter(r -> "TRAN".equals(r.getAuditType()))
                .count();
        long systemCount = records.stream()
                .filter(r -> "SYST".equals(r.getAuditType()))
                .count();
        long successCount = records.stream()
                .filter(r -> "SUCC".equals(r.getStatus()))
                .count();
        long failureCount = records.stream()
                .filter(r -> "FAIL".equals(r.getStatus()))
                .count();

        report.append(String.format("  Access Events:      %d%n", accessCount));
        report.append(String.format("  Transaction Events: %d%n", transactionCount));
        report.append(String.format("  System Events:      %d%n", systemCount));
        report.append(String.format("  Successes:          %d%n", successCount));
        report.append(String.format("  Failures:           %d%n", failureCount));
        report.append(String.format("  Total Records:      %d%n", records.size()));
        report.append(SEPARATOR).append("\n");
        report.append("*** END OF REPORT ***\n");
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
