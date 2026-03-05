package com.portfolio.reporting;

import com.portfolio.model.ErrorRecord;
import com.portfolio.model.SecurityLogRecord;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.ErrorRecordRepository;
import com.portfolio.support.SecurityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit Report Service.
 * Migrated from COBOL RPTAUD00.
 * Produces security audit reports from AUDITLOG + ERRLOG DB2 tables.
 * Read-only: does not modify any data.
 */
@Service
public class AuditReportService {

    private static final Logger log = LoggerFactory.getLogger(AuditReportService.class);

    private final SecurityLogRepository securityLogRepository;
    private final ErrorRecordRepository errorRecordRepository;
    private final Db2StatisticsService statisticsService;

    public AuditReportService(SecurityLogRepository securityLogRepository,
                               ErrorRecordRepository errorRecordRepository,
                               Db2StatisticsService statisticsService) {
        this.securityLogRepository = securityLogRepository;
        this.errorRecordRepository = errorRecordRepository;
        this.statisticsService = statisticsService;
    }

    /**
     * Generate security audit report.
     * Replaces COBOL RPTAUD00 audit report generation.
     */
    public AuditReport generateAuditReport(LocalDate reportDate) {
        log.info("Generating audit report for {} (RPTAUD00)", reportDate);

        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.atTime(23, 59, 59);

        List<SecurityLogRecord> auditLogs = securityLogRepository
                .findByAuditTimestampBetween(startOfDay, endOfDay);
        statisticsService.recordQuery();

        List<ErrorRecord> errorLogs = errorRecordRepository.findByProcessDate(reportDate);
        statisticsService.recordQuery();

        AuditReport report = new AuditReport();
        report.setReportDate(reportDate);
        report.setAuditLogEntries(auditLogs);
        report.setErrorLogEntries(errorLogs);
        report.setTotalAuditEntries(auditLogs.size());
        report.setTotalErrorEntries(errorLogs.size());

        long severeErrors = errorLogs.stream()
                .filter(e -> e.getErrorSeverity() >= ErrorRecord.SEVERITY_SEVERE)
                .count();
        report.setSevereErrorCount(severeErrors);

        log.info("Audit report generated: {} audit entries, {} error entries, {} severe",
                auditLogs.size(), errorLogs.size(), severeErrors);
        return report;
    }

    /**
     * Audit report data structure.
     */
    public static class AuditReport {
        private LocalDate reportDate;
        private List<SecurityLogRecord> auditLogEntries = new ArrayList<>();
        private List<ErrorRecord> errorLogEntries = new ArrayList<>();
        private int totalAuditEntries;
        private int totalErrorEntries;
        private long severeErrorCount;

        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public List<SecurityLogRecord> getAuditLogEntries() { return auditLogEntries; }
        public void setAuditLogEntries(List<SecurityLogRecord> auditLogEntries) { this.auditLogEntries = auditLogEntries; }
        public List<ErrorRecord> getErrorLogEntries() { return errorLogEntries; }
        public void setErrorLogEntries(List<ErrorRecord> errorLogEntries) { this.errorLogEntries = errorLogEntries; }
        public int getTotalAuditEntries() { return totalAuditEntries; }
        public void setTotalAuditEntries(int totalAuditEntries) { this.totalAuditEntries = totalAuditEntries; }
        public int getTotalErrorEntries() { return totalErrorEntries; }
        public void setTotalErrorEntries(int totalErrorEntries) { this.totalErrorEntries = totalErrorEntries; }
        public long getSevereErrorCount() { return severeErrorCount; }
        public void setSevereErrorCount(long severeErrorCount) { this.severeErrorCount = severeErrorCount; }
    }
}
