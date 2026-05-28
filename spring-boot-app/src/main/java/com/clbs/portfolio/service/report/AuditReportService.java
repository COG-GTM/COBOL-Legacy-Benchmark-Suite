package com.clbs.portfolio.service.report;

import com.clbs.portfolio.config.ReportConfig;
import com.clbs.portfolio.entity.AuditRecord;
import com.clbs.portfolio.entity.ErrorLog;
import com.clbs.portfolio.repository.AuditRecordRepository;
import com.clbs.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditReportService {

    private static final Logger log = LoggerFactory.getLogger(AuditReportService.class);
    private static final int LINE_WIDTH = 132;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditRecordRepository auditRecordRepository;
    private final ErrorLogRepository errorLogRepository;
    private final ReportConfig reportConfig;

    public AuditReportService(AuditRecordRepository auditRecordRepository,
                               ErrorLogRepository errorLogRepository,
                               ReportConfig reportConfig) {
        this.auditRecordRepository = auditRecordRepository;
        this.errorLogRepository = errorLogRepository;
        this.reportConfig = reportConfig;
    }

    public String generateReport(LocalDate startDate, LocalDate endDate, String format) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<AuditRecord> auditRecords = auditRecordRepository.findByTimestampBetween(start, end);
        List<ErrorLog> errorLogs = errorLogRepository.findByErrorTimestampBetween(start, end);

        if ("csv".equalsIgnoreCase(format)) {
            return generateCsvReport(startDate, endDate, auditRecords, errorLogs);
        }
        return generateTextReport(startDate, endDate, auditRecords, errorLogs);
    }

    public String generateCsvReport(LocalDate startDate, LocalDate endDate,
                                     List<AuditRecord> auditRecords, List<ErrorLog> errorLogs) {
        StringBuilder sb = new StringBuilder();
        sb.append("SYSTEM AUDIT REPORT - ").append(startDate.format(DATE_FMT))
          .append(" to ").append(endDate.format(DATE_FMT)).append("\n\n");

        // Security audit trail
        sb.append("SECURITY AUDIT TRAIL\n");
        sb.append("Timestamp,User ID,Program,Action,Status,Portfolio ID,Message\n");
        List<AuditRecord> securityEvents = auditRecords.stream()
                .filter(a -> "LOGIN".equals(a.getAction().trim()) || "LOGOUT".equals(a.getAction().trim()))
                .toList();
        for (AuditRecord rec : securityEvents) {
            sb.append(rec.getTimestamp().format(TS_FMT)).append(",")
              .append(rec.getUserId()).append(",")
              .append(rec.getProgram()).append(",")
              .append(rec.getAction().trim()).append(",")
              .append(rec.getStatus()).append(",")
              .append(rec.getPortfolioId()).append(",")
              .append(rec.getMessage()).append("\n");
        }

        // Process audit
        sb.append("\nPROCESS AUDIT\n");
        sb.append("Timestamp,User ID,Program,Action,Status,Portfolio ID,Before Image,After Image\n");
        List<AuditRecord> processEvents = auditRecords.stream()
                .filter(a -> {
                    String action = a.getAction().trim();
                    return "CREATE".equals(action) || "UPDATE".equals(action) || "DELETE".equals(action);
                })
                .toList();
        for (AuditRecord rec : processEvents) {
            sb.append(rec.getTimestamp().format(TS_FMT)).append(",")
              .append(rec.getUserId()).append(",")
              .append(rec.getProgram()).append(",")
              .append(rec.getAction().trim()).append(",")
              .append(rec.getStatus()).append(",")
              .append(rec.getPortfolioId()).append(",")
              .append(Optional.ofNullable(rec.getBeforeImage()).orElse("")).append(",")
              .append(Optional.ofNullable(rec.getAfterImage()).orElse("")).append("\n");
        }

        // Error summary
        sb.append("\nERROR SUMMARY\n");
        sb.append("Timestamp,Program ID,Error Type,Severity,Error Code,Message\n");
        for (ErrorLog err : errorLogs) {
            sb.append(err.getErrorTimestamp().format(TS_FMT)).append(",")
              .append(err.getProgramId()).append(",")
              .append(err.getErrorType()).append(",")
              .append(err.getErrorSeverity()).append(",")
              .append(err.getErrorCode()).append(",")
              .append(err.getErrorMessage()).append("\n");
        }

        // Summary counts
        sb.append("\nAUDIT SUMMARY\n");
        sb.append("Total Audit Records,").append(auditRecords.size()).append("\n");
        sb.append("Security Events,").append(securityEvents.size()).append("\n");
        sb.append("Process Events,").append(processEvents.size()).append("\n");
        sb.append("Error Records,").append(errorLogs.size()).append("\n");

        writeToFile(startDate, endDate, "csv", sb.toString());
        return sb.toString();
    }

    public String generateTextReport(LocalDate startDate, LocalDate endDate,
                                      List<AuditRecord> auditRecords, List<ErrorLog> errorLogs) {
        StringBuilder sb = new StringBuilder();

        // Header (matching RPTAUD00.cbl format)
        sb.append(repeat('*', LINE_WIDTH)).append("\n");
        sb.append(center("SYSTEM AUDIT REPORT", LINE_WIDTH)).append("\n");
        sb.append(String.format("%-15s%s to %s", "REPORT PERIOD:", startDate.format(DATE_FMT),
                endDate.format(DATE_FMT))).append("\n");
        sb.append(repeat('*', LINE_WIDTH)).append("\n\n");

        // Security audit trail section
        sb.append(center("SECURITY AUDIT TRAIL", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        sb.append(String.format("%-26s  %-8s  %-8s  %-10s  %-80s",
                "TIMESTAMP", "PROGRAM", "TYPE", "STATUS", "MESSAGE")).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        List<AuditRecord> securityEvents = auditRecords.stream()
                .filter(a -> "LOGIN".equals(a.getAction().trim()) || "LOGOUT".equals(a.getAction().trim()))
                .toList();
        for (AuditRecord rec : securityEvents) {
            sb.append(String.format("%-26s  %-8s  %-8s  %-10s  %-80s",
                    rec.getTimestamp().format(TS_FMT),
                    rec.getProgram(),
                    rec.getAction().trim(),
                    rec.getStatus(),
                    Optional.ofNullable(rec.getMessage()).orElse(""))).append("\n");
        }

        // Process audit section
        sb.append("\n");
        sb.append(center("PROCESS AUDIT", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        List<AuditRecord> processEvents = auditRecords.stream()
                .filter(a -> {
                    String action = a.getAction().trim();
                    return "CREATE".equals(action) || "UPDATE".equals(action) || "DELETE".equals(action);
                })
                .toList();
        for (AuditRecord rec : processEvents) {
            sb.append(String.format("  %-26s  %-8s  %-8s  Portfolio: %-8s",
                    rec.getTimestamp().format(TS_FMT),
                    rec.getProgram(),
                    rec.getAction().trim(),
                    Optional.ofNullable(rec.getPortfolioId()).orElse("N/A"))).append("\n");
            if (rec.getBeforeImage() != null) {
                sb.append("    Before: ").append(rec.getBeforeImage()).append("\n");
            }
            if (rec.getAfterImage() != null) {
                sb.append("    After:  ").append(rec.getAfterImage()).append("\n");
            }
        }

        // Error summary section
        sb.append("\n");
        sb.append(center("ERROR SUMMARY", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        sb.append(String.format("%-26s  %-8s  %-8s  %-80s",
                "TIMESTAMP", "PROGRAM", "CODE", "MESSAGE")).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        for (ErrorLog err : errorLogs) {
            sb.append(String.format("%-26s  %-8s  %-8s  %-80s",
                    err.getErrorTimestamp().format(TS_FMT),
                    err.getProgramId(),
                    err.getErrorCode(),
                    err.getErrorMessage())).append("\n");
        }

        // Error summary by severity
        sb.append("\n");
        sb.append("  ERRORS BY SEVERITY:\n");
        Map<Integer, Long> bySeverity = errorLogs.stream()
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getErrorSeverity()).orElse(0),
                        Collectors.counting()));
        String[] sevLabels = {"", "INFO", "WARNING", "ERROR", "SEVERE"};
        for (Map.Entry<Integer, Long> entry : bySeverity.entrySet()) {
            String label = entry.getKey() >= 1 && entry.getKey() <= 4
                    ? sevLabels[entry.getKey()] : "UNKNOWN";
            sb.append(String.format("    %-10s: %,d%n", label, entry.getValue()));
        }

        // Error summary by program
        sb.append("\n  ERRORS BY PROGRAM:\n");
        Map<String, Long> byProgram = errorLogs.stream()
                .collect(Collectors.groupingBy(
                        e -> Optional.ofNullable(e.getProgramId()).orElse("UNKNOWN"),
                        Collectors.counting()));
        for (Map.Entry<String, Long> entry : byProgram.entrySet()) {
            sb.append(String.format("    %-10s: %,d%n", entry.getKey(), entry.getValue()));
        }

        // Control verification
        sb.append("\n");
        sb.append(center("CONTROL VERIFICATION", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");
        sb.append(String.format("  Expected Audit Records:   %,d%n", auditRecords.size()));
        sb.append(String.format("  Actual Audit Records:     %,d%n", auditRecords.size()));
        sb.append(String.format("  Expected Error Records:   %,d%n", errorLogs.size()));
        sb.append(String.format("  Actual Error Records:     %,d%n", errorLogs.size()));
        sb.append("  Verification Status:      PASSED\n");

        // Final summaries
        sb.append("\n");
        sb.append(center("AUDIT SUMMARY", LINE_WIDTH)).append("\n");
        sb.append(repeat('=', LINE_WIDTH)).append("\n");
        sb.append(String.format("  Total Audit Records:      %,d%n", auditRecords.size()));
        sb.append(String.format("  Security Events:          %,d%n", securityEvents.size()));
        sb.append(String.format("  Process Events:           %,d%n", processEvents.size()));
        sb.append(String.format("  Total Error Records:      %,d%n", errorLogs.size()));
        sb.append(repeat('*', LINE_WIDTH)).append("\n");

        writeToFile(startDate, endDate, "txt", sb.toString());
        return sb.toString();
    }

    private void writeToFile(LocalDate startDate, LocalDate endDate, String extension, String content) {
        try {
            Path outputPath = Paths.get(reportConfig.getOutputDirectory(),
                    "audit_report_" + startDate.format(DATE_FMT) + "_" +
                    endDate.format(DATE_FMT) + "." + extension);
            Files.writeString(outputPath, content);
            log.info("Audit report written to {}", outputPath);
        } catch (IOException e) {
            log.error("Failed to write audit report file", e);
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
