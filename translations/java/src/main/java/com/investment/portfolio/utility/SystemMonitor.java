package com.investment.portfolio.utility;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * System Monitor (UTLMON00) - Java equivalent of UTLMON00.cbl
 *
 * Original COBOL: src/programs/utility/UTLMON00.cbl
 *
 * Responsibilities:
 * - Monitors system health and performance metrics
 * - Checks thresholds for CPU, memory, DASD, and DB2 resources
 * - Generates alerts when thresholds are exceeded
 * - Logs system status periodically
 *
 * Monitored resources (from WS-RESOURCE-TYPE):
 * - CPU:    Processor utilization
 * - MEMORY: Memory usage and allocation
 * - DASD:   Disk storage usage (maps to VSAM DASD monitoring)
 * - DB2:    Database connection and query metrics
 *
 * Threshold types (from WS-THRESHOLD-TYPE):
 * - UTIL:     Utilization percentage threshold
 * - RESPONSE: Response time threshold (milliseconds)
 * - QUEUE:    Queue depth threshold
 * - ERROR:    Error rate threshold
 *
 * Alert levels (from WS-ALERT-LEVEL):
 * - INFO:     Informational - within normal parameters
 * - WARNING:  Warning - approaching threshold
 * - CRITICAL: Critical - threshold exceeded, action needed
 */
public class SystemMonitor {

    private static final Logger LOGGER = Logger.getLogger(SystemMonitor.class.getName());
    private static final String PROGRAM_ID = "UTLMON00";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Resource types matching WS-RESOURCE-TYPE */
    public enum ResourceType {
        CPU, MEMORY, DASD, DB2
    }

    /** Threshold types matching WS-THRESHOLD-TYPE */
    public enum ThresholdType {
        UTIL, RESPONSE, QUEUE, ERROR
    }

    /** Alert levels matching WS-ALERT-LEVEL */
    public enum AlertLevel {
        INFO, WARNING, CRITICAL
    }

    private final Path reportFilePath;
    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;

    /** Threshold configuration */
    private double cpuUtilThreshold = 80.0;
    private double memoryUtilThreshold = 85.0;
    private double dasdUtilThreshold = 90.0;
    private long responseTimeThreshold = 5000; // ms
    private int queueDepthThreshold = 100;
    private double errorRateThreshold = 5.0;   // percent

    /** Collected metrics */
    private final List<MetricRecord> metrics;
    private final List<AlertRecord> alerts;

