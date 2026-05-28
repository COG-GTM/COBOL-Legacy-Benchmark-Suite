package com.clbs.portfolio.service.report;

import com.clbs.portfolio.config.ReportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class SystemStatsReportService {

    private static final Logger log = LoggerFactory.getLogger(SystemStatsReportService.class);
    private static final int LINE_WIDTH = 132;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JobExplorer jobExplorer;
    private final ReportConfig reportConfig;

    @PersistenceContext
    private EntityManager entityManager;

    public SystemStatsReportService(JobExplorer jobExplorer, ReportConfig reportConfig) {
        this.jobExplorer = jobExplorer;
        this.reportConfig = reportConfig;
    }

    public String generateReport(LocalDate startDate, LocalDate endDate) {
        List<String> jobNames = jobExplorer.getJobNames();
        List<JobExecution> allExecutions = new ArrayList<>();

        for (String jobName : jobNames) {
            List<Long> instanceIds = jobExplorer.getJobInstances(jobName, 0, 1000).stream()
                    .map(ji -> ji.getInstanceId())
                    .toList();
            for (Long instanceId : instanceIds) {
                allExecutions.addAll(
                        jobExplorer.getJobExecutions(
                                jobExplorer.getJobInstance(instanceId)));
            }
        }

        // Filter to date range
        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.atTime(LocalTime.MAX);
        List<JobExecution> periodExecutions = allExecutions.stream()
                .filter(je -> {
                    LocalDateTime createTime = je.getCreateTime();
                    if (createTime == null) return false;
                    return !createTime.isBefore(startDt) && !createTime.isAfter(endDt);
                })
                .toList();

        return generateTextReport(startDate, endDate, periodExecutions);
    }

    private String generateTextReport(LocalDate startDate, LocalDate endDate,
                                       List<JobExecution> executions) {
        StringBuilder sb = new StringBuilder();

        // Header (matching RPTSTA00.cbl format)
        sb.append(repeat('*', LINE_WIDTH)).append("\n");
        sb.append(center("SYSTEM STATISTICS AND PERFORMANCE REPORT", LINE_WIDTH)).append("\n");
        sb.append(String.format("%-15s%s to %s", "REPORT PERIOD:", startDate.format(DATE_FMT),
                endDate.format(DATE_FMT))).append("\n");
        sb.append(repeat('*', LINE_WIDTH)).append("\n\n");

        // Database statistics (from Hibernate stats)
        sb.append(center("DATABASE STATISTICS", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        Map<String, Object> hibernateStats = getHibernateStatistics();
        long dbCalls = (long) hibernateStats.getOrDefault("queryExecutionCount", 0L);
        long maxQueryTime = (long) hibernateStats.getOrDefault("queryExecutionMaxTime", 0L);

        sb.append(String.format("  %-30s %,15d%n", "DB2 CALLS:", dbCalls));
        sb.append(String.format("  %-30s %,15d ms%n", "MAX QUERY TIME:", maxQueryTime));

        if (dbCalls > 0) {
            BigDecimal avgResponse = BigDecimal.valueOf(maxQueryTime)
                    .divide(BigDecimal.valueOf(Math.max(dbCalls, 1)), 3, RoundingMode.HALF_UP);
            sb.append(String.format("  %-30s %,15.3f ms%n", "AVG RESPONSE:", avgResponse));
        }

        // Batch job statistics
        sb.append("\n");
        sb.append(center("BATCH JOB STATISTICS", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        long totalJobs = executions.size();
        long successCount = executions.stream()
                .filter(je -> je.getStatus() == BatchStatus.COMPLETED).count();
        long failedCount = executions.stream()
                .filter(je -> je.getStatus() == BatchStatus.FAILED).count();

        BigDecimal successRate = BigDecimal.ZERO;
        if (totalJobs > 0) {
            successRate = BigDecimal.valueOf(successCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalJobs), 2, RoundingMode.HALF_UP);
        }

        long totalDurationMs = executions.stream()
                .filter(je -> je.getStartTime() != null && je.getEndTime() != null)
                .mapToLong(je -> ChronoUnit.MILLIS.between(je.getStartTime(), je.getEndTime()))
                .sum();

        BigDecimal avgDuration = BigDecimal.ZERO;
        long completedWithTimes = executions.stream()
                .filter(je -> je.getStartTime() != null && je.getEndTime() != null).count();
        if (completedWithTimes > 0) {
            avgDuration = BigDecimal.valueOf(totalDurationMs)
                    .divide(BigDecimal.valueOf(completedWithTimes), 2, RoundingMode.HALF_UP);
        }

        sb.append(String.format("  %-30s %,15d%n", "BATCH JOBS:", totalJobs));
        sb.append(String.format("  %-30s %,15d%n", "SUCCESS COUNT:", successCount));
        sb.append(String.format("  %-30s %,15d%n", "FAILED COUNT:", failedCount));
        sb.append(String.format("  %-30s %,15.2f ms%n", "TOTAL ELAPSED TIME:", BigDecimal.valueOf(totalDurationMs)));
        sb.append(String.format("  %-30s %,15.2f ms%n", "AVG DURATION:", avgDuration));
        sb.append(String.format("  %-30s %14.2f%%%n", "SUCCESS RATE:", successRate));

        // Job detail breakdown
        sb.append("\n");
        sb.append(center("JOB DETAIL BREAKDOWN", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        Map<String, List<JobExecution>> byJobName = executions.stream()
                .collect(Collectors.groupingBy(je -> je.getJobInstance().getJobName()));
        for (Map.Entry<String, List<JobExecution>> entry : byJobName.entrySet()) {
            List<JobExecution> jobExecs = entry.getValue();
            long jobSuccess = jobExecs.stream()
                    .filter(je -> je.getStatus() == BatchStatus.COMPLETED).count();
            sb.append(String.format("  %-20s  Total: %,5d  Success: %,5d  Failed: %,5d%n",
                    entry.getKey(), jobExecs.size(), jobSuccess,
                    jobExecs.size() - jobSuccess));
        }

        // Resource utilization summary
        sb.append("\n");
        sb.append(center("RESOURCE UTILIZATION SUMMARY", LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        BigDecimal memoryUtilPct = BigDecimal.valueOf(usedMemory)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalMemory), 2, RoundingMode.HALF_UP);

        sb.append(String.format("  %-30s %,15d bytes%n", "TOTAL MEMORY:", totalMemory));
        sb.append(String.format("  %-30s %,15d bytes%n", "USED MEMORY:", usedMemory));
        sb.append(String.format("  %-30s %,15d bytes%n", "FREE MEMORY:", freeMemory));
        sb.append(String.format("  %-30s %14.2f%%%n", "MEMORY UTILIZATION:", memoryUtilPct));

        sb.append("\n");
        sb.append(repeat('*', LINE_WIDTH)).append("\n");

        writeToFile(startDate, endDate, sb.toString());
        return sb.toString();
    }

    private Map<String, Object> getHibernateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            org.hibernate.stat.Statistics hibernateStats =
                    entityManager.unwrap(org.hibernate.Session.class)
                            .getSessionFactory().getStatistics();
            stats.put("queryExecutionCount", hibernateStats.getQueryExecutionCount());
            stats.put("queryExecutionMaxTime", hibernateStats.getQueryExecutionMaxTime());
        } catch (Exception e) {
            log.warn("Could not retrieve Hibernate statistics: {}", e.getMessage());
            stats.put("queryExecutionCount", 0L);
            stats.put("queryExecutionMaxTime", 0L);
        }
        return stats;
    }

    private void writeToFile(LocalDate startDate, LocalDate endDate, String content) {
        try {
            Path outputPath = Paths.get(reportConfig.getOutputDirectory(),
                    "system_stats_" + startDate.format(DATE_FMT) + "_" +
                    endDate.format(DATE_FMT) + ".txt");
            Files.writeString(outputPath, content);
            log.info("System stats report written to {}", outputPath);
        } catch (IOException e) {
            log.error("Failed to write system stats report file", e);
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