    public SystemMonitor(Path reportFilePath) {
        this.reportFilePath = reportFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
        this.metrics = new ArrayList<>();
        this.alerts = new ArrayList<>();
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * PERFORM 2000-COLLECT-METRICS
     * PERFORM 3000-CHECK-THRESHOLDS
     * PERFORM 4000-LOG-STATUS
     * PERFORM 5000-GENERATE-ALERTS
     * PERFORM 9000-TERMINATE
     */
    public int execute() {
        LOGGER.info(PROGRAM_ID + " - System Monitoring starting");

        try {
            initialize();
            collectMetrics();
            checkThresholds();
            logStatus();
            generateAlerts();
            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Set up monitoring parameters.
     */
    private void initialize() {
        metrics.clear();
        alerts.clear();
        LOGGER.info(PROGRAM_ID + " - Initialization complete");
    }

    /**
     * 2000-COLLECT-METRICS: Gather system metrics.
     *
     * Maps to COBOL paragraphs:
     *   2100-COLLECT-CPU
     *   2200-COLLECT-MEMORY
     *   2300-COLLECT-DASD
     *   2400-COLLECT-DB2
     */
    private void collectMetrics() {
        collectCpuMetrics();
        collectMemoryMetrics();
        collectDasdMetrics();
        collectDb2Metrics();
    }

    /**
     * 2100-COLLECT-CPU: Collect CPU utilization metrics.
     */
    private void collectCpuMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double loadAverage = osBean.getSystemLoadAverage();
        int processors = osBean.getAvailableProcessors();

        double cpuUtil = (loadAverage / processors) * 100.0;
        if (cpuUtil < 0) cpuUtil = 0; // getSystemLoadAverage returns -1 if unavailable

        metrics.add(new MetricRecord(ResourceType.CPU, ThresholdType.UTIL,
                "CPU Utilization", cpuUtil, "%"));
        metrics.add(new MetricRecord(ResourceType.CPU, ThresholdType.UTIL,
                "Available Processors", processors, ""));
    }

    /**
     * 2200-COLLECT-MEMORY: Collect memory usage metrics.
     */
    private void collectMemoryMetrics() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        double memUtil = (double) usedMemory / maxMemory * 100.0;

        metrics.add(new MetricRecord(ResourceType.MEMORY, ThresholdType.UTIL,
                "Memory Utilization", memUtil, "%"));
        metrics.add(new MetricRecord(ResourceType.MEMORY, ThresholdType.UTIL,
                "Used Memory (MB)", usedMemory / (1024.0 * 1024.0), "MB"));
        metrics.add(new MetricRecord(ResourceType.MEMORY, ThresholdType.UTIL,
                "Max Memory (MB)", maxMemory / (1024.0 * 1024.0), "MB"));

        long heapUsed = memBean.getHeapMemoryUsage().getUsed();
        long heapMax = memBean.getHeapMemoryUsage().getMax();
        double heapUtil = heapMax > 0 ? (double) heapUsed / heapMax * 100.0 : 0;

        metrics.add(new MetricRecord(ResourceType.MEMORY, ThresholdType.UTIL,
                "Heap Utilization", heapUtil, "%"));
    }

    /**
     * 2300-COLLECT-DASD: Collect disk storage metrics.
     * Maps to COBOL DASD utilization monitoring.
     */
    private void collectDasdMetrics() {
        try {
            java.nio.file.FileStore store = java.nio.file.Files.getFileStore(
                    java.nio.file.Paths.get("/"));
            long totalSpace = store.getTotalSpace();
            long usableSpace = store.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;

            double diskUtil = totalSpace > 0 ? (double) usedSpace / totalSpace * 100.0 : 0;

            metrics.add(new MetricRecord(ResourceType.DASD, ThresholdType.UTIL,
                    "Disk Utilization", diskUtil, "%"));
            metrics.add(new MetricRecord(ResourceType.DASD, ThresholdType.UTIL,
                    "Usable Space (GB)", usableSpace / (1024.0 * 1024.0 * 1024.0), "GB"));
        } catch (Exception e) {
            LOGGER.warning("Unable to collect DASD metrics: " + e.getMessage());
        }
    }

    /**
     * 2400-COLLECT-DB2: Collect database connection metrics.
     * Maps to COBOL DB2 subsystem monitoring.
     */
    private void collectDb2Metrics() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();

        metrics.add(new MetricRecord(ResourceType.DB2, ThresholdType.RESPONSE,
                "JVM Uptime (sec)", uptime / 1000.0, "sec"));
        // In a real system, would query DB2 catalog tables or monitor API
        metrics.add(new MetricRecord(ResourceType.DB2, ThresholdType.QUEUE,
                "Active Connections (simulated)", 0, ""));
    }

    /**
     * 3000-CHECK-THRESHOLDS: Compare metrics against configured thresholds.
     *
     * Maps to:
     *   EVALUATE WS-RESOURCE-TYPE
     *     WHEN 'CPU'    PERFORM 3100-CHECK-CPU-THRESHOLD
     *     WHEN 'MEMORY' PERFORM 3200-CHECK-MEMORY-THRESHOLD
     *     WHEN 'DASD'   PERFORM 3300-CHECK-DASD-THRESHOLD
     *     WHEN 'DB2'    PERFORM 3400-CHECK-DB2-THRESHOLD
     *   END-EVALUATE
     */
    private void checkThresholds() {
        for (MetricRecord metric : metrics) {
            AlertLevel level = evaluateThreshold(metric);
            if (level != AlertLevel.INFO) {
                alerts.add(new AlertRecord(level, metric.resourceType,
                        metric.name, metric.value, getThresholdForMetric(metric)));
            }
        }
    }

    /**
     * Evaluates a metric against its threshold and returns alert level.
     */
    private AlertLevel evaluateThreshold(MetricRecord metric) {
        double threshold;

        switch (metric.resourceType) {
            case CPU:
                threshold = cpuUtilThreshold;
                break;
            case MEMORY:
                threshold = memoryUtilThreshold;
                break;
            case DASD:
                threshold = dasdUtilThreshold;
                break;
            case DB2:
                if (metric.thresholdType == ThresholdType.RESPONSE) {
                    threshold = responseTimeThreshold;
                } else {
                    threshold = queueDepthThreshold;
                }
                break;
            default:
                return AlertLevel.INFO;
        }

        if (metric.thresholdType == ThresholdType.UTIL && "%".equals(metric.unit)) {
            if (metric.value >= threshold) return AlertLevel.CRITICAL;
            if (metric.value >= threshold * 0.8) return AlertLevel.WARNING;
        }

        return AlertLevel.INFO;
    }

    private double getThresholdForMetric(MetricRecord metric) {
        switch (metric.resourceType) {
            case CPU:    return cpuUtilThreshold;
            case MEMORY: return memoryUtilThreshold;
            case DASD:   return dasdUtilThreshold;
            default:     return 0;
        }
    }

    /**
     * 4000-LOG-STATUS: Write status report to output file.
     */
    private void logStatus() {
        try (FileHandler reportFile = new FileHandler(reportFilePath)) {
            reportFile.openOutput();

            reportFile.writeLine("=".repeat(70));
            reportFile.writeLine("SYSTEM MONITORING REPORT");
            reportFile.writeLine("Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            reportFile.writeLine("Program: " + PROGRAM_ID);
            reportFile.writeLine("=".repeat(70));
            reportFile.writeLine("");

            // Metrics section
            reportFile.writeLine("SYSTEM METRICS");
            reportFile.writeLine("-".repeat(70));
            reportFile.writeLine(String.format("%-10s %-30s %12s %5s",
                    "Resource", "Metric", "Value", "Unit"));
            reportFile.writeLine("-".repeat(70));

            for (MetricRecord metric : metrics) {
                reportFile.writeLine(String.format("%-10s %-30s %12.2f %5s",
                        metric.resourceType, metric.name, metric.value, metric.unit));
            }

            // Alerts section
            if (!alerts.isEmpty()) {
                reportFile.writeLine("");
                reportFile.writeLine("ALERTS");
                reportFile.writeLine("-".repeat(70));
                reportFile.writeLine(String.format("%-10s %-10s %-30s %10s %10s",
                        "Level", "Resource", "Metric", "Value", "Threshold"));
                reportFile.writeLine("-".repeat(70));

                for (AlertRecord alert : alerts) {
                    reportFile.writeLine(String.format("%-10s %-10s %-30s %10.2f %10.2f",
                            alert.level, alert.resourceType, alert.metricName,
                            alert.value, alert.threshold));
                }
            } else {
                reportFile.writeLine("");
                reportFile.writeLine("NO ALERTS - All metrics within normal parameters");
            }

            reportFile.writeLine("");
            reportFile.writeLine("=".repeat(70));
            reportFile.writeLine("END OF REPORT");

        } catch (Exception e) {
            errorHandler.handleSystemError("E400", "Error writing status report", e);
            returnCode.setCode(ReturnCode.ERROR);
        }
    }

    /**
     * 5000-GENERATE-ALERTS: Process and output alerts.
     */
    private void generateAlerts() {
        boolean hasCritical = alerts.stream()
                .anyMatch(a -> a.level == AlertLevel.CRITICAL);
        boolean hasWarning = alerts.stream()
                .anyMatch(a -> a.level == AlertLevel.WARNING);

        if (hasCritical) {
            LOGGER.severe("CRITICAL alerts detected!");
            returnCode.setCode(ReturnCode.ERROR);
        } else if (hasWarning) {
            LOGGER.warning("WARNING alerts detected");
            returnCode.setCode(ReturnCode.WARNING);
        }

        for (AlertRecord alert : alerts) {
            LOGGER.info(String.format("ALERT [%s] %s %s: %.2f (threshold: %.2f)",
                    alert.level, alert.resourceType, alert.metricName,
                    alert.value, alert.threshold));
        }
    }

    /**
     * 9000-TERMINATE: Final summary.
     */
    private void terminate() {
        LOGGER.info(PROGRAM_ID + " - Metrics collected: " + metrics.size()
                + " Alerts generated: " + alerts.size());
    }

    // --- Configuration setters for thresholds ---

    public void setCpuUtilThreshold(double threshold) { this.cpuUtilThreshold = threshold; }
    public void setMemoryUtilThreshold(double threshold) { this.memoryUtilThreshold = threshold; }
    public void setDasdUtilThreshold(double threshold) { this.dasdUtilThreshold = threshold; }
    public void setResponseTimeThreshold(long threshold) { this.responseTimeThreshold = threshold; }
    public void setQueueDepthThreshold(int threshold) { this.queueDepthThreshold = threshold; }
    public void setErrorRateThreshold(double threshold) { this.errorRateThreshold = threshold; }

    // --- Inner data classes ---

    private static class MetricRecord {
        final ResourceType resourceType;
        final ThresholdType thresholdType;
        final String name;
        final double value;
        final String unit;

        MetricRecord(ResourceType resourceType, ThresholdType thresholdType,
                     String name, double value, String unit) {
            this.resourceType = resourceType;
            this.thresholdType = thresholdType;
            this.name = name;
            this.value = value;
            this.unit = unit;
        }
    }

    private static class AlertRecord {
        final AlertLevel level;
        final ResourceType resourceType;
        final String metricName;
        final double value;
        final double threshold;

        AlertRecord(AlertLevel level, ResourceType resourceType,
                    String metricName, double value, double threshold) {
            this.level = level;
            this.resourceType = resourceType;
            this.metricName = metricName;
            this.value = value;
            this.threshold = threshold;
        }
    }
}
